package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.application.service.requisicao.CreateRequisicaoCompraResponse;
import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.domain.model.MotivoRequisicao;
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
import java.util.List;
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
    private static final UUID REQUISICAO_OS_UUID = UUID.randomUUID();
    private static final UUID PECA_UUID_1 = UUID.randomUUID();
    private static final UUID PECA_UUID_2 = UUID.randomUUID();

    @InjectMock
    RequisicaoCompraService requisicaoService;

    @InjectMock
    PecaService pecaService;

    @BeforeEach
    void setUp() {
        var mockPeca1 = Peca.reconstitute(
                PECA_UUID_1, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 50L, 10L, LocalDateTime.now(), 0L);
        var mockPeca2 = Peca.reconstitute(
                PECA_UUID_2, "PEA-002", "Filtro de Ar",
                new BigDecimal("25.00"), 30L, 5L, LocalDateTime.now(), 0L);

        var itens = List.of(
                new ItemRequisicaoCompra(PECA_UUID_1, 10L),
                new ItemRequisicaoCompra(PECA_UUID_2, 5L));

        var mockRequisicao = RequisicaoCompra.reconstitute(
                REQUISICAO_UUID, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        var mockRequisicaoOs = RequisicaoCompra.reconstitute(
                REQUISICAO_OS_UUID, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ORDEM_SERVICO, LocalDateTime.now());

        Mockito.when(pecaService.buscarPorId(Mockito.any()))
                .thenReturn(mockPeca1);

        Mockito.when(requisicaoService.criar(Mockito.any()))
                .thenReturn(new CreateRequisicaoCompraResponse(REQUISICAO_UUID,
                        List.of(new CreateRequisicaoCompraResponse.ItemRequisicaoCompraItemResponse(PECA_UUID_1, 10L),
                                new CreateRequisicaoCompraResponse.ItemRequisicaoCompraItemResponse(PECA_UUID_2, 5L)),
                        "ABERTA", "ESTOQUE_MINIMO", LocalDateTime.now()));

        Mockito.when(requisicaoService.buscarPorId(REQUISICAO_UUID))
                .thenReturn(mockRequisicao);

        Mockito.when(requisicaoService.buscarPorId(REQUISICAO_OS_UUID))
                .thenReturn(mockRequisicaoOs);

        Mockito.doThrow(new AppException(409,
                "Requisição de compra vinculada a Ordem de Serviço não pode ser cancelada"))
                .when(requisicaoService).cancelar(REQUISICAO_OS_UUID);

        Mockito.when(requisicaoService.buscarPorId(Mockito.argThat(id ->
                !id.equals(REQUISICAO_UUID) && !id.equals(REQUISICAO_OS_UUID))))
                .thenThrow(new AppException(404, "Requisição não encontrada"));
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validRequisicao_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "motivo": "ESTOQUE_MINIMO",
                          "itens": [
                            {"pecaUuid": "%s", "quantidade": 10},
                            {"pecaUuid": "%s", "quantidade": 5}
                          ]
                        }
                        """.formatted(PECA_UUID_1, PECA_UUID_2))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("status", equalTo("ABERTA"))
                .body("itens.size()", equalTo(2))
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
    void create_emptyItens_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "ESTOQUE_MINIMO", "itens": []}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getById_existing_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + REQUISICAO_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(REQUISICAO_UUID.toString()))
                .body("status", equalTo("ABERTA"))
                .body("itens.size()", equalTo(2));
    }

    @Test
    @Order(5)
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
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void enviar_aberta_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + REQUISICAO_UUID + "/enviar")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = {"admin"})
    void cancelar_aberta_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + REQUISICAO_UUID + "/cancelar")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = {"admin"})
    void cancelar_ordemServico_returns409() {
        given()
                .when()
                .put(BASE_PATH + "/" + REQUISICAO_OS_UUID + "/cancelar")
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(9)
    @TestSecurity(user = "atendente", roles = {"user"})
    void create_asAtendente_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "motivo": "ESTOQUE_MINIMO",
                          "itens": [{"pecaUuid": "%s", "quantidade": 5}]
                        }
                        """.formatted(PECA_UUID_1))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(10)
    @TestSecurity(user = "atendente", roles = {"user"})
    void getById_asAtendente_returns403() {
        given()
                .when()
                .get(BASE_PATH + "/" + REQUISICAO_UUID)
                .then()
                .statusCode(403);
    }
}
