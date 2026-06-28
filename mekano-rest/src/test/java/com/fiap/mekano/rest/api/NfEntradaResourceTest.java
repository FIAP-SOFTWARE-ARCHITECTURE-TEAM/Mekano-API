package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.nfentrada.CreateNfEntradaResponse;
import com.fiap.mekano.application.service.nfentrada.NfEntradaService;
import com.fiap.mekano.domain.model.NfEntrada;
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

import static org.mockito.ArgumentMatchers.any;

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

    @BeforeEach
    void setUp() {
        var now = LocalDateTime.now();
        var mockNf = NfEntrada.reconstitute(
                NF_UUID,
                "35200612345678000190550000001234567890123456",
                new BigDecimal("1875.00"),
                PECA_UUID, REQUISICAO_UUID, now);

        Mockito.when(nfEntradaService.registrar(any()))
                .thenReturn(new CreateNfEntradaResponse(NF_UUID,
                        "35200612345678000190550000001234567890123456",
                        new BigDecimal("1875.00"),
                        PECA_UUID, REQUISICAO_UUID, now));

        Mockito.when(nfEntradaService.buscarPorId(NF_UUID))
                .thenReturn(mockNf);

        Mockito.when(nfEntradaService.buscarPorId(Mockito.argThat(id -> !id.equals(NF_UUID))))
                .thenThrow(new com.fiap.mekano.domain.exception.AppException(404, "NF não encontrada"));
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
