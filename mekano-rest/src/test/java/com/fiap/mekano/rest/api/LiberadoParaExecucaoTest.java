package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.ItemOs;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.port.out.ItemOsRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@DisplayName("GET /os — liberadoParaExecucao")
class LiberadoParaExecucaoTest {

    private static final String BASE_PATH = "/api/v1/os";

    @InjectMock
    OrdemDeServicoServicePort osService;

    @InjectMock
    ItemOsRepositoryPort itemOsRepository;

    @InjectMock
    OrcamentoRepositoryPort orcamentoRepository;

    @InjectMock
    PecaRepositoryPort pecaRepository;

    private UUID osUuid;
    private UUID orcamentoUuid;
    private UUID pecaUuid;

    @BeforeEach
    void setup() {
        osUuid = UUID.randomUUID();
        orcamentoUuid = UUID.randomUUID();
        pecaUuid = UUID.randomUUID();
    }

    private OrdemDeServico buildOs(StatusOS status, UUID orcamentoUuid) {
        return OrdemDeServico.reconstitute(
                osUuid, UUID.randomUUID(), UUID.randomUUID(), "Problema teste",
                status, null, orcamentoUuid, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                LocalDateTime.now(), null);
    }

    private Orcamento buildOrcamentoAprovado() {
        var itens = List.of(new ItemOrcamento("Troca de óleo", 1L, new BigDecimal("150.00")));
        var orcamento = Orcamento.reconstitute(orcamentoUuid, "Diagnóstico",
                itens, new BigDecimal("150.00"), LocalDateTime.now());
        orcamento.aprovar();
        return orcamento;
    }

    private Orcamento buildOrcamentoPendente() {
        var itens = List.of(new ItemOrcamento("Troca de óleo", 1L, new BigDecimal("150.00")));
        return Orcamento.reconstitute(orcamentoUuid, "Diagnóstico",
                itens, new BigDecimal("150.00"), LocalDateTime.now());
    }

    private ItemOs buildItemPeca(UUID pecaUuid, long qtd) {
        return ItemOs.create(osUuid, pecaUuid, "PECA", "Óleo 5W30", qtd);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @DisplayName("true quando orçamento aprovado e todas peças com saldo suficiente")
    void liberado_true_quandoAprovadoComEstoque() {
        OrdemDeServico os = buildOs(StatusOS.AGUARDANDO_EXECUCAO, orcamentoUuid);
        when(osService.findAll(anyInt(), anyInt(), anyString())).thenReturn(List.of(os));
        when(osService.countAll()).thenReturn(1L);

        when(itemOsRepository.findByOsUuid(osUuid)).thenReturn(List.of(buildItemPeca(pecaUuid, 5)));

        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(buildOrcamentoAprovado()));

        var peca = Peca.reconstitute(pecaUuid, "PEA-001", "Óleo 5W30",
                new BigDecimal("45.90"), 50L, 0L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaUuid)).thenReturn(Optional.of(peca));

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content[0].liberadoParaExecucao", equalTo(true));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @DisplayName("false quando orçamento aprovado mas peça com saldo insuficiente")
    void liberado_false_quandoAprovadoSemEstoque() {
        OrdemDeServico os = buildOs(StatusOS.AGUARDANDO_EXECUCAO, orcamentoUuid);
        when(osService.findAll(anyInt(), anyInt(), anyString())).thenReturn(List.of(os));
        when(osService.countAll()).thenReturn(1L);

        when(itemOsRepository.findByOsUuid(osUuid)).thenReturn(List.of(buildItemPeca(pecaUuid, 20)));

        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(buildOrcamentoAprovado()));

        var peca = Peca.reconstitute(pecaUuid, "PEA-001", "Óleo 5W30",
                new BigDecimal("45.90"), 10L, 0L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaUuid)).thenReturn(Optional.of(peca));

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content[0].liberadoParaExecucao", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @DisplayName("false quando orçamento não aprovado (PENDENTE)")
    void liberado_false_quandoOrcamentoPendente() {
        OrdemDeServico os = buildOs(StatusOS.AGUARDANDO_APROVACAO, orcamentoUuid);
        when(osService.findAll(anyInt(), anyInt(), anyString())).thenReturn(List.of(os));
        when(osService.countAll()).thenReturn(1L);

        when(itemOsRepository.findByOsUuid(osUuid)).thenReturn(List.of(buildItemPeca(pecaUuid, 5)));

        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(buildOrcamentoPendente()));

        var peca = Peca.reconstitute(pecaUuid, "PEA-001", "Óleo 5W30",
                new BigDecimal("45.90"), 50L, 0L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaUuid)).thenReturn(Optional.of(peca));

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content[0].liberadoParaExecucao", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @DisplayName("false quando OS não possui orçamento")
    void liberado_false_quandoSemOrcamento() {
        OrdemDeServico os = buildOs(StatusOS.RECEBIDA, null);
        when(osService.findAll(anyInt(), anyInt(), anyString())).thenReturn(List.of(os));
        when(osService.countAll()).thenReturn(1L);

        when(itemOsRepository.findByOsUuid(osUuid)).thenReturn(List.of());

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content[0].liberadoParaExecucao", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    @DisplayName("true quando orçamento aprovado e OS sem peças (somente serviços)")
    void liberado_true_quandoSemPecasApenasServicos() {
        OrdemDeServico os = buildOs(StatusOS.AGUARDANDO_EXECUCAO, orcamentoUuid);
        when(osService.findAll(anyInt(), anyInt(), anyString())).thenReturn(List.of(os));
        when(osService.countAll()).thenReturn(1L);

        var itemServico = ItemOs.create(osUuid, UUID.randomUUID(), "SERVICO", "Troca de óleo", 1L);
        when(itemOsRepository.findByOsUuid(osUuid)).thenReturn(List.of(itemServico));

        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(buildOrcamentoAprovado()));

        given()
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content[0].liberadoParaExecucao", equalTo(true));
    }
}
