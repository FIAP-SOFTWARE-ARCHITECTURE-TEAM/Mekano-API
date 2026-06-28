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

    // ─────────────── APROVAR ORCAMENTO ───────────────

    @Test
    @Order(5)
    @TestSecurity(user = "atendente", roles = {"atendente"})
    void aprovarOrcamento_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/aprovar-orcamento")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_EXECUCAO"));
    }

    // ─────────────── FINALIZAR ───────────────

    @Test
    @Order(6)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void finalizar_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/finalizar")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINALIZADA"));
    }

    // ─────────────── ENTREGAR ───────────────

    @Test
    @Order(7)
    @TestSecurity(user = "atendente", roles = {"atendente"})
    void entregar_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/entregar")
                .then()
                .statusCode(200)
                .body("status", equalTo("ENTREGUE"));
    }

    // ─────────────── TRANSIÇÃO INVÁLIDA ───────────────

    @Test
    @Order(8)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void iniciarDiagnostico_estadoTerminal_returns422() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/iniciar-diagnostico")
                .then()
                .statusCode(422);
    }

    // ─────────────── AUTORIZAÇÃO ───────────────

    @Test
    @Order(9)
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
    @Order(10)
    void listAll_withoutAuth_returns401() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(401);
    }

    // ─────────────── REPROVAR COM MOTIVO ───────────────

    @Test
    @Order(11)
    @TestSecurity(user = "admin", roles = {"admin"})
    void reprovarOrcamento_returns200() {
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

        // Reprovar
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Valor muito alto para o cliente"}
                        """)
                .when()
                .put(BASE_PATH + "/" + osId + "/reprovar-orcamento")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"))
                .body("motivoCancelamento", equalTo("Valor muito alto para o cliente"));
    }
}
