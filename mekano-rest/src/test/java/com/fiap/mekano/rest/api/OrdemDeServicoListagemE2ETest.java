package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Testes E2E da listagem de OS e consulta pública de status (itens 2 e 3 do feedback).
 *
 * <p>Usa beans REAIS e dados persistidos de verdade para provar a exclusão lógica:
 * OS em status terminal (FINALIZADA/ENTREGUE/CANCELADA) NUNCA aparece no GET /os,
 * o totalElements não as conta e a ordenação default segue a prioridade de status.
 *
 * <p>Fixture segue o padrão de {@link PagamentoResourceTest} / {@link OrcamentoFluxoCriticoE2ETest}.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdemDeServicoListagemE2ETest {

    private static final String OS_PATH = "/api/v1/os";
    private static final String PECA_PATH = "/api/v1/pecas";

    private static String pecaId;
    private static String servicoId;
    private static UUID clienteId;
    private static UUID veiculoId;
    private static String osRecebidaId;
    private static String osEmExecucaoId;
    private static String osFinalizadaId;
    private static String osCanceladaId;

    @Inject
    ClienteRepositoryPort clienteRepository;

    @Inject
    VeiculoRepositoryPort veiculoRepository;

    @Inject
    ServicoRepositoryPort servicoRepository;

    @Inject
    PecaRepositoryPort pecaRepository;

    @Inject
    UserTransaction utx;

    @Test
    @Order(1)
    @TestSecurity(user = "admin", roles = {"admin"})
    void fixture_criarOSesEmDiversosStatus() throws Exception {
        pecaId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "E2E-LIST-001", "descricao": "Peca E2E listagem", \
                         "unidadeMedida": "UNIDADE", "valorUnitario": 120.00}
                        """)
                .when()
                .post(PECA_PATH)
                .then()
                .statusCode(201)
                .extract().path("id");

        pecaRepository.creditarSaldo(UUID.fromString(pecaId), 100);

        utx.begin();
        try {
            Cliente cliente = Cliente.create("Cliente List E2E", "52998224725", "cliente-list@e2e.com",
                    "51999999999", "Rua C", "300", "Centro", "Porto Alegre", "RS", "90010000");
            cliente = clienteRepository.create(cliente);
            clienteId = cliente.getId();

            Veiculo veiculo = Veiculo.create(cliente.getId(), "LST0A00", "VW", "Gol", 2019);
            veiculo = veiculoRepository.create(veiculo);
            veiculoId = veiculo.getId();

            Servico servico = Servico.create("Lavagem E2E", "Lavagem completa E2E", new BigDecimal("50.00"));
            servicoId = servicoRepository.save(servico).getId().toString();
            utx.commit();
        } catch (Exception e) {
            utx.rollback();
            throw e;
        }

        // OS ativa simples (RECEBIDA)
        osRecebidaId = criarOS("E2E listagem recebida");

        // OS ativa em execução (EM_EXECUCAO) — prioridade máxima na listagem default
        osEmExecucaoId = criarOS("E2E listagem em execucao");
        avancarDiagnosticoEAprovar(osEmExecucaoId);
        iniciarExecucao(osEmExecucaoId);

        // OS terminal FINALIZADA — deve desaparecer da listagem
        osFinalizadaId = criarOS("E2E listagem finalizada");
        avancarDiagnosticoEAprovar(osFinalizadaId);
        iniciarExecucao(osFinalizadaId);
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"observacao": "Finalizada para teste de exclusao"}
                        """)
                .when()
                .put(OS_PATH + "/" + osFinalizadaId + "/finalizar-execucao")
                .then()
                .statusCode(200)
                .body("status", equalTo("FINALIZADA"));

        // OS terminal CANCELADA — deve desaparecer da listagem
        osCanceladaId = criarOS("E2E listagem cancelada");
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Cancelada para teste de exclusao"}
                        """)
                .when()
                .put(OS_PATH + "/" + osCanceladaId + "/cancelar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"));
    }

    private String criarOS(String descricaoProblema) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", "descricaoProblema": "%s"}
                        """.formatted(clienteId, veiculoId, descricaoProblema))
                .when()
                .post(OS_PATH)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private void avancarDiagnosticoEAprovar(String osId) {
        given().when().put(OS_PATH + "/" + osId + "/iniciar-diagnostico")
                .then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao": "Diagnostico E2E listagem", \
                         "itens": [{"referenciaUuid": "%s", "tipo": "PECA", "quantidade": 1}, \
                                   {"referenciaUuid": "%s", "tipo": "SERVICO"}]}
                        """.formatted(pecaId, servicoId))
                .when()
                .put(OS_PATH + "/" + osId + "/finalizar-diagnostico")
                .then()
                .statusCode(200);

        String orcamentoId = given()
                .when()
                .get("/api/v1/orcamentos?osUuid=" + osId)
                .then()
                .statusCode(200)
                .extract().path("id");

        given().when().post("/api/v1/orcamentos/" + orcamentoId + "/aprovar")
                .then().statusCode(200);
    }

    private void iniciarExecucao(String osId) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"mecanicoUuid": "%s", "observacao": "E2E listagem"}
                        """.formatted(UUID.randomUUID()))
                .when()
                .put(OS_PATH + "/" + osId + "/iniciar-execucao")
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_EXECUCAO"));
    }

    @Test
    @Order(2)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listAll_naoRetornaStatusTerminais_eTotalConsistente() {
        var content = given()
                .queryParam("page", 0)
                .queryParam("size", 100)
                .when()
                .get(OS_PATH)
                .then()
                .statusCode(200)
                .extract();

        List<String> statuses = content.path("content.status");
        List<String> ids = content.path("content.id");
        long totalElements = ((Number) content.path("totalElements")).longValue();
        int totalPages = content.path("totalPages");

        // exclusão lógica: nenhum status terminal na listagem
        assertThat(statuses)
                .doesNotContain("FINALIZADA", "ENTREGUE", "CANCELADA");

        // OS terminais criadas pela fixture ausentes
        assertThat(ids).doesNotContain(osFinalizadaId, osCanceladaId);
        assertThat(ids).contains(osRecebidaId, osEmExecucaoId);

        // paginação consistente: total = soma dos itens não-terminais, páginas batem
        assertThat(totalElements).isGreaterThanOrEqualTo(statuses.size());
        assertThat(totalPages).isEqualTo((int) Math.ceil((double) totalElements / 100));
    }

    @Test
    @Order(3)
    @TestSecurity(user = "admin", roles = {"admin"})
    void listAll_sortDefault_ordenaPorPrioridadeDeStatus() {
        List<String> ids = given()
                .queryParam("page", 0)
                .queryParam("size", 100)
                .when()
                .get(OS_PATH)
                .then()
                .statusCode(200)
                .extract().path("content.id");

        // EM_EXECUCAO (prioridade 0) vem antes de RECEBIDA (prioridade 3)
        assertThat(ids.indexOf(osEmExecucaoId))
                .isLessThan(ids.indexOf(osRecebidaId));
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = {"admin"})
    void filtro_porCliente_excluiTerminais_eTotalElementsConsistente() {
        var content = given()
                .queryParam("clienteId", clienteId)
                .queryParam("page", 0)
                .queryParam("size", 50)
                .when()
                .get(OS_PATH + "/filtro")
                .then()
                .statusCode(200)
                .extract();

        List<String> statuses = content.path("content.status");
        List<String> ids = content.path("content.id");
        long totalElements = ((Number) content.path("totalElements")).longValue();
        int totalPages = content.path("totalPages");

        assertThat(ids).doesNotContain(osFinalizadaId, osCanceladaId);
        assertThat(ids).contains(osRecebidaId, osEmExecucaoId);
        assertThat(statuses).doesNotContain("FINALIZADA", "ENTREGUE", "CANCELADA");

        // fix da paginação: totalElements reflete a query (não content.size() hardcoded),
        // e totalPages é calculado de verdade
        assertThat(totalElements).isEqualTo(statuses.size());
        assertThat(totalPages).isEqualTo((int) Math.ceil((double) totalElements / 50));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = {"admin"})
    void filtro_paginaUnica_totalPagesCalculadoDeVerdade() {
        long totalElements = ((Number) given()
                .queryParam("clienteId", clienteId)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get(OS_PATH + "/filtro")
                .then()
                .statusCode(200)
                .extract().path("totalElements")).longValue();

        int totalPages = given()
                .queryParam("clienteId", clienteId)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get(OS_PATH + "/filtro")
                .then()
                .statusCode(200)
                .extract().path("totalPages");

        // com size=1 e 2 OS ativas do cliente: total=2, totalPages=2 (antes era sempre 1)
        assertThat(totalElements).isEqualTo(2L);
        assertThat(totalPages).isEqualTo(2);
    }

    @Test
    @Order(6)
    void statusPublico_osAtiva_retorna200SemAutenticacao() {
        given()
                .when()
                .get(OS_PATH + "/" + osRecebidaId + "/status")
                .then()
                .statusCode(200)
                .body("id", equalTo(osRecebidaId))
                .body("status", equalTo("RECEBIDA"));
    }

    @Test
    @Order(7)
    void statusPublico_uuidInexistente_retorna404ProblemJson() {
        given()
                .when()
                .get(OS_PATH + "/00000000-0000-0000-0000-000000000000/status")
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }
}
