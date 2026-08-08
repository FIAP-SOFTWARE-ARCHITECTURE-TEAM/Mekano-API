package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.nfentrada.NfEntradaService;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NfEntradaResourceTest {

    private static final String BASE_PATH = "/api/v1/nf-entrada";
    private static final UUID NF_UUID = UUID.randomUUID();
    private static final UUID PECA_UUID = UUID.randomUUID();
    private static final UUID REQUISICAO_UUID = UUID.randomUUID();

    @Inject
    NfEntradaService nfEntradaService;

    @InjectMock
    NfEntradaRepositoryPort nfEntradaRepository;

    @InjectMock
    PecaRepositoryPort pecaRepository;

    @InjectMock
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @InjectMock
    EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        var now = LocalDateTime.now();
        var mockNf = NfEntrada.reconstitute(
                NF_UUID,
                "35200612345678000190550000001234567890123456",
                new BigDecimal("1875.00"),
                PECA_UUID, REQUISICAO_UUID, now);

        // Mock requisicao in PRODUTO_RECEBIDO status
        var mockRequisicao = RequisicaoCompra.reconstitute(
                REQUISICAO_UUID, PECA_UUID, 5L,
                StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, now);

        Mockito.when(requisicaoRepository.buscarPorId(REQUISICAO_UUID))
                .thenReturn(Optional.of(mockRequisicao));

        Mockito.when(nfEntradaRepository.salvar(any()))
                .thenReturn(mockNf);

        Mockito.when(nfEntradaRepository.buscarPorId(NF_UUID))
                .thenReturn(Optional.of(mockNf));

        Mockito.when(nfEntradaRepository.buscarPorId(Mockito.argThat(id -> !id.equals(NF_UUID))))
                .thenReturn(Optional.empty());

        Mockito.when(pecaRepository.buscarPorId(PECA_UUID))
                .thenReturn(Optional.empty());
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validNfEntrada_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"chaveAcesso": "35200612345678000190550000001234567890123456", "valorTotal": 1875.00, "requisicaoCompraId": "%s"}
                        """.formatted(REQUISICAO_UUID))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("chaveAcesso", equalTo("35200612345678000190550000001234567890123456"))
                .body("valorTotal", equalTo(1875.00f))
                .header("Location", notNullValue());

        // EST-06: verificar que o saldo da peça foi creditado com a quantidade correta
        verify(pecaRepository).creditarSaldo(PECA_UUID, 5);
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
                .get(BASE_PATH + "/" + NF_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(NF_UUID.toString()))
                .body("chaveAcesso", equalTo("35200612345678000190550000001234567890123456"))
                .body("valorTotal", equalTo(1875.00f));
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
    @TestSecurity(user = "atendente", roles = {"user"})
    void create_asAtendente_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"chaveAcesso": "35200612345678000190550000001234567890123456", "valorTotal": 100.00, "requisicaoCompraId": "%s"}
                        """.formatted(REQUISICAO_UUID))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(6)
    @TestSecurity(user = "atendente", roles = {"user"})
    void getById_asAtendente_returns403() {
        given()
                .when()
                .get(BASE_PATH + "/" + NF_UUID)
                .then()
                .statusCode(403);
    }
}