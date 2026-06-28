package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.nfentrada.CreateNfEntradaResponse;
import com.fiap.mekano.application.service.nfentrada.NfEntradaService;
import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.infrastructure.repository.NfEntradaPanacheRepository;
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
import java.util.Optional;
import java.util.UUID;

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

    @InjectMock
    NfEntradaService nfEntradaService;

    @InjectMock
    NfEntradaRepositoryPort nfEntradaRepository;

    @InjectMock
    NfEntradaPanacheRepository nfEntradaPanacheRepository;

    @BeforeEach
    void setUp() {
        var now = LocalDateTime.now();
        var mockNf = NfEntrada.reconstitute(
                NF_UUID, "123456", "1", "12345678000190",
                "Auto Peças Ltda", now, new BigDecimal("1500.00"),
                new BigDecimal("270.00"), new BigDecimal("75.00"),
                new BigDecimal("30.00"), new BigDecimal("1875.00"),
                "35200612345678000190550000001234567890123456", now);

        Mockito.when(nfEntradaService.registrar(Mockito.any()))
                .thenReturn(new CreateNfEntradaResponse(NF_UUID, 10, now));

        Mockito.when(nfEntradaRepository.buscarPorId(NF_UUID))
                .thenReturn(Optional.of(mockNf));

        Mockito.when(nfEntradaRepository.buscarPorId(Mockito.argThat(id -> !id.equals(NF_UUID))))
                .thenReturn(Optional.empty());

        // listAll setup not needed — tested via real integration
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validNfEntrada_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"numero": "123456", "serie": "1", "cnpjFornecedor": "12345678000190", "nomeFornecedor": "Auto Peças Ltda", "dataEmissao": "2026-06-01T10:00:00", "valorMercadoria": 1500.00, "icms": 270.00, "ipi": 75.00, "outrosImpostos": 30.00, "chaveAcesso": "35200612345678000190550000001234567890123456", "pecaId": "%s", "requisicaoCompraId": "%s", "quantidade": 10}
                        """.formatted(PECA_UUID, REQUISICAO_UUID))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("numero", equalTo("123456"))
                .body("cnpjFornecedor", equalTo("12345678000190"))
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
                .get(BASE_PATH + "/" + NF_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(NF_UUID.toString()))
                .body("numero", equalTo("123456"))
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
                        {"numero": "654321", "serie": "1", "cnpjFornecedor": "12345678000190", "nomeFornecedor": "Teste", "dataEmissao": "2026-06-01T10:00:00", "valorMercadoria": 100.00, "icms": 10.00, "ipi": 5.00, "outrosImpostos": 1.00, "chaveAcesso": "35200612345678000190550000001234567890123456", "pecaId": "%s", "requisicaoCompraId": "%s", "quantidade": 5}
                        """.formatted(PECA_UUID, REQUISICAO_UUID))
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
