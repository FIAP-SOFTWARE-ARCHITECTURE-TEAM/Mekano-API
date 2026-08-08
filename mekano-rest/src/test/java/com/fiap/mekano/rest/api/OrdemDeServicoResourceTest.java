package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdemDeServicoResourceTest {

    private static final String BASE_PATH = "/api/v1/os";
    private static String createdUuid;
    private static final UUID CLIENTE_UUID = UUID.randomUUID();
    private static final UUID VEICULO_UUID = UUID.randomUUID();

    @InjectMock
    ClienteRepositoryPort clienteRepository;

    @InjectMock
    VeiculoRepositoryPort veiculoRepository;

    @BeforeEach
    void setup() {
        var fakeCliente = Cliente.reconstitute(
                CLIENTE_UUID, "Cliente Teste", "52998224725",
                "cliente@teste.com", null,
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000",
                LocalDateTime.now());
        var fakeVeiculo = Veiculo.reconstitute(
                VEICULO_UUID, CLIENTE_UUID, "ABC1234", "Toyota", "Corolla", 2020, LocalDateTime.now());

        when(clienteRepository.findById(CLIENTE_UUID)).thenReturn(Optional.of(fakeCliente));
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(fakeCliente));
        when(veiculoRepository.findById(VEICULO_UUID)).thenReturn(Optional.of(fakeVeiculo));
        when(veiculoRepository.findById(any(UUID.class))).thenReturn(Optional.of(fakeVeiculo));
    }

    // ─────────────── CREATE ───────────────

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void create_asAdmin_returns201() {
        createdUuid = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "clienteId": "%s",
                          "veiculoId": "%s",
                          "descricaoProblema": "Motor falhando ao acelerar"
                        }
                        """.formatted(CLIENTE_UUID, VEICULO_UUID))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("RECEBIDA"))
                .body("descricaoProblema", equalTo("Motor falhando ao acelerar"))
                .header("Location", notNullValue())
                .extract().path("id");
    }

    // ─────────────── UPDATE ───────────────

    @Test
    @Order(20)
    @TestSecurity(user = "admin", roles = {"admin"})
    void update_emRecebida_returns200() {
        UUID novoCliente = UUID.randomUUID();
        UUID novoVeiculo = UUID.randomUUID();

        String osId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Problema original"}
                        """.formatted(CLIENTE_UUID, VEICULO_UUID))
                .when()
                .post(BASE_PATH)
                .then().statusCode(201).extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Problema corrigido"}
                        """.formatted(novoCliente, novoVeiculo))
                .when()
                .put(BASE_PATH + "/" + osId)
                .then()
                .statusCode(200)
                .body("clienteId", equalTo(novoCliente.toString()))
                .body("veiculoId", equalTo(novoVeiculo.toString()))
                .body("descricaoProblema", equalTo("Problema corrigido"))
                .body("status", equalTo("RECEBIDA"));

        given()
                .when()
                .get(BASE_PATH + "/" + osId)
                .then()
                .statusCode(200)
                .body("descricaoProblema", equalTo("Problema corrigido"));
    }

    @Test
    @Order(21)
    @TestSecurity(user = "admin", roles = {"admin"})
    void update_foraDeRecebida_returns422() {
        String osId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Problema"}
                        """.formatted(CLIENTE_UUID, VEICULO_UUID))
                .when()
                .post(BASE_PATH)
                .then().statusCode(201).extract().path("id");

        given().put(BASE_PATH + "/" + osId + "/iniciar-diagnostico").then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Novo problema"}
                        """.formatted(CLIENTE_UUID, VEICULO_UUID))
                .when()
                .put(BASE_PATH + "/" + osId)
                .then()
                .statusCode(422);
    }

    @Test
    @Order(22)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void update_asMecanico_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "teste"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .put(BASE_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    // ─────────────── INICIAR DIAGNOSTICO ───────────────

    @Test
    @Order(2)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void iniciarDiagnostico_asMecanico_returns200() {
        given()
                .when()
                .put(BASE_PATH + "/" + createdUuid + "/iniciar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_DIAGNOSTICO"));
    }

    // ─────────────── GET STATUS ───────────────

    @Test
    @Order(3)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getStatus_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid + "/status")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("status", equalTo("EM_DIAGNOSTICO"))
                .body("dataEntrada", notNullValue());
    }

    @Test
    @Order(4)
    void getStatus_anonimo_retorna200() {
        given()
                .when()
                .get(BASE_PATH + "/" + createdUuid + "/status")
                .then()
                .statusCode(200)
                .body("id", equalTo(createdUuid))
                .body("status", equalTo("EM_DIAGNOSTICO"))
                .body("dataEntrada", notNullValue());
    }

    // ─────────────── AUTORIZAÇÃO ───────────────

    @Test
    @Order(8)
    @TestSecurity(user = "mecanico", roles = {"mecanico"})
    void create_asMecanico_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "teste"}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(403);
    }

    @Test
    @Order(9)
    void listAll_withoutAuth_returns401() {
        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(401);
    }

    // ─────────────── FILTRO ───────────────

    @Test
    @Order(10)
    @TestSecurity(user = "admin", roles = {"admin"})
    void findAllWithFilters_porStatus_retornaFiltradas() {
        given()
                .when()
                .queryParam("status", "EM_DIAGNOSTICO")
                .get(BASE_PATH + "/filtro")
                .then()
                .statusCode(200)
                .body("content", notNullValue())
                .body("page", equalTo(0))
                .body("size", equalTo(10));
    }

    @Test
    @Order(11)
    @TestSecurity(user = "admin", roles = {"admin"})
    void findAllWithFilters_semResultados_retornaListaVazia() {
        given()
                .when()
                .queryParam("status", "FINALIZADA")
                .get(BASE_PATH + "/filtro")
                .then()
                .statusCode(200)
                .body("content", hasSize(0));
    }

    @Test
    @Order(12)
    @TestSecurity(user = "admin", roles = {"admin"})
    void getTempoMedio_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/tempo-medio")
                .then()
                .statusCode(200)
                .body("breakdownPorMecanico", notNullValue());
    }
}