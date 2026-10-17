package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Correlation-id por request (docs/ARCHITECTURE-REVIEW-2026-10-17.md item 14):
 * header X-Request-ID ecoado na resposta e dentro do problem+json de erros.
 * Usa o endpoint público GET /os/{id}/status — sem autenticação.
 */
@QuarkusTest
@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    private static final String STATUS_PATH = "/api/v1/os/00000000-0000-0000-0000-000000000000/status";

    @Test
    @DisplayName("header X-Request-ID do cliente é ecoado na resposta")
    void headerDoClienteEcoado() {
        given()
                .header("X-Request-ID", "req-teste-123")
                .when()
                .get(STATUS_PATH)
                .then()
                .statusCode(404)
                .header("X-Request-ID", equalTo("req-teste-123"));
    }

    @Test
    @DisplayName("sem header, um correlation-id é gerado")
    void semHeaderGeraCorrelationId() {
        given()
                .when()
                .get(STATUS_PATH)
                .then()
                .statusCode(404)
                .header("X-Request-ID", notNullValue());
    }

    @Test
    @DisplayName("problem+json carrega o correlationId da requisição")
    void problemJsonCarregaCorrelationId() {
        given()
                .header("X-Request-ID", "req-erro-456")
                .when()
                .get(STATUS_PATH)
                .then()
                .statusCode(404)
                .body("correlationId", equalTo("req-erro-456"));
    }

    @Test
    @DisplayName("header malicioso (>64 chars) é descartado — correlation-id gerado")
    void headerInvalidoEDescartado() {
        String longo = "x".repeat(100);
        given()
                .header("X-Request-ID", longo)
                .when()
                .get(STATUS_PATH)
                .then()
                .statusCode(404)
                .header("X-Request-ID", notNullValue());
    }
}
