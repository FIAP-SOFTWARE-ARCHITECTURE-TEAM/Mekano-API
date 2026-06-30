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
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PagamentoResourceTest {

    private static final String OS_PATH = "/api/v1/os";
    private static final String PECA_PATH = "/api/v1/pecas";
    private static final String ORCAMENTO_PATH = "/api/v1/orcamentos";

    private static String osId;
    private static String pecaId;

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void createPecaEOS() {
        pecaId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "E2E-TEST-001", "descricao": "Peca E2E", \
                         "unidadeMedida": "UNIDADE", "valorUnitario": 150.00}
                        """)
                .when()
                .post(PECA_PATH)
                .then()
                .statusCode(201)
                .extract().path("id");

        osId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", \
                         "descricaoProblema": "E2E fluxo pagamento"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(OS_PATH)
                .then()
                .statusCode(201)
                .body("status", equalTo("RECEBIDA"))
                .body("statusPagamento", equalTo("NAO_COBRADO"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void diagnostico() {
        given()
                .when()
                .put(OS_PATH + "/" + osId + "/iniciar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_DIAGNOSTICO"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao": "Diagnostico E2E",
                         "itens": [{"referenciaUuid": "%s", "tipo": "PECA", "quantidade": 1}]}
                        """.formatted(pecaId))
                .when()
                .put(OS_PATH + "/" + osId + "/finalizar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("AGUARDANDO_APROVACAO"));
    }

    @Test
    @Order(3)
    @TestSecurity(user = "cliente", roles = {"cliente"})
    void aprovarOrcamento() {
        String orcUuid = given()
                .when()
                .get(ORCAMENTO_PATH + "?osUuid=" + osId)
                .then()
                .statusCode(200)
                .extract().path("id");

        given()
                .when()
                .post(ORCAMENTO_PATH + "/" + orcUuid + "/aprovar")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = {"admin"})
    void finalizarExecucao() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"observacao": "Concluido E2E"}
                        """)
                .when()
                .put(OS_PATH + "/" + osId + "/finalizar-execucao")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINALIZADA"));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = {"admin"})
    void entregaSemPagamento_retorna422() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"recebidoPor": "Cliente"}
                        """)
                .when()
                .patch(OS_PATH + "/" + osId + "/entregar")
                .then()
                .statusCode(422);
    }

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void confirmarPagamento() {
        given()
                .when()
                .patch(OS_PATH + "/" + osId + "/confirmar-pagamento")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"))
                .body("transacaoId", startsWith("MOCK-"))
                .body("osUuid", equalTo(osId))
                .body("valorCobrado", notNullValue())
                .body("dataPagamento", notNullValue());
    }

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = {"admin"})
    void entregar() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"recebidoPor": "Cliente E2E"}
                        """)
                .when()
                .patch(OS_PATH + "/" + osId + "/entregar")
                .then()
                .statusCode(200)
                .body("status", equalTo("ENTREGUE"))
                .body("statusPagamento", equalTo("CONFIRMADO"))
                .body("statusEntrega", equalTo("ENTREGUE"))
                .body("recebidoPor", equalTo("Cliente E2E"));
    }

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getVerifyEstadoFinal() {
        given()
                .when()
                .get(OS_PATH + "/" + osId)
                .then()
                .statusCode(200)
                .body("status", equalTo("ENTREGUE"))
                .body("statusPagamento", equalTo("CONFIRMADO"))
                .body("statusEntrega", equalTo("ENTREGUE"))
                .body("valorCobrado", notNullValue())
                .body("referenciaPagamento", startsWith("MOCK-"))
                .body("recebidoPor", equalTo("Cliente E2E"))
                .body("pagamentoConfirmadoEm", notNullValue())
                .body("entregueEm", notNullValue());
    }

    @Test
    @Order(9)
    @TestSecurity(user = "admin", roles = {"admin"})
    void pagamentoSemCobranca_retorna409() {
        String novaOsId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", \
                         "descricaoProblema": "Erro pagamento"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(OS_PATH)
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .when()
                .patch(OS_PATH + "/" + novaOsId + "/confirmar-pagamento")
                .then()
                .statusCode(409);
    }
}
