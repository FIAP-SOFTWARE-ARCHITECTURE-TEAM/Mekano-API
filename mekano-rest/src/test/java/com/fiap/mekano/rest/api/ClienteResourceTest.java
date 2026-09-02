package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.in.ClienteServicePort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "atendente-test", roles = {"atendente"})
class ClienteResourceTest {

    private static final String BASE_PATH = "/api/v1/clientes";
    private static final UUID CLIENTE_ATIVO = UUID.randomUUID();
    private static final UUID CLIENTE_INATIVO = UUID.randomUUID();

    @InjectMock
    ClienteServicePort clienteService;

    @BeforeEach
    void setUp() {
        Cliente ativo = Cliente.reconstitute(
                CLIENTE_ATIVO, "Cliente Ativo", "52998224725", "ativo@teste.com", null,
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000",
                LocalDateTime.now(), true);

        Cliente inativo = Cliente.reconstitute(
                CLIENTE_INATIVO, "Cliente Inativo", "11144477735", "inativo@teste.com", null,
                "Rua B", "200", "Centro", "São Paulo", "SP", "01001000",
                LocalDateTime.now(), false);

        when(clienteService.findClienteById(CLIENTE_ATIVO)).thenReturn(ativo);
        when(clienteService.findClienteById(CLIENTE_INATIVO)).thenReturn(inativo);
        when(clienteService.findAllClientes(0, 10, "nome,asc", null)).thenReturn(List.of(ativo, inativo));
        when(clienteService.countAllClientes(null)).thenReturn(2L);
        when(clienteService.findAllClientes(0, 10, "nome,asc", true)).thenReturn(List.of(ativo));
        when(clienteService.countAllClientes(true)).thenReturn(1L);
        when(clienteService.findAllClientes(0, 10, "nome,asc", false)).thenReturn(List.of(inativo));
        when(clienteService.countAllClientes(false)).thenReturn(1L);
    }

    @Test
    void getById_ativo_returns200ComIsActiveTrue() {
        given()
                .when()
                .get(BASE_PATH + "/" + CLIENTE_ATIVO)
                .then()
                .statusCode(200)
                .body("id", equalTo(CLIENTE_ATIVO.toString()))
                .body("isActive", equalTo(true));
    }

    @Test
    void getById_inativo_returns200ComIsActiveFalse() {
        given()
                .when()
                .get(BASE_PATH + "/" + CLIENTE_INATIVO)
                .then()
                .statusCode(200)
                .body("isActive", equalTo(false));
    }

    @Test
    void listAll_retornaAtivosEInativos() {
        given()
                .when()
                .get(BASE_PATH + "?page=0&size=10&sort=nome,asc")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].isActive", equalTo(true))
                .body("content[1].isActive", equalTo(false));
    }

    @Test
    void listAll_filtroIsActiveTrue_retornaSomenteAtivos() {
        given()
                .when()
                .get(BASE_PATH + "?page=0&size=10&sort=nome,asc&isActive=true")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].isActive", equalTo(true))
                .body("totalElements", equalTo(1));
    }

    @Test
    void listAll_filtroIsActiveFalse_retornaSomenteInativos() {
        given()
                .when()
                .get(BASE_PATH + "?page=0&size=10&sort=nome,asc&isActive=false")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].isActive", equalTo(false))
                .body("totalElements", equalTo(1));
    }

    @Test
    void delete_cliente_returns204() {
        given()
                .when()
                .delete(BASE_PATH + "/" + CLIENTE_ATIVO)
                .then()
                .statusCode(204);
    }

    @Test
    void reativar_cliente_returns204() {
        given()
                .when()
                .put(BASE_PATH + "/" + CLIENTE_INATIVO + "/ativar")
                .then()
                .statusCode(204);
    }
}