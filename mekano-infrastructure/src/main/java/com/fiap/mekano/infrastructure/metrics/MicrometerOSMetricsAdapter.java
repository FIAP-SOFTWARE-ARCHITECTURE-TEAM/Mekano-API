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
 * Adapter Micrometer para métricas de negócio de Ordens de Serviço.
 * Registra counters e timers expostos via Prometheus em /q/metrics.
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
            Timer.builder("os.duracao.fase")
                    .description("Duracao da fase da OS em milissegundos")
                    .tag("fase", fase)
                    .register(registry)
                    .record(duracao);
        }
    }
}
