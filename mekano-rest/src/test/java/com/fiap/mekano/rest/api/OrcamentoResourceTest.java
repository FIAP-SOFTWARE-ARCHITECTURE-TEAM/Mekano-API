package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
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
class OrcamentoResourceTest {

    private static final String BASE_PATH = "/api/v1/orcamentos";
    private static final UUID ORCAMENTO_UUID = UUID.randomUUID();
    private static final UUID OS_UUID = UUID.randomUUID();

    @InjectMock
    OrcamentoServicePort orcamentoService;

    private static Orcamento mockOrcamento() {
        var itens = List.of(new ItemOrcamento("Troca de óleo", 1L, new BigDecimal("89.90")));
        return Orcamento.reconstitute(ORCAMENTO_UUID, "Orçamento completo", itens,
                new BigDecimal("89.90"), LocalDateTime.now(),
                StatusOrcamento.PENDENTE, OS_UUID, LocalDateTime.now().plusHours(72));
    }

    private static Orcamento mockOrcamentoAprovado() {
        var itens = List.of(new ItemOrcamento("Troca de óleo", 1L, new BigDecimal("89.90")));
        return Orcamento.reconstitute(ORCAMENTO_UUID, "Orçamento completo", itens,
                new BigDecimal("89.90"), LocalDateTime.now(),
                StatusOrcamento.APROVADO, OS_UUID, LocalDateTime.now().plusHours(72));
    }

    private static Orcamento mockOrcamentoReprovado() {
        var itens = List.of(new ItemOrcamento("Troca de óleo", 1L, new BigDecimal("89.90")));
        return Orcamento.reconstitute(ORCAMENTO_UUID, "Orçamento completo", itens,
                new BigDecimal("89.90"), LocalDateTime.now(),
                StatusOrcamento.REPROVADO, OS_UUID, LocalDateTime.now().plusHours(72));
    }

    @BeforeEach
    void setUp() {
        var pendente = mockOrcamento();
        var aprovado = mockOrcamentoAprovado();
        var reprovado = mockOrcamentoReprovado();

        Mockito.when(orcamentoService.gerarOrcamento(Mockito.any()))
                .thenReturn(pendente);

        Mockito.when(orcamentoService.buscarPorId(ORCAMENTO_UUID))
                .thenReturn(pendente);

        Mockito.when(orcamentoService.buscarPorId(Mockito.argThat(id -> !id.equals(ORCAMENTO_UUID))))
                .thenThrow(new AppException(404, "Orçamento não encontrado"));

        Mockito.when(orcamentoService.aprovar(Mockito.any()))
                .thenReturn(aprovado);

        Mockito.when(orcamentoService.reprovar(Mockito.any()))
                .thenReturn(reprovado);
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void gerar_comDadosValidos_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"ordemServicoUuid": "%s", "descricao": "Orçamento completo", "itens": [{"descricao": "Troca de óleo", "quantidade": 1, "valorUnitario": 89.90}]}
                        """.formatted(OS_UUID.toString()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("descricao", equalTo("Orçamento completo"))
                .body("status", equalTo("PENDENTE"))
                .body("valorTotal", equalTo(89.90f))
                .header("Location", notNullValue());
    }

    @Test
    @Order(2)
    @TestSecurity(user = "admin", roles = {"admin"})
    void gerar_semItens_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"ordemServicoUuid": "%s", "descricao": "Sem itens", "itens": []}
                        """.formatted(OS_UUID.toString()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(3)
    @TestSecurity(user = "cliente", roles = {"cliente"})
    void aprovar_comDadosValidos_returns200() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(BASE_PATH + "/" + ORCAMENTO_UUID + "/aprovar")
                .then()
                .statusCode(200)
                .body("id", equalTo(ORCAMENTO_UUID.toString()))
                .body("status", equalTo("APROVADO"));
    }

    @Test
    @Order(4)
    @TestSecurity(user = "cliente", roles = {"cliente"})
    void reprovar_comMotivo_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Orçamento muito caro"}
                        """)
                .when()
                .post(BASE_PATH + "/" + ORCAMENTO_UUID + "/reprovar")
                .then()
                .statusCode(200)
                .body("id", equalTo(ORCAMENTO_UUID.toString()))
                .body("status", equalTo("REPROVADO"));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = {"admin"})
    void buscarPorId_existente_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + ORCAMENTO_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(ORCAMENTO_UUID.toString()))
                .body("status", equalTo("PENDENTE"));
    }

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void buscarPorId_inexistente_returns404() {
        given()
                .when()
                .get(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(7)
    void gerar_semAutenticacao_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"ordemServicoUuid": "%s", "descricao": "X", "itens": [{"descricao": "Y", "quantidade": 1, "valorUnitario": 10.0}]}
                        """.formatted(OS_UUID.toString()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(401);
    }
}
