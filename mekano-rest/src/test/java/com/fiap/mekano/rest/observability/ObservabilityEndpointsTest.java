package com.fiap.mekano.rest.observability;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * Testes de integração dos endpoints de observabilidade expostos pelas extensões
 * quarkus-smallrye-health (EXT-06), quarkus-micrometer-registry-prometheus (EXT-07)
 * e quarkus-smallrye-openapi (EXT-04, já presente).
 *
 * Infra reutilizada (D-04 do 06-CONTEXT.md):
 * - @QuarkusTest sobe a aplicação completa.
 * - Perfil %test ativa DevServices PostgreSQL automaticamente (sem jdbc.url declarada).
 * - Flyway roda migrations antes do test run.
 *
 * Cobertura UAT da Fase 6:
 *  - UAT 1: health_returnsUp
 *  - UAT 2: healthReady_includesDatasource
 *  - UAT 3: metrics_returnsPrometheusFormat
 *  - UAT 4: openapi_exposesUsersResource
 *  - Bônus D-01: healthLive_includesCustomCheck (valida ApplicationLivenessCheck)
 */
@QuarkusTest
class ObservabilityEndpointsTest {

    @Test
    void health_returnsUp() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.status", hasItem("UP"));
    }

    @Test
    void healthReady_includesDatasource() {
        // DataSourceHealthCheck auto-registrado pela combinação smallrye-health + jdbc-postgresql.
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem(containsString("Database")));
    }

    @Test
    void healthLive_includesCustomCheck() {
        // Valida D-01: ApplicationLivenessCheck registrado com nome "mekano-application".
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("mekano-application"));
    }

    @Test
    void metrics_returnsPrometheusFormat() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                // Micrometer Quarkus 3.36 expõe Prometheus em OpenMetrics 1.0 por padrão
                // (application/openmetrics-text), mantendo compatibilidade com scrapers que
                // ainda aceitam text/plain do formato Prometheus clássico.
                .contentType(anyOf(containsString("text/plain"), containsString("openmetrics-text")))
                .body(containsString("# HELP"))
                .body(containsString("# TYPE"))
                .body(containsString("jvm_memory_used_bytes"));
    }

    @Test
    void openapi_exposesUsersResource() {
        // Valida UAT 4: tag Users, path /users, e schemas referenciados pelas @APIResponse.
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("Users"))
                .body(containsString("/users"))
                .body(containsString("UserResponse"))
                .body(containsString("ErrorResponse"));
    }
}
