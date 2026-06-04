package com.fiap.mekano.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testes de integração do rate limiter no endpoint {@code POST /auth/login}.
 *
 * <p>Verifica que {@code 11+} tentativas consecutivas com o mesmo email
 * exaurem o token bucket (10/min) e retornam HTTP 429 com header
 * {@code Retry-After}.
 *
 * <p>Usa {@link JwtTestProfile} para fornecer o par RSA in-memory (mesmo
 * padrão de {@link AuthResourceTest}) e {@code @TestSecurity} para seed do
 * usuário de teste.
 *
 * <p>O rate limit aplica-se por chave composta {@code IP:email}. Como o
 * IP é fixo dentro do teste (localhost), e o email é constante, cada
 * requisição consome 1 token do mesmo bucket. Após 10 tokens, a 11ª
 * chamada retorna 429.
 *
 * <p>Todas as chamadas são síncronas e sequenciais para garantir que o
 * bucket não reabasteça entre requisições (período configurado para 1 min).
 */
@QuarkusTest
@TestProfile(JwtTestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoginRateLimiterTest {

    private static final String EMAIL = "ratelimit@fiap.br";
    private static final String PASSWORD = "abc123";

    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {"user"})
    void seed_user_for_rate_limit_test() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Rate Limit User","email":"%s","password":"%s"}
                        """.formatted(EMAIL, PASSWORD))
                .when()
                .post("/users")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    void login_after11Attempts_returns429() {
        // Primeiras 10 chamadas — devem retornar 200 (credenciais válidas)
        // ou 401 (se algo deu errado), mas NUNCA 429
        for (int i = 0; i < 10; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .body("""
                            {"email":"%s","password":"%s"}
                            """.formatted(EMAIL, PASSWORD))
                    .when()
                    .post("/auth/login")
                    .then()
                    .statusCode(not(429));
        }

        // 11ª chamada — deve retornar 429 Too Many Requests
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s"}
                        """.formatted(EMAIL, PASSWORD))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(429)
                .header("Retry-After", notNullValue());
    }
}
