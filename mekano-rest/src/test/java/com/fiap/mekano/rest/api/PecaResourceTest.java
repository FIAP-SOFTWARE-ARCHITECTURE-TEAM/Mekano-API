package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.CreatePecaResponse;
import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.UnidadeMedida;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.repository.PecaPanacheRepository;
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
class PecaResourceTest {

    private static final String BASE_PATH = "/api/v1/pecas";
    private static final UUID PECA_UUID = UUID.randomUUID();

    @InjectMock
    PecaService pecaService;

    @InjectMock
    PecaRepositoryPort pecaRepository;

    @InjectMock
    PecaPanacheRepository pecaPanacheRepository;

    @BeforeEach
    void setUp() {
        var mockPeca = Peca.reconstitute(
                PECA_UUID, "PEA-001", "Óleo do Motor 5W30",
                UnidadeMedida.LITRO, new BigDecimal("45.90"),
                50L, 10L, LocalDateTime.now());

        Mockito.when(pecaService.criar(Mockito.any()))
                .thenReturn(new CreatePecaResponse(PECA_UUID, "Óleo do Motor 5W30", 50, 10));

        Mockito.when(pecaRepository.buscarPorId(PECA_UUID))
                .thenReturn(Optional.of(mockPeca));

        Mockito.when(pecaRepository.buscarPorId(Mockito.argThat(id -> !id.equals(PECA_UUID))))
                .thenReturn(Optional.empty());

        // listAll setup not needed — tested via real integration
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validPeca_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "PEA-001", "descricao": "Óleo do Motor 5W30", "unidadeMedida": "LITRO", "valorUnitario": 45.90, "estoqueMinimo": 10}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("descricao", equalTo("Óleo do Motor 5W30"))
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
                .get(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(PECA_UUID.toString()))
                .body("descricao", equalTo("Óleo do Motor 5W30"))
                .body("saldoAtual", equalTo(50));
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
                        {"codigo": "PEA-002", "descricao": "Teste", "unidadeMedida": "UNIDADE", "valorUnitario": 10.00}
                        """)
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
                .get(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(403);
    }
}
