package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Testes de integração REST para POST /users.
 *
 * DevServices: %test profile não declara jdbc.url → Quarkus ativa Testcontainers PostgreSQL
 *              automaticamente com docker.io/library/postgres:16-alpine.
 *
 * Isolamento: %test.quarkus.flyway.clean-at-start=true limpa o schema antes de cada test run.
 *
 * Ordenação intencional (D-03): testes compartilham estado do banco.
 * Order(1) insere VALID_EMAIL; Order(2) tenta inserir o mesmo email → 409.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserResourceTest {

    private static final String VALID_EMAIL = "ana@fiap.br";

    @Test
    @Order(1)

    void create_validUser_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Ana", "email": "%s", "password": "abc123"}
                        """.formatted(VALID_EMAIL))
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Ana"))
                .body("email", equalTo(VALID_EMAIL))
                .body("createdAt", notNullValue())
                .body("passwordHash", nullValue());  // CRÍTICO: hash nunca deve aparecer na resposta
    }

    @Test
    @Order(2)

    void create_duplicateEmail_returns409() {
        // VALID_EMAIL foi inserido em Order(1) — este deve falhar com 409
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Ana2", "email": "%s", "password": "abc123"}
                        """.formatted(VALID_EMAIL))
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(409)
                .body("message", notNullValue());
    }

    @Test
    @Order(3)

    void create_invalidEmail_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Test", "email": "naoemail", "password": "abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(400)
                .body("violations", notNullValue());
    }

    @Test
    @Order(4)

    void create_missingFields_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(400)
                .body("violations", notNullValue());
}
}
