package com.fiap.mekano.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testes de integração REST para soft delete de usuários (Phase 9, UAT-4).
 *
 * <p>Ciclo: criar usuário → deletar (204) → GET retorna 404 (soft delete).
 *
 * <p>DevServices: %test profile ativa Testcontainers PostgreSQL automaticamente.
 * {@code %test.quarkus.flyway.clean-at-start=true} limpa o schema antes de cada test run.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserSoftDeleteTest {

    private static String createdUserId;

    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {"user"})
    void createUser_forSoftDeleteTest() {
        createdUserId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Delete Me","email":"softdelete@fiap.br","password":"abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .extract().path("id").toString();
    }

    @Test
    @Order(2)
    @TestSecurity(user = "testuser", roles = {"user"})
    void delete_softDelete_returns204() {
        given()
                .when()
                .delete("/api/v1/users/" + createdUserId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(3)
    @TestSecurity(user = "testuser", roles = {"user"})
    void get_afterDelete_returns404() {
        given()
                .when()
                .get("/api/v1/users/" + createdUserId)
                .then()
                .statusCode(404)
                .body("message", notNullValue());
    }
}
