package com.fiap.mekano.infrastructure.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Habilita o histograma (buckets) para as metricas HTTP do servidor.
 *
 * <p>O Quarkus/Micrometer nao expoe uma propriedade de configuracao para
 * {@code percentilesHistogram} no binder HTTP; o {@link MeterFilter} garante
 * a geracao de {@code http_server_requests_seconds_bucket}, necessaria para
 * as consultas de p95/p99 dos dashboards.
 */
@Singleton
public class HttpServerMetricsConfig {

    @Produces
    @Singleton
    public MeterFilter httpServerHistogramFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName() != null && id.getName().startsWith("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
