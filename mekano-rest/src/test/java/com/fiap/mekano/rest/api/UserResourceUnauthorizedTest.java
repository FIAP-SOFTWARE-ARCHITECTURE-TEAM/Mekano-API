package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Testes de regressão de autenticação para UAT-1 (ROADMAP) e D-04.
 *
 * Sem `@TestSecurity` e sem `@TestProfile`: exercita o pipeline real
 * (`quarkus.http.auth.proactive=false` em application.properties) e a chave
 * default `publicKey.pem` carregada via `mp.jwt.verify.publickey.location`.
 *
 * UAT-1 (D-06): POST /users sem token → 401 (desafio HTTP www-authenticate,
 *                sem body — Quarkus JWT auth mechanism envia challenge antes
 *                do ExceptionMapper).
 * D-04 (G11):   GET /q/health sem token → 200 (extensões Quarkus public-by-default).
 */
@QuarkusTest
class UserResourceUnauthorizedTest {

    @Test
    void noToken_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Ana", "email": "ana.noauth@fiap.br", "password": "abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(401);
    }

    @Test
    void publicHealthEndpoint_returns200WithoutToken() {
        given()
                .when()
                .get("/q/health")
                .then()
                .statusCode(200);
    }
}
