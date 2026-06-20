package com.fiap.mekano.rest.observability;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Liveness check customizado para a aplicação Mekano.
 *
 * Decisão D-01 (06-CONTEXT.md): criação puramente pedagógica para demonstrar
 * o pattern MicroProfile Health 4.0 — não há lógica de detecção de deadlock
 * ou recurso interno a verificar.
 *
 * Aparece em GET /q/health/live e em GET /q/health (agregado) ao lado dos
 * checks auto-registrados pelo Quarkus (DataSourceHealthCheck via Agroal).
 *
 * @Liveness: qualifier CDI que faz a extensão SmallRye Health vincular
 *            este bean ao endpoint /q/health/live.
 * @ApplicationScoped: ciclo de vida do app — não é recriado por requisição,
 *                     já que call() não tem estado mutável.
 */
@Liveness
@ApplicationScoped
public class ApplicationLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("mekano-application");
    }
}
