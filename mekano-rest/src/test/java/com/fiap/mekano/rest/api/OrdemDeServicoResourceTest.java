package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdemDeServicoResourceTest {

    private static final String BASE_PATH = "/api/v1/ordens-de-servico";
    private static final UUID OS_UUID = UUID.randomUUID();
    private static final UUID CLIENTE_UUID = UUID.randomUUID();
    private static final UUID VEICULO_UUID = UUID.randomUUID();
    private static final UUID MECANICO_UUID = UUID.randomUUID();

    @InjectMock
    OrdemDeServicoServicePort ordemDeServicoService;

    @BeforeEach
    void setUp() {
        var recebida = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.RECEBIDA, null,
                null, null, null, null, null, null,
                LocalDateTime.now(), 0L);

        var emDiag = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.EM_DIAGNOSTICO, null,
                null, null, null, null, null, null,
                LocalDateTime.now(), 1L);

        var aguardando = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.AGUARDANDO_APROVACAO, null,
                UUID.randomUUID(), null, null, null, null, LocalDateTime.now(),
                LocalDateTime.now(), 2L);

        var emExec = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.EM_EXECUCAO, null,
                UUID.randomUUID(), MECANICO_UUID, LocalDateTime.now(), null,
                "Iniciado", LocalDateTime.now(),
                LocalDateTime.now(), 3L);

        var finalizada = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.FINALIZADA, null,
                UUID.randomUUID(), MECANICO_UUID, LocalDateTime.now(), LocalDateTime.now(),
                "Concluído", LocalDateTime.now(),
                LocalDateTime.now(), 4L);

        var entregue = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.ENTREGUE, null,
                UUID.randomUUID(), MECANICO_UUID, LocalDateTime.now(), LocalDateTime.now(),
                "Concluído", LocalDateTime.now(),
                LocalDateTime.now(), 5L);

        var cancelada = OrdemDeServico.reconstitute(OS_UUID, CLIENTE_UUID, VEICULO_UUID,
                "Barulho no motor", StatusOS.CANCELADA, "Cliente desistiu",
                null, null, null, null, null, null,
                LocalDateTime.now(), 6L);

        Mockito.when(ordemDeServicoService.criar(Mockito.any()))
                .thenReturn(recebida);

        Mockito.when(ordemDeServicoService.buscarPorId(OS_UUID))
                .thenReturn(aguardando);

        Mockito.when(ordemDeServicoService.buscarPorId(Mockito.argThat(id -> !id.equals(OS_UUID))))
                .thenThrow(new AppException(404, "OS não encontrada"));

        Mockito.when(ordemDeServicoService.iniciarDiagnostico(OS_UUID))
                .thenReturn(emDiag);

        Mockito.when(ordemDeServicoService.finalizarDiagnostico(OS_UUID))
                .thenReturn(aguardando);

        Mockito.when(ordemDeServicoService.iniciarExecucao(Mockito.any()))
                .thenReturn(emExec);

        Mockito.when(ordemDeServicoService.finalizarExecucao(Mockito.any()))
                .thenReturn(finalizada);

        Mockito.when(ordemDeServicoService.entregar(OS_UUID))
                .thenReturn(entregue);

        Mockito.when(ordemDeServicoService.cancelar(Mockito.any()))
                .thenReturn(cancelada);

        Mockito.when(ordemDeServicoService.listarComFiltros(Mockito.eq("EM_EXECUCAO"), Mockito.isNull(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(List.of(emExec));

        Mockito.when(ordemDeServicoService.contar())
                .thenReturn(1L);

        Mockito.when(ordemDeServicoService.calcularTempoMedioExecucao())
                .thenReturn(Optional.of(5.5));
    }

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void criar_comDadosValidos_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "Barulho no motor"}
                        """.formatted(CLIENTE_UUID.toString(), VEICULO_UUID.toString()))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("RECEBIDA"))
                .header("Location", notNullValue());
    }

    @Test
    @Order(2)
    @TestSecurity(user = "admin", roles = {"admin"})
    void criar_camposFaltando_returns400() {
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
    void buscarPorId_existente_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/" + OS_UUID)
                .then()
                .statusCode(200)
                .body("id", equalTo(OS_UUID.toString()));
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = {"admin"})
    void buscarPorId_inexistente_returns404() {
        given()
                .when()
                .get(BASE_PATH + "/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listar_comFiltroStatus_returns200() {
        given()
                .queryParam("status", "EM_EXECUCAO")
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content.size()", greaterThan(0))
                .body("content[0].status", equalTo("EM_EXECUCAO"));
    }

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = {"admin"})
    void iniciarDiagnostico_returns200() {
        given()
                .when()
                .post(BASE_PATH + "/" + OS_UUID + "/iniciar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_DIAGNOSTICO"));
    }

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = {"admin"})
    void finalizarDiagnostico_returns200() {
        given()
                .when()
                .post(BASE_PATH + "/" + OS_UUID + "/finalizar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("AGUARDANDO_APROVACAO"));
    }

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = {"admin"})
    void iniciarExecucao_comMecanico_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"mecanicoUuid": "%s", "observacao": "Iniciado"}
                        """.formatted(MECANICO_UUID.toString()))
                .when()
                .put(BASE_PATH + "/" + OS_UUID + "/iniciar-execucao")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_EXECUCAO"))
                .body("mecanicoUuid", notNullValue())
                .body("execucaoIniciadaEm", notNullValue());
    }

    @Test
    @Order(9)
    @TestSecurity(user = "admin", roles = {"admin"})
    void finalizarExecucao_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"observacao": "Concluído"}
                        """)
                .when()
                .put(BASE_PATH + "/" + OS_UUID + "/finalizar")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINALIZADA"));
    }

    @Test
    @Order(10)
    @TestSecurity(user = "admin", roles = {"admin"})
    void entregar_returns200() {
        given()
                .when()
                .post(BASE_PATH + "/" + OS_UUID + "/entregar")
                .then()
                .statusCode(200)
                .body("status", equalTo("ENTREGUE"));
    }

    @Test
    @Order(11)
    @TestSecurity(user = "admin", roles = {"admin"})
    void cancelar_comMotivo_returns200() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"osUuid": "%s", "motivo": "Cliente desistiu"}
                        """.formatted(OS_UUID.toString()))
                .when()
                .post(BASE_PATH + "/" + OS_UUID + "/cancelar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"))
                .body("motivoCancelamento", equalTo("Cliente desistiu"));
    }

    @Test
    @Order(12)
    @TestSecurity(user = "admin", roles = {"admin"})
    void tempoMedioExecucao_returns200() {
        given()
                .when()
                .get(BASE_PATH + "/metricas/tempo-medio")
                .then()
                .statusCode(200)
                .body("mediaHoras", equalTo(5.5f));
    }

    @Test
    @Order(13)
    @TestSecurity(user = "admin", roles = {"admin"})
    void deletar_returns204() {
        given()
                .when()
                .delete(BASE_PATH + "/" + OS_UUID)
                .then()
                .statusCode(204);
    }
}