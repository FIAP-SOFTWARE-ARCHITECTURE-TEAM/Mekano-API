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

    // ─────────────── GET STATUS (público) ───────────────

    @Test
    @Order(3)
    void getStatus_withoutAuth_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid + "/status")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("status", equalTo("EM_DIAGNOSTICO"))
                .body("dataEntrada", notNullValue());
    }

    // ─────────────── FINALIZAR DIAGNOSTICO ───────────────

    @Test
    @Order(4)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void finalizarDiagnostico_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/finalizar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("AGUARDANDO_APROVACAO"));
    }

    // ─────────────── GET STATUS (após diagnóstico) ───────────────

    @Test
    @Order(5)
    void getStatus_aguardandoAprovacao_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("AGUARDANDO_APROVACAO"));
    }

    // ─────────────── CANCELAR ───────────────

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void cancelar_os_aguardando_aprovacao_returns_200() {
        // Criar nova OS e levar até AGUARDANDO_APROVACAO
        String osId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Freio com ruído"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(BASE_PATH)
                .then().statusCode(201).extract().path("id");

        given().put(BASE_PATH + "/" + osId + "/iniciar-diagnostico").then().statusCode(200);
        given().put(BASE_PATH + "/" + osId + "/finalizar-diagnostico").then().statusCode(200);

        // Cancelar de AGUARDANDO_APROVACAO
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Cliente desistiu do serviço"}
                        """)
                .when()
                .put(BASE_PATH + "/" + osId + "/cancelar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"))
                .body("motivoCancelamento", equalTo("Cliente desistiu do serviço"));
    }
}
