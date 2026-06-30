package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdemDeServicoResourceTest {

    private static final String BASE_PATH = "/api/v1/os";
    private static String createdUuid;

    // ─────────────── CREATE ───────────────

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_asAdmin_returns201() {
        createdUuid = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "clienteId": "%s",
                          "veiculoId": "%s",
                          "descricaoProblema": "Motor falhando ao acelerar"
                        }
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("RECEBIDA"))
                .body("descricaoProblema", equalTo("Motor falhando ao acelerar"))
                .header("Location", notNullValue())
                .extract().path("id");
    }

    // ─────────────── UPDATE ───────────────

    @Test
    @Order(20)
    @TestSecurity(user = "admin", roles = {"admin"})
    void update_emRecebida_returns200() {
        // Criar nova OS para testar update
        UUID novoCliente = UUID.randomUUID();
        UUID novoVeiculo = UUID.randomUUID();

        String osId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Problema original"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(BASE_PATH)
                .then().statusCode(201).extract().path("id");

        // Update com novos dados
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Problema corrigido"}
                        """.formatted(novoCliente, novoVeiculo))
                .when()
                .put(BASE_PATH + "/" + osId)
                .then()
                .statusCode(200)
                .body("clienteId", equalTo(novoCliente.toString()))
                .body("veiculoId", equalTo(novoVeiculo.toString()))
                .body("descricaoProblema", equalTo("Problema corrigido"))
                .body("status", equalTo("RECEBIDA"));

        // Confirmar persistência via GET
        given()
                .when()
                .get(BASE_PATH + "/" + osId)
                .then()
                .statusCode(200)
                .body("descricaoProblema", equalTo("Problema corrigido"));
    }

    @Test
    @Order(21)
    @TestSecurity(user = "admin", roles = {"admin"})
    void update_foraDeRecebida_returns422() {
        // Criar OS e avançar para EM_DIAGNOSTICO
        String osId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Problema"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(BASE_PATH)
                .then().statusCode(201).extract().path("id");

        given().put(BASE_PATH + "/" + osId + "/iniciar-diagnostico").then().statusCode(200);

        // Tentar update em EM_DIAGNOSTICO — deve falhar
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Novo problema"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .put(BASE_PATH + "/" + osId)
                .then()
                .statusCode(422);
    }

    @Test
    @Order(22)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void update_asMecanico_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "teste"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .put(BASE_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    // ─────────────── INICIAR DIAGNOSTICO ───────────────

    @Test
    @Order(2)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void iniciarDiagnostico_asMecanico_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/iniciar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_DIAGNOSTICO"));
    }

    // ─────────────── GET STATUS ───────────────

    @Test
    @Order(3)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getStatus_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid + "/status")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("status", equalTo("EM_DIAGNOSTICO"))
                .body("dataEntrada", notNullValue());
    }

    // ─────────────── AUTORIZAÇÃO ───────────────

    @Test
    @Order(8)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void create_asMecanico_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "teste"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(9)
    void listAll_withoutAuth_returns401() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(401);
    }
}
