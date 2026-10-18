package com.fiap.mekano.rest.filter;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Rate limiter de janela fixa, em memória, por chave (rota + IP).
 *
 * <p>Escolha de design (docs/ARCHITECTURE-REVIEW-2026-10-17.md item 5): filtro
 * customizado sem dependência nova (KISS) — o objetivo é proteger {@code /auth/*}
 * contra brute force e o webhook contra flood, não substituir um API gateway.
 *
 * <p>Limitação conhecida: contadores por instância (não distribuído) e chave por
 * remoteAddr (atrás de proxy reverso todos os clientes compartilham o IP do proxy).
 * Suficiente para o deploy atual (1 pod); revisar com HPA multi-pod.
 */
@ApplicationScoped
public class FixedWindowRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private Supplier<Instant> clock = Instant::now;

    /**
     * @return true se a requisição é permitida dentro da janela; false quando o limite foi atingido
     */
    public boolean allow(String key, int limit, int windowSeconds) {
        Instant now = clock.get();
        long windowMillis = windowSeconds * 1000L;
        long nowMillis = now.toEpochMilli();

        boolean[] allowed = new boolean[1];
        windows.compute(key, (k, window) -> {
            if (window == null || nowMillis - window.windowStart >= windowMillis) {
                allowed[0] = true;
                return new Window(nowMillis, 1);
            }
            if (window.count < limit) {
                allowed[0] = true;
                return new Window(window.windowStart, window.count + 1);
            }
            allowed[0] = false;
            return window;
        });
        return allowed[0];
    }

    /** Ponto de injeção de relógio para testes (package-private). */
    void setClock(Supplier<Instant> clock) {
        this.clock = clock;
    }

    private static final class Window {
        private final long windowStart;
        private final int count;

        private Window(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
