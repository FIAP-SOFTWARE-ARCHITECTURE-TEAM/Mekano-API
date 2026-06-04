package com.fiap.mekano.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiter baseado em Token Bucket.
 *
 * <p>Cada chave (ex: {@code "IP:email"}) tem um bucket com capacidade máxima
 * de {@code capacity} tokens, reabastecidos a {@code tokensPerPeriod}
 * a cada {@code period}.
 *
 * <p>Thread-safe: {@link ConcurrentHashMap} com lock por chave via
 * {@code synchronized} nos métodos do bucket.
 *
 * <p>Buckets expirados (sem acesso por 2 períodos) podem ser removidos
 * via {@link #cleanExpiredBuckets()}. Em produção, sugere-se integração
 * com {@code io.quarkus.scheduler.Scheduled} para limpeza periódica.
 */
@ApplicationScoped
public class TokenBucketRateLimiter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration period;

    public TokenBucketRateLimiter(
            @ConfigProperty(name = "mekano.auth.ratelimiter.capacity", defaultValue = "10") int capacity,
            @ConfigProperty(name = "mekano.auth.ratelimiter.period", defaultValue = "PT1M") String periodStr) {
        this.capacity = capacity;
        this.period = Duration.parse(periodStr);
    }

    /**
     * Tenta consumir um token do bucket identificado por {@code key}.
     *
     * @param key chave composta (ex: "192.168.1.1:user@email.com")
     * @return true se o consumo foi permitido; false se o bucket está vazio
     */
    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, period));
        return bucket.tryConsume();
    }

    /**
     * Retorna quantos segundos até o bucket reabastecer 1 token.
     *
     * @param key chave do bucket
     * @return segundos até próximo token disponível, ou 0 se não há bucket
     */
    public long getRetryAfterSeconds(String key) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) return 0;
        return bucket.getRetryAfterSeconds();
    }

    /**
     * Limpa buckets inativos (prevenção de vazamento de memória).
     *
     * <p>Remove buckets cujo último acesso foi há mais de {@code 2 * period}.
     * Chamado pelo operador via {@code @Scheduled} do Quarkus ou sob demanda.
     */
    public void cleanExpiredBuckets() {
        Instant cutoff = Instant.now().minus(period.multipliedBy(2));
        buckets.values().removeIf(b -> b.getLastAccess().isBefore(cutoff));
    }

    static class Bucket {
        private final int capacity;
        private final Duration period;
        private int tokens;
        private Instant lastRefill;
        private Instant lastAccess;

        Bucket(int capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
            this.lastAccess = Instant.now();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                lastAccess = Instant.now();
                return true;
            }
            return false;
        }

        synchronized long getRetryAfterSeconds() {
            refill();
            if (tokens > 0) return 0;
            // Quanto tempo até ganhar 1 token
            long nanosPerToken = period.toNanos() / capacity;
            long elapsed = Duration.between(lastRefill, Instant.now()).toNanos();
            long remainingNanos = nanosPerToken - (elapsed % nanosPerToken);
            return Math.max(1, remainingNanos / 1_000_000_000);
        }

        synchronized Instant getLastAccess() { return lastAccess; }

        private void refill() {
            Instant now = Instant.now();
            long elapsed = Duration.between(lastRefill, now).toNanos();
            long nanosPerPeriod = period.toNanos();
            long periodsElapsed = elapsed / nanosPerPeriod;
            if (periodsElapsed > 0) {
                long newTokens = periodsElapsed * capacity;
                tokens = (int) Math.min(capacity, tokens + newTokens);
                lastRefill = lastRefill.plus(period.multipliedBy(periodsElapsed));
            }
        }
    }
}
