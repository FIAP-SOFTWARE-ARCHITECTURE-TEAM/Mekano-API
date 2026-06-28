package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class AdminUserResourceSecurityTest {

    @Test
    @TestSecurity(user = "cliente-test", roles = {"cliente"})
    void post_quandoNaoAdmin_deveRetornar403() {
        given()
                .contentType("application/json")
                .body("""
                      {
                        "name": "User Teste",
                        "email": "user.teste@mekano.com",
                        "role": "admin"
                      }
                      """)
        .when()
                .post("/api/v1/admin/usuarios")
        .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "cliente-test", roles = {"cliente"})
    void get_quandoNaoAdmin_deveRetornar403() {
        given()
        .when()
                .get("/api/v1/admin/usuarios")
        .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "cliente-test", roles = {"cliente"})
    void delete_quandoNaoAdmin_deveRetornar403() {
        given()
        .when()
                .delete("/api/v1/admin/usuarios/{uuid}", UUID.randomUUID())
        .then()
                .statusCode(403);
    }
}
