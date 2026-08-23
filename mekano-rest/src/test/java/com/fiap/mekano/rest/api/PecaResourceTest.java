package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.peca.CreatePecaResponse;
import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.UpdatePecaCommand;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PecaResourceTest {

    private static final String BASE_PATH = "/api/v1/pecas";
    private static final UUID PECA_UUID = UUID.randomUUID();
    private static final UUID OTHER_UUID = UUID.randomUUID();

    private static Peca mockPeca;

    @InjectMock
    PecaService pecaService;

    @BeforeEach
    void setUp() {
        mockPeca = Peca.reconstitute(
                PECA_UUID, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"),
                50L, 10L, LocalDateTime.now(), 0L);

        Mockito.when(pecaService.criar(Mockito.any()))
                .thenReturn(new CreatePecaResponse(PECA_UUID, "PEA-001", "Óleo do Motor 5W30",
                        new BigDecimal("45.90"), 50L, 10L, LocalDateTime.now()));

        Mockito.when(pecaService.buscarPorId(PECA_UUID))
                .thenReturn(mockPeca);

        Mockito.when(pecaService.buscarPorId(Mockito.argThat(id -> !id.equals(PECA_UUID))))
                .thenThrow(new AppException(404, "Peça não encontrada"));

        Mockito.when(pecaService.updatePeca(eq(PECA_UUID), any(UpdatePecaCommand.class)))
                .thenAnswer(invocation -> {
                    UpdatePecaCommand cmd = invocation.getArgument(1);
                    return Peca.reconstitute(
                            PECA_UUID, cmd.codigo(), cmd.descricao(), cmd.valorUnitario(),
                            50L, cmd.estoqueMinimo(), mockPeca.getCreatedAt(), 0L);
                });

        Mockito.doNothing().when(pecaService).excluir(PECA_UUID);

        Mockito.doThrow(new AppException(409, "Peça vinculada a OS ativa"))
                .when(pecaService).excluir(OTHER_UUID);

        Mockito.doNothing().when(pecaService).reativar(PECA_UUID);

        Peca inativa = Peca.reconstitute(
                OTHER_UUID, "PEA-999", "Peça Inativa",
                new BigDecimal("10.00"),
                1L, 1L, LocalDateTime.now(), 0L, false);

        Mockito.when(pecaService.findAll(0, 10, null)).thenReturn(java.util.List.of(mockPeca, inativa));
        Mockito.when(pecaService.countAll(null)).thenReturn(2L);
        Mockito.when(pecaService.findAll(0, 10, true)).thenReturn(java.util.List.of(mockPeca));
        Mockito.when(pecaService.countAll(true)).thenReturn(1L);
        Mockito.when(pecaService.findAll(0, 10, false)).thenReturn(java.util.List.of(inativa));
        Mockito.when(pecaService.countAll(false)).thenReturn(1L);
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_validPeca_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "PEA-001", "descricao": "Óleo do Motor 5W30", "valorUnitario": 45.90, "estoqueMinimo": 10}
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
                .body("saldoAtual", equalTo(50))
                .body("isActive", equalTo(true));
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
                        {"codigo": "PEA-002", "descricao": "Teste", "valorUnitario": 10.00}
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

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = {"admin"})
    void put_existing_updatesFieldsAndReturns200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "PEA-002", "descricao": "Filtro de Óleo", "valorUnitario": 29.90, "estoqueMinimo": 5}
                        """)
                .when()
                .put(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(200)
                .body("codigo", equalTo("PEA-002"))
                .body("descricao", equalTo("Filtro de Óleo"))
                .body("valorUnitario", equalTo(29.90f))
                .body("estoqueMinimo", equalTo(5));
    }

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = {"admin"})
    void put_preservesSaldoAtual() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "PEA-003", "descricao": "Pastilha de Freio", "valorUnitario": 89.90, "estoqueMinimo": 3}
                        """)
                .when()
                .put(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(200)
                .body("saldoAtual", equalTo(50));
    }

    @Test
    @Order(9)
    @TestSecurity(user = "atendente", roles = {"user"})
    void put_asAtendente_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "PEA-004", "descricao": "Teste", "valorUnitario": 10.00}
                        """)
                .when()
                .put(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(10)
    @TestSecurity(user = "admin", roles = {"admin"})
    void delete_existing_returns204() {
        given()
                .when()
                .delete(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(11)
    @TestSecurity(user = "admin", roles = {"admin"})
    void delete_linkedToActiveOS_returns409() {
        given()
                .when()
                .delete(BASE_PATH + "/" + OTHER_UUID)
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(12)
    @TestSecurity(user = "atendente", roles = {"user"})
    void delete_asAtendente_returns403() {
        given()
                .when()
                .delete(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(13)
    @TestSecurity(user = "admin", roles = {"admin"})
    void reativar_existing_returns204() {
        given()
                .when()
                .put(BASE_PATH + "/" + PECA_UUID + "/ativar")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(14)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listAll_filtroIsActive_returnsOnlyRequested() {
        given()
                .when()
                .get(BASE_PATH + "?isActive=false")
                .then()
                .statusCode(200)
                .body("content.descricao", hasItem("Peça Inativa"))
                .body("content.descricao", not(hasItem("Óleo do Motor 5W30")))
                .body("totalElements", equalTo(1));

        given()
                .when()
                .get(BASE_PATH + "?isActive=true")
                .then()
                .statusCode(200)
                .body("content.descricao", hasItem("Óleo do Motor 5W30"))
                .body("content.descricao", not(hasItem("Peça Inativa")))
                .body("totalElements", equalTo(1));

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("totalElements", equalTo(2));
    }
}
