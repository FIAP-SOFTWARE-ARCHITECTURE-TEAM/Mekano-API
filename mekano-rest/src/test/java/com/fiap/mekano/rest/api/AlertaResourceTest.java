package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.domain.model.Peca;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
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
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlertaResourceTest {

    private static final String BASE_PATH = "/api/v1/alertas";

    @InjectMock
    PecaService pecaService;

    @BeforeEach
    void setUp() {
        var abaixoMinimo = Peca.reconstitute(
                UUID.randomUUID(), "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 3L, 10L, LocalDateTime.now());

        var acimaMinimo = Peca.reconstitute(
                UUID.randomUUID(), "PEA-002", "Filtro de Óleo",
                new BigDecimal("15.50"), 50L, 5L, LocalDateTime.now());

        Mockito.when(pecaService.listarAbaixoEstoqueMinimo())
                .thenReturn(List.of(abaixoMinimo));
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listAlertas_filtersLowStock_returns200() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    @Order(2)
    @TestSecurity(user = "atendente", roles = {"atendente"})
    void listAlertas_asAtendente_returns200() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @TestSecurity(user = "user", roles = {"user"})
    void listAlertas_asUser_returns403() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(403);
    }
}
