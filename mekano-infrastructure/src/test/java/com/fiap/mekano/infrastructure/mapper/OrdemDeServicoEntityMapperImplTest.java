package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.os.StatusEntrega;
import com.fiap.mekano.domain.os.StatusPagamento;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrdemDeServicoEntityMapperImpl")
class OrdemDeServicoEntityMapperImplTest {

    private final OrdemDeServicoEntityMapperImpl mapper = new OrdemDeServicoEntityMapperImpl();

    @Test
    @DisplayName("deve mapear domínio para entity incluindo pagamento e entrega")
    void deveMapearDomainParaEntityComNovosCampos() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        UUID mecanicoUuid = UUID.randomUUID();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        LocalDateTime dataAprovacao = LocalDateTime.now().minusDays(4);
        LocalDateTime execucaoIniciadaEm = LocalDateTime.now().minusDays(3);
        LocalDateTime execucaoFinalizadaEm = LocalDateTime.now().minusDays(2);
        LocalDateTime cobrancaGeradaEm = LocalDateTime.now().minusDays(2);
        LocalDateTime pagamentoConfirmadoEm = LocalDateTime.now().minusDays(1);
        LocalDateTime entregueEm = LocalDateTime.now();

        OrdemDeServico domain = OrdemDeServico.reconstitute(
                id,
                clienteId,
                veiculoId,
                "Motor falhando",
                StatusOS.ENTREGUE,
                null,
                orcamentoUuid,
                mecanicoUuid,
                execucaoIniciadaEm,
                execucaoFinalizadaEm,
                "Executado com sucesso",
                dataAprovacao,
                createdAt,
                7L,
                StatusPagamento.CONFIRMADO,
                StatusEntrega.ENTREGUE,
                cobrancaGeradaEm,
                pagamentoConfirmadoEm,
                "PIX-123",
                entregueEm,
                "João Cliente"
        );

        OrdemDeServicoEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getUuid());
        assertEquals(clienteId, entity.getClienteUuid());
        assertEquals(veiculoId, entity.getVeiculoUuid());
        assertEquals("Motor falhando", entity.getDescricaoProblema());
        assertEquals(StatusOS.ENTREGUE.name(), entity.getStatus());
        assertEquals(orcamentoUuid, entity.getOrcamentoUuid());
        assertEquals(mecanicoUuid, entity.getMecanicoUuid());
        assertEquals(execucaoIniciadaEm, entity.getExecucaoIniciadaEm());
        assertEquals(execucaoFinalizadaEm, entity.getExecucaoFinalizadaEm());
        assertEquals("Executado com sucesso", entity.getObservacaoExecucao());
        assertEquals(dataAprovacao, entity.getDataAprovacao());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(7L, entity.getVersion());

        assertEquals(StatusPagamento.CONFIRMADO.name(), entity.getStatusPagamento());
        assertEquals(StatusEntrega.ENTREGUE.name(), entity.getStatusEntrega());
        assertEquals(cobrancaGeradaEm, entity.getCobrancaGeradaEm());
        assertEquals(pagamentoConfirmadoEm, entity.getPagamentoConfirmadoEm());
        assertEquals("PIX-123", entity.getReferenciaPagamento());
        assertEquals(entregueEm, entity.getEntregueEm());
        assertEquals("João Cliente", entity.getRecebidoPor());
    }

    @Test
    @DisplayName("deve mapear entity para domínio incluindo pagamento e entrega")
    void deveMapearEntityParaDomainComNovosCampos() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        UUID mecanicoUuid = UUID.randomUUID();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        LocalDateTime dataAprovacao = LocalDateTime.now().minusDays(4);
        LocalDateTime execucaoIniciadaEm = LocalDateTime.now().minusDays(3);
        LocalDateTime execucaoFinalizadaEm = LocalDateTime.now().minusDays(2);
        LocalDateTime cobrancaGeradaEm = LocalDateTime.now().minusDays(2);
        LocalDateTime pagamentoConfirmadoEm = LocalDateTime.now().minusDays(1);
        LocalDateTime entregueEm = LocalDateTime.now();

        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();
        entity.setUuid(id);
        entity.setClienteUuid(clienteId);
        entity.setVeiculoUuid(veiculoId);
        entity.setDescricaoProblema("Motor falhando");
        entity.setStatus(StatusOS.ENTREGUE.name());
        entity.setMotivoCancelamento(null);
        entity.setOrcamentoUuid(orcamentoUuid);
        entity.setMecanicoUuid(mecanicoUuid);
        entity.setExecucaoIniciadaEm(execucaoIniciadaEm);
        entity.setExecucaoFinalizadaEm(execucaoFinalizadaEm);
        entity.setObservacaoExecucao("Executado com sucesso");
        entity.setDataAprovacao(dataAprovacao);
        entity.setCreatedAt(createdAt);
        entity.setVersion(7L);

        entity.setStatusPagamento(StatusPagamento.CONFIRMADO.name());
        entity.setStatusEntrega(StatusEntrega.ENTREGUE.name());
        entity.setCobrancaGeradaEm(cobrancaGeradaEm);
        entity.setPagamentoConfirmadoEm(pagamentoConfirmadoEm);
        entity.setReferenciaPagamento("PIX-123");
        entity.setEntregueEm(entregueEm);
        entity.setRecebidoPor("João Cliente");

        OrdemDeServico domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(clienteId, domain.getClienteId());
        assertEquals(veiculoId, domain.getVeiculoId());
        assertEquals("Motor falhando", domain.getDescricaoProblema());
        assertEquals(StatusOS.ENTREGUE, domain.getStatus());
        assertEquals(orcamentoUuid, domain.getOrcamentoUuid());
        assertEquals(mecanicoUuid, domain.getMecanicoUuid());
        assertEquals(execucaoIniciadaEm, domain.getExecucaoIniciadaEm());
        assertEquals(execucaoFinalizadaEm, domain.getExecucaoFinalizadaEm());
        assertEquals("Executado com sucesso", domain.getObservacaoExecucao());
        assertEquals(dataAprovacao, domain.getDataAprovacao());
        assertEquals(createdAt, domain.getCreatedAt());
        assertEquals(7L, domain.getVersion());

        assertEquals(StatusPagamento.CONFIRMADO, domain.getStatusPagamento());
        assertEquals(StatusEntrega.ENTREGUE, domain.getStatusEntrega());
        assertEquals(cobrancaGeradaEm, domain.getCobrancaGeradaEm());
        assertEquals(pagamentoConfirmadoEm, domain.getPagamentoConfirmadoEm());
        assertEquals("PIX-123", domain.getReferenciaPagamento());
        assertEquals(entregueEm, domain.getEntregueEm());
        assertEquals("João Cliente", domain.getRecebidoPor());
    }

    @Test
    @DisplayName("deve retornar null quando domínio for null")
    void deveRetornarNullQuandoDomainForNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("deve retornar null quando entity for null")
    void deveRetornarNullQuandoEntityForNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    @DisplayName("deve aplicar defaults quando statusPagamento e statusEntrega vierem null da entity")
    void deveAplicarDefaultsQuandoNovosStatusVieremNull() {
        OrdemDeServicoEntity entity = new OrdemDeServicoEntity();

        entity.setUuid(UUID.randomUUID());
        entity.setClienteUuid(UUID.randomUUID());
        entity.setVeiculoUuid(UUID.randomUUID());
        entity.setDescricaoProblema("Motor falhando");
        entity.setStatus(StatusOS.RECEBIDA.name());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVersion(0L);

        entity.setStatusPagamento(null);
        entity.setStatusEntrega(null);

        OrdemDeServico domain = mapper.toDomain(entity);

        assertEquals(StatusPagamento.NAO_COBRADO, domain.getStatusPagamento());
        assertEquals(StatusEntrega.NAO_LIBERADA, domain.getStatusEntrega());
    }
}