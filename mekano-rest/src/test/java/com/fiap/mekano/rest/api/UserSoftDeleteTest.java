package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestSecurity(user = "testuser", roles = {"admin", "user"})
class UserSoftDeleteTest {

    @Test
    void createUser_forSoftDeleteTest() {
        String email = uniqueEmail("create");

        given()
                .contentType("application/json")
                .body("""
                      {
                        "name": "Delete Me",
                        "email": "%s",
                        "password": "123456"
                      }
                      """.formatted(email))
        .when()
                .post("/api/v1/users")
        .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void delete_softDelete_returns204() {
        String userId = createUserAndReturnId();

        given()
        .when()
                .delete("/api/v1/users/{id}", userId)
        .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    @Test
    void get_afterDelete_returns404() {
        String userId = createUserAndReturnId();

        given()
        .when()
                .delete("/api/v1/users/{id}", userId)
        .then()
                .log().ifValidationFails()
                .statusCode(204);

        given()
        .when()
                .get("/api/v1/users/{id}", userId)
        .then()
                .log().ifValidationFails()
                .statusCode(404)
                .contentType(containsString("application/problem+json"))
                .body("detail", notNullValue())
                .body("type", equalTo("about:blank"))
                .body("title", equalTo("Not Found"))
                .body("status", equalTo(404));
    }

    private String createUserAndReturnId() {
        String email = "softdelete-" + UUID.randomUUID() + "@mekano.com";

        var response =
                given()
                        .contentType("application/json")
                        .body("""
                              {
                                "name": "Delete Me",
                                "email": "%s",
                                "password": "123456"
                              }
                              """.formatted(email))
                .when()
                        .post("/api/v1/users")
                .then()
                        .log().all()
                        .extract();

        int statusCode = response.statusCode();

        if (statusCode != 201) {
            throw new AssertionError(
                    "Esperado 201, mas veio " + statusCode +
                    ". Body: " + response.asString()
            );
        }

        String id = response.path("id");

        if (id == null) {
            id = response.path("uuid");
        }

        if (id == null) {
            throw new AssertionError(
                    "Resposta não contém id nem uuid. Body: " + response.asString()
            );
        }

        return id;
    }

    private String uniqueEmail(String prefix) {
        return "softdelete-" + prefix + "-" + UUID.randomUUID() + "@mekano.com";
    }
}