package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.in.VeiculoServicePort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "atendente-test", roles = {"atendente"})
class VeiculoGetResourceTest {

    private static final String BASE_PATH = "/api/v1/veiculos";
    private static final UUID VEICULO_ATIVO = UUID.randomUUID();
    private static final UUID VEICULO_INATIVO = UUID.randomUUID();

    @InjectMock
    VeiculoServicePort veiculoService;

    @BeforeEach
    void setUp() {
        UUID clienteUuid = UUID.randomUUID();

        Veiculo ativo = Veiculo.reconstitute(
                VEICULO_ATIVO, clienteUuid, "ABC1234", "Toyota", "Corolla", 2020,
                LocalDateTime.now(), true);

        Veiculo inativo = Veiculo.reconstitute(
                VEICULO_INATIVO, clienteUuid, "DEF5678", "Honda", "Civic", 2021,
                LocalDateTime.now(), false);

        when(veiculoService.findById(VEICULO_ATIVO)).thenReturn(ativo);
        when(veiculoService.findById(VEICULO_INATIVO)).thenReturn(inativo);
    }

    @Test
    void getById_ativo_returns200ComIsActiveTrue() {
        given()
                .when()
                .get(BASE_PATH + "/" + VEICULO_ATIVO)
                .then()
                .statusCode(200)
                .body("id", equalTo(VEICULO_ATIVO.toString()))
                .body("isActive", equalTo(true));
    }

    @Test
    void getById_inativo_returns200ComIsActiveFalse() {
        given()
                .when()
                .get(BASE_PATH + "/" + VEICULO_INATIVO)
                .then()
                .statusCode(200)
                .body("isActive", equalTo(false));
    }
}