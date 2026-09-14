package com.fiap.mekano.infrastructure.metrics;

import com.fiap.mekano.domain.model.StatusOS;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerOSMetricsAdapterTest {

    private MeterRegistry registry;
    private MicrometerOSMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        adapter = new MicrometerOSMetricsAdapter(registry);
    }

    @Test
    @DisplayName("registrarCriacaoOS deve incrementar counter os.criadas.total")
    void registrarCriacaoOS_deveIncrementarCounter() {
        adapter.registrarCriacaoOS();
        adapter.registrarCriacaoOS();

        Counter counter = registry.find("os.criadas.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("registrarTransicaoStatus deve incrementar counter com tags corretas")
    void registrarTransicaoStatus_deveIncrementarCounterComTags() {
        adapter.registrarTransicaoStatus(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO);
        adapter.registrarTransicaoStatus(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO);
        adapter.registrarTransicaoStatus(StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO);

        Counter counterRecebida = registry.find("os.status.alterado")
                .tag("status_origem", "RECEBIDA")
                .tag("status_destino", "EM_DIAGNOSTICO")
                .counter();
        assertThat(counterRecebida).isNotNull();
        assertThat(counterRecebida.count()).isEqualTo(2.0);

        Counter counterDiagnostico = registry.find("os.status.alterado")
                .tag("status_origem", "EM_DIAGNOSTICO")
                .tag("status_destino", "AGUARDANDO_APROVACAO")
                .counter();
        assertThat(counterDiagnostico).isNotNull();
        assertThat(counterDiagnostico.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("registrarTransicaoStatus com origem null deve usar tag CRIACAO")
    void registrarTransicaoStatus_origemNull_deveUsarTagCriacao() {
        adapter.registrarTransicaoStatus(null, StatusOS.RECEBIDA);

        Counter counter = registry.find("os.status.alterado")
                .tag("status_origem", "CRIACAO")
                .tag("status_destino", "RECEBIDA")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("registrarFalhaTransicao deve incrementar counter com tags status_atual e status_tentado")
    void registrarFalhaTransicao_deveIncrementarCounter() {
        adapter.registrarFalhaTransicao(StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_APROVACAO);

        Counter counter = registry.find("os.transicao.falha")
                .tag("status_atual", "EM_EXECUCAO")
                .tag("status_tentado", "AGUARDANDO_APROVACAO")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("registrarTempoFase deve registrar timer com tag fase")
    void registrarTempoFase_deveRegistrarTimer() {
        Duration duracao = Duration.ofMinutes(30);
        adapter.registrarTempoFase("DIAGNOSTICO", duracao);
        adapter.registrarTempoFase("DIAGNOSTICO", Duration.ofMinutes(10));

        Timer timer = registry.find("os.duracao.fase").tag("fase", "DIAGNOSTICO").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(40.0 * 60 * 1000);
    }

    @Test
    @DisplayName("registrarTempoFase com duracao negativa ou null nao deve registrar")
    void registrarTempoFase_duracaoInvalida_naoRegistra() {
        adapter.registrarTempoFase("TOTAL", Duration.ofMillis(-1));
        adapter.registrarTempoFase("TOTAL", null);

        Timer timer = registry.find("os.duracao.fase").tag("fase", "TOTAL").timer();
        assertThat(timer).isNull();
    }

    @Test
    @DisplayName("todas as metricas devem ser independentes")
    void metricas_devemSerIndependentes() {
        adapter.registrarCriacaoOS();
        adapter.registrarTransicaoStatus(StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO);
        adapter.registrarFalhaTransicao(StatusOS.RECEBIDA, StatusOS.FINALIZADA);
        adapter.registrarTempoFase("TOTAL", Duration.ofSeconds(60));

        assertThat(registry.find("os.criadas.total").counter()).isNotNull();
        assertThat(registry.find("os.status.alterado").counter()).isNotNull();
        assertThat(registry.find("os.transicao.falha").counter()).isNotNull();
        assertThat(registry.find("os.duracao.fase").timer()).isNotNull();
    }
}
