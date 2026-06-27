package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testes de integração REST para /servicos.
 *
 * <p>Acesso admin exclusivo — usa {@code @TestSecurity(roles = "admin")}.
 * Ordenação intencional: testes compartilham estado do banco.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServicoResourceTest {

    private static final String BASE_PATH = "/api/v1/servicos";
    private static String createdUuid;

    // ───────────────── CREATE ─────────────────

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validServico_returns201() {
        createdUuid = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Troca de óleo", "descricao": "Óleo sintético 5W30", "valor": 89.90}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Troca de óleo"))
                .body("descricao", equalTo("Óleo sintético 5W30"))
                .body("valor", equalTo(89.90f))
                .body("createdAt", notNullValue())
                .header("Location", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_duplicateName_returns409() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Troca de óleo", "descricao": "Outra desc", "valor": 100.00}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"))
                .body("status", equalTo(409))
                .body("detail", notNullValue());
    }

    @Test
    @Order(3)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_valorZero_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Serviço Grátis", "descricao": "desc", "valor": 0}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_valorNegativo_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Serviço Negativo", "descricao": "desc", "valor": -10.00}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_missingFields_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    // ───────────────── GET BY ID ─────────────────

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getById_existingServico_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("nome", equalTo("Troca de óleo"));
    }

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getById_nonExisting_returns404() {
        given()
                .when()
                .get(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    // ───────────────── LIST ─────────────────

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listAll_returns200WithPagination() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThan(0))
                .body("page", equalTo(0))
                .body("totalElements", greaterThan(0));
    }

    // ───────────────── UPDATE ─────────────────

    @Test
    @Order(9)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listAll_withInvalidPagination_returns200WithSanitizedValues() {
        given()
                .queryParam("page", -1)
                .queryParam("size", 0)
                .queryParam("sort", " ")
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(10));
    }

    @Test
    @Order(10)
    @TestSecurity(user = "admin", roles = {"admin"})
    void update_validData_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Troca de óleo sintético", "descricao": "Óleo premium 5W30", "valor": 129.90}
                        """)
                .when()
                .put(BASE_PATH + "/" + createdUuid)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Troca de óleo sintético"))
                .body("descricao", equalTo("Óleo premium 5W30"))
                .body("valor", equalTo(129.90f));
    }

    @Test
    @Order(11)
    @TestSecurity(user = "admin", roles = {"admin"})
    void update_nonExisting_returns404() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "X", "descricao": "Y", "valor": 10.00}
                        """)
                .when()
                .put(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    // ───────────────── DELETE ─────────────────

    @Test
    @Order(12)
    @TestSecurity(user = "admin", roles = {"admin"})
    void delete_existingServico_returns204() {
        given()
                .when()
                .delete(BASE_PATH + "/" + createdUuid)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(13)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getById_afterDelete_returns404() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid)
                .then()
                .statusCode(404);
    }

    // ───────────────── AUTHORIZATION ─────────────────

    @Test
    @Order(14)
    @TestSecurity(user = "atendente", roles = {"user"})
    void create_asAtendente_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "Alinhamento", "descricao": "desc", "valor": 120.00}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(15)
    @TestSecurity(user = "atendente", roles = {"user"})
    void listAll_asAtendente_returns403() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(16)
    @TestSecurity(user = "atendente", roles = {"user"})
    void delete_asAtendente_returns403() {
        given()
                .when()
                .delete(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(403);
    }

    @Test
    @Order(17)
    @TestSecurity(user = "atendente", roles = {"user"})
    void update_asAtendente_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nome": "X", "descricao": "Y", "valor": 10.00}
                        """)
                .when()
                .put(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(403);
    }
}
