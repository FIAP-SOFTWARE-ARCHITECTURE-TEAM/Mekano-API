package com.fiap.mekano.infrastructure.metrics;

import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.port.out.OSMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;

/**
 * Adapter Micrometer para metricas de negocio de Ordens de Servico.
 * Registra counters e timers expostos via Prometheus em /q/metrics.
 *
 * <p>Metrias por fase da OS:
 * <ul>
 *   <li>{@code os.fase.duracao} (Timer, tag: fase) — duracao de cada fase</li>
 *   <li>{@code os.criadas.total} (Counter) — total de OS criadas</li>
 *   <li>{@code os.status.alterado} (Counter, tags: status_origem, status_destino)</li>
 *   <li>{@code os.transicao.falha} (Counter, tags: status_atual, status_tentado)</li>
 *   <li>{@code os.status.atual} (Counter, tag: status) — OS por status atual</li>
 * </ul>
 */
@ApplicationScoped
public class MicrometerOSMetricsAdapter implements OSMetricsPort {

    private final MeterRegistry registry;

    @Inject
    public MicrometerOSMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void registrarCriacaoOS() {
        Counter.builder("os.criadas.total")
                .description("Total de Ordens de Servico criadas")
                .register(registry)
                .increment();
    }

    @Override
    public void registrarTransicaoStatus(StatusOS de, StatusOS para) {
        String origem = de != null ? de.name() : "CRIACAO";
        Counter.builder("os.status.alterado")
                .description("Total de transicoes de status de OS")
                .tag("status_origem", origem)
                .tag("status_destino", para.name())
                .register(registry)
                .increment();
    }

    @Override
    public void registrarFalhaTransicao(StatusOS statusAtual, StatusOS tentado) {
        Counter.builder("os.transicao.falha")
                .description("Total de tentativas de transicao invalida")
                .tag("status_atual", statusAtual.name())
                .tag("status_tentado", tentado.name())
                .register(registry)
                .increment();
    }

    @Override
    public void registrarTempoFase(String fase, Duration duracao) {
        if (duracao != null && !duracao.isNegative()) {
            Timer.builder("os.fase.duracao")
                    .description("Duracao da fase da OS em milissegundos")
                    .tag("fase", fase)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry)
                    .record(duracao);
        }
    }

    @Override
    public void registrarOSPorStatus(String status) {
        if (status != null && !status.isBlank()) {
            Counter.builder("os.status.atual")
                    .description("Total de OS por status atual")
                    .tag("status", status)
                    .register(registry)
                    .increment();
        }
    }
}
