package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.application.service.requisicao.CreateRequisicaoCompraResponse;
import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RequisicaoCompraResourceTest {

    private static final String BASE_PATH = "/api/v1/requisicoes-compra";
    private static final UUID REQUISICAO_UUID = UUID.randomUUID();
    private static final UUID PECA_UUID = UUID.randomUUID();

    @InjectMock
    RequisicaoCompraService requisicaoService;

    @InjectMock
    PecaService pecaService;

    @BeforeEach
    void setUp() {
        var mockPeca = Peca.reconstitute(
                PECA_UUID, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 50L, 10L, LocalDateTime.now());

        var mockRequisicao = RequisicaoCompra.reconstitute(
                REQUISICAO_UUID, PECA_UUID, 10L,
                StatusRequisicao.ABERTA, com.fiap.mekano.domain.model.MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        Mockito.when(pecaService.buscarPorId(PECA_UUID))
                .thenReturn(mockPeca);

        Mockito.when(requisicaoService.criar(Mockito.any()))
                .thenReturn(new CreateRequisicaoCompraResponse(REQUISICAO_UUID, PECA_UUID, 10L,
                        "ABERTA", "ESTOQUE_MINIMO", LocalDateTime.now()));

        Mockito.when(requisicaoService.buscarPorId(REQUISICAO_UUID))
                .thenReturn(mockRequisicao);

        Mockito.when(requisicaoService.buscarPorId(Mockito.argThat(id -> !id.equals(REQUISICAO_UUID))))
                .thenThrow(new com.fiap.mekano.domain.exception.AppException(404, "Requisição não encontrada"));
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validRequisicao_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"pecaId": "%s", "quantidade": 10, "motivo": "ESTOQUE_MINIMO"}
                        """.formatted(PECA_UUID))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("status", equalTo("ABERTA"))
                .body("quantidade", equalTo(10))
                .header("Location", notNullValue());
    }

    @Test
    @Order(2)
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

    @Test
    @Order(3)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getById_existing_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + REQUISICAO_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(REQUISICAO_UUID.toString()))
                .body("status", equalTo("ABERTA"))
                .body("quantidade", equalTo(10));
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getById_nonExisting_returns404() {
        given()
                .when()
                .get(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = {"admin"})
    void enviar_aberta_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + REQUISICAO_UUID + "/enviar")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void cancelar_aberta_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + REQUISICAO_UUID + "/cancelar")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    @TestSecurity(user = "atendente", roles = {"user"})
    void create_asAtendente_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"pecaId": "%s", "quantidade": 5, "motivo": "ESTOQUE_MINIMO"}
                        """.formatted(PECA_UUID))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(8)
    @TestSecurity(user = "atendente", roles = {"user"})
    void getById_asAtendente_returns403() {
        given()
                .when()
                .get(BASE_PATH + "/" + REQUISICAO_UUID)
                .then()
                .statusCode(403);
    }
}
