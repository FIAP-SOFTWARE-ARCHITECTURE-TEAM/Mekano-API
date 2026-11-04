package com.fiap.mekano.rest.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FixedWindowRateLimiter")
class FixedWindowRateLimiterTest {

    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-10-17T12:00:00Z"));
    private FixedWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new FixedWindowRateLimiter();
        limiter.setClock(agora::get);
    }

    private void avancar(Duration d) {
        agora.set(agora.get().plus(d));
    }

    @Test
    @DisplayName("permite até o limite e bloqueia a partir dele")
    void permiteAteLimiteEBloqueiaDepois() {
        assertThat(limiter.allow("auth:1.2.3.4", 3, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 3, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 3, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 3, 60)).isFalse();
    }

    @Test
    @DisplayName("janela nova reseta o contador")
    void janelaNovaResetaContador() {
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isFalse();

        avancar(Duration.ofSeconds(61));

        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
    }

    @Test
    @DisplayName("chaves distintas não compartilham janela")
    void chavesDistintasIndependentes() {
        assertThat(limiter.allow("auth:1.2.3.4", 1, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 1, 60)).isFalse();

        assertThat(limiter.allow("auth:5.6.7.8", 1, 60)).isTrue();
        assertThat(limiter.allow("webhooks/evolution:1.2.3.4", 1, 60)).isTrue();
    }

    @Test
    @DisplayName("requisições permitidas dentro da janela não a estendem")
    void janelaNaoDesliza() {
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
        avancar(Duration.ofSeconds(30));
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
        avancar(Duration.ofSeconds(31)); // 61s após início da janela → nova janela

        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isTrue();
        assertThat(limiter.allow("auth:1.2.3.4", 2, 60)).isFalse();
    }
}
