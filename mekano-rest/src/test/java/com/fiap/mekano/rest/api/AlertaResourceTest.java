package com.fiap.mekano.rest.api;

import com.fiap.mekano.infrastructure.entity.PecaEntity;
import com.fiap.mekano.infrastructure.repository.PecaPanacheRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlertaResourceTest {

    private static final String BASE_PATH = "/api/v1/alertas";

    @InjectMock
    PecaPanacheRepository pecaPanacheRepository;

    @BeforeEach
    void setUp() {
        var abaixoMinimo = new PecaEntity();
        abaixoMinimo.uuid = UUID.randomUUID();
        abaixoMinimo.descricao = "Óleo do Motor 5W30";
        abaixoMinimo.saldo = 3;
        abaixoMinimo.estoqueMinimo = 10;

        var acimaMinimo = new PecaEntity();
        acimaMinimo.uuid = UUID.randomUUID();
        acimaMinimo.descricao = "Filtro de Óleo";
        acimaMinimo.saldo = 50;
        acimaMinimo.estoqueMinimo = 5;

        Mockito.when(pecaPanacheRepository.listAll())
                .thenReturn(List.of(abaixoMinimo, acimaMinimo));
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
