package com.fiap.mekano.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do {@link TokenBucketRateLimiter}.
 *
 * <p>Testa a lógica do token bucket sem CDI — instancia o {@code @ApplicationScoped}
 * diretamente com valores explícitos (as anotações {@code @ConfigProperty} são
 * ignoradas em chamadas diretas ao construtor).
 *
 * <p>Cenários:
 * <ul>
 *     <li>10 requisições consecutivas (mesma chave) — todas permitidas</li>
 *     <li>11ª requisição (mesma chave) — rejeitada (bucket vazio)</li>
 *     <li>{@code getRetryAfterSeconds} &gt; 0 após exaustão</li>
 *     <li>Chaves diferentes não interferem</li>
 * </ul>
 */
class TokenBucketRateLimiterTest {

    private static final int CAPACITY = 10;
    private static final String PERIOD = "PT1M";
    private static final String KEY = "192.168.1.1:user@fiap.br";

    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new TokenBucketRateLimiter(CAPACITY, PERIOD);
    }

    @Test
    void first10Calls_shouldBeAllowed() {
        for (int i = 0; i < CAPACITY; i++) {
            assertTrue(rateLimiter.tryConsume(KEY),
                    "Chamada " + (i + 1) + " deveria ser permitida");
        }
    }

    @Test
    void eleventhCall_shouldBeRejected() {
        // Consome os 10 tokens disponíveis
        for (int i = 0; i < CAPACITY; i++) {
            rateLimiter.tryConsume(KEY);
        }

        // 11ª chamada deve ser rejeitada
        assertFalse(rateLimiter.tryConsume(KEY));
    }

    @Test
    void retryAfterSeconds_shouldBePositiveAfterExhaustion() {
        // Exaure o bucket
        for (int i = 0; i < CAPACITY; i++) {
            rateLimiter.tryConsume(KEY);
        }

        long retryAfter = rateLimiter.getRetryAfterSeconds(KEY);
        assertTrue(retryAfter > 0,
                "Retry-After deveria ser > 0 após exaustão, mas foi " + retryAfter);
    }

    @Test
    void differentKeys_shouldNotInterfere() {
        String keyA = "192.168.1.1:alice@fiap.br";
        String keyB = "192.168.2.1:bob@fiap.br";

        // Exaure bucket da keyA
        for (int i = 0; i < CAPACITY; i++) {
            rateLimiter.tryConsume(keyA);
        }

        // keyA deve estar exaurida
        assertFalse(rateLimiter.tryConsume(keyA));

        // keyB ainda deve ter todos os tokens
        assertTrue(rateLimiter.tryConsume(keyB));
        assertTrue(rateLimiter.tryConsume(keyB));
    }

    @Test
    void retryAfterSeconds_shouldBeZeroWhenBucketNotEmpty() {
        // Bucket ainda tem tokens
        long retryAfter = rateLimiter.getRetryAfterSeconds(KEY);
        assertEquals(0, retryAfter,
                "Retry-After deveria ser 0 quando o bucket ainda tem tokens");
    }

    @Test
    void retryAfterSeconds_shouldBeZeroForUnknownKey() {
        long retryAfter = rateLimiter.getRetryAfterSeconds("unknown:key");
        assertEquals(0, retryAfter,
                "Retry-After deveria ser 0 para chave desconhecida");
    }
}
