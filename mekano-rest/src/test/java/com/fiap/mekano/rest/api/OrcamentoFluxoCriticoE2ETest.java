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
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Testes E2E dos fluxos críticos de orçamento (item 3 do feedback da avaliação).
 *
 * <p>Diferente de {@link OrcamentoResourceTest} (que mocka o serviço), esta classe
 * usa beans REAIS (service + repositórios) e valida a idempotência de ponta a ponta:
 * aprovação/reprovação em replay retorna 200 sem duplicar efeitos; conflito de
 * estado retorna 422 problem+json.
 *
 * <p>Fixture segue o padrão de {@link PagamentoResourceTest}: peça via REST,
 * cliente/veículo/serviço via repositório real com UserTransaction explícita,
 * OS via REST e orçamento gerado automaticamente no finalizar-diagnostico.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrcamentoFluxoCriticoE2ETest {

    private static final String OS_PATH = "/api/v1/os";
    private static final String PECA_PATH = "/api/v1/pecas";
    private static final String ORCAMENTO_PATH = "/api/v1/orcamentos";

    private static String pecaId;
    private static String servicoId;
    private static String osAprovacaoId;
    private static String orcamentoAprovacaoId;
    private static String osReprovacaoId;
    private static String orcamentoReprovacaoId;

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
    void fixture_criarOsWithOrcamentosPendentes() throws Exception {
        pecaId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"codigo": "E2E-ORC-001", "descricao": "Peca E2E orcamento", \
                         "unidadeMedida": "UNIDADE", "valorUnitario": 150.00}
                        """)
                .when()
                .post(PECA_PATH)
                .then()
                .statusCode(201)
                .extract().path("id");

        pecaRepository.creditarSaldo(UUID.fromString(pecaId), 100);

        UUID clienteId;
        UUID veiculoId;
        utx.begin();
        try {
            Cliente cliente = Cliente.create("Cliente Orc E2E", "52998224725", "cliente-orc@e2e.com",
                    "51999999999", "Rua B", "200", "Centro", "Porto Alegre", "RS", "90010000");
            cliente = clienteRepository.create(cliente);
            clienteId = cliente.getId();

            Veiculo veiculo = Veiculo.create(cliente.getId(), "ORC0A00", "Fiat", "Mobi", 2021);
            veiculo = veiculoRepository.create(veiculo);
            veiculoId = veiculo.getId();

            Servico servico = Servico.create("Revisao E2E", "Revisao completa E2E", new BigDecimal("99.90"));
            servicoId = servicoRepository.save(servico).getId().toString();
            utx.commit();
        } catch (Exception e) {
            utx.rollback();
            throw e;
        }

        osAprovacaoId = criarOS(clienteId, veiculoId, "E2E fluxo aprovacao");
        orcamentoAprovacaoId = diagnosticoEExtrairOrcamento(osAprovacaoId);

        osReprovacaoId = criarOS(clienteId, veiculoId, "E2E fluxo reprovacao");
        orcamentoReprovacaoId = diagnosticoEExtrairOrcamento(osReprovacaoId);
    }

    private String criarOS(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"clienteId": "%s", "veiculoId": "%s", \
                         "descricaoProblema": "%s"}
                        """.formatted(clienteId, veiculoId, descricaoProblema))
                .when()
                .post(OS_PATH)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    /** Diagnóstico completo via REST — orçamento é gerado automaticamente (pendente de aprovação). */
    private String diagnosticoEExtrairOrcamento(String osId) {
        given()
                .when()
                .put(OS_PATH + "/" + osId + "/iniciar-diagnostico")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"descricao": "Diagnostico E2E orcamento", \
                         "itens": [{"referenciaUuid": "%s", "tipo": "PECA", "quantidade": 1}, \
                                   {"referenciaUuid": "%s", "tipo": "SERVICO"}]}
                        """.formatted(pecaId, servicoId))
                .when()
                .put(OS_PATH + "/" + osId + "/finalizar-diagnostico")
                .then()
                .statusCode(200)
                .body("status", equalTo("AGUARDANDO_APROVACAO"));

        return given()
                .when()
                .get(ORCAMENTO_PATH + "?osUuid=" + osId)
                .then()
                .statusCode(200)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void aprovar_replayIdempotente_retorna200DuasVezes() {
        // primeira aprovação
        given()
                .when()
                .post(ORCAMENTO_PATH + "/" + orcamentoAprovacaoId + "/aprovar")
                .then()
                .statusCode(200)
                .body("status", equalTo("APROVADO"));

        // replay (retry do cliente/webhook) — idempotente, sem 422 e sem duplicar efeitos
        given()
                .when()
                .post(ORCAMENTO_PATH + "/" + orcamentoAprovacaoId + "/aprovar")
                .then()
                .statusCode(200)
                .body("id", equalTo(orcamentoAprovacaoId))
                .body("status", equalTo("APROVADO"));
    }

    @Test
    @Order(3)
    void aprovar_aposReplay_osAvancaUmaUnicaVez() {
        // OS associada reflete a transição única: AGUARDANDO_APROVACAO → AGUARDANDO_EXECUCAO
        given()
                .when()
                .get(OS_PATH + "/" + osAprovacaoId + "/status")
                .then()
                .statusCode(200)
                .body("id", equalTo(osAprovacaoId))
                .body("status", equalTo("AGUARDANDO_EXECUCAO"));
    }

    @Test
    @Order(4)
    void reprovar_aposAprovacao_conflitoRetorna422ProblemJson() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Tentativa de reprovar orçamento já aprovado"}
                        """)
                .when()
                .post(ORCAMENTO_PATH + "/" + orcamentoAprovacaoId + "/reprovar")
                .then()
                .statusCode(422)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(5)
    void reprovar_replayIdempotente_retorna200DuasVezes() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Cliente desistiu do servico"}
                        """)
                .when()
                .post(ORCAMENTO_PATH + "/" + orcamentoReprovacaoId + "/reprovar")
                .then()
                .statusCode(200)
                .body("status", equalTo("REPROVADO"));

        // replay com o mesmo motivo — idempotente, sem 422
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"motivo": "Cliente desistiu do servico"}
                        """)
                .when()
                .post(ORCAMENTO_PATH + "/" + orcamentoReprovacaoId + "/reprovar")
                .then()
                .statusCode(200)
                .body("id", equalTo(orcamentoReprovacaoId))
                .body("status", equalTo("REPROVADO"));
    }

    @Test
    @Order(6)
    void aprovar_aposReprovacao_conflitoRetorna422ProblemJson() {
        given()
                .when()
                .post(ORCAMENTO_PATH + "/" + orcamentoReprovacaoId + "/aprovar")
                .then()
                .statusCode(422)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(7)
    void aprovar_orcamentoInexistente_retorna404ProblemJson() {
        given()
                .when()
                .post(ORCAMENTO_PATH + "/00000000-0000-0000-0000-000000000000/aprovar")
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }
}
