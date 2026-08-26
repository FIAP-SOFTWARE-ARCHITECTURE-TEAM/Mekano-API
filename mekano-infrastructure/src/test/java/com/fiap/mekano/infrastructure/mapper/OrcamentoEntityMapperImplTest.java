package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.infrastructure.entity.OrcamentoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrcamentoEntityMapperImpl")
class OrcamentoEntityMapperImplTest {

    private final OrcamentoEntityMapperImpl mapper = new OrcamentoEntityMapperImpl();

    @Test
    @DisplayName("round-trip toEntity→toDomain preserva pecaId")
    void roundTripPreservaPecaId() {
        UUID pecaId = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ItemOrcamento item = new ItemOrcamento("Óleo Motor 5W30", 2L, new BigDecimal("45.50"), pecaId);
        Orcamento orcamento = Orcamento.reconstitute(
                orcamentoUuid, "Diagnóstico", List.of(item),
                new BigDecimal("91.00"), now, StatusOrcamento.PENDENTE, osUuid, now.plusDays(7));

        OrcamentoEntity entity = mapper.toEntity(orcamento);
        Orcamento result = mapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(1, result.getItens().size());
        assertEquals(pecaId, result.getItens().get(0).getPecaId());
        assertEquals("Óleo Motor 5W30", result.getItens().get(0).getDescricao());
        assertEquals(2L, result.getItens().get(0).getQuantidade());
        assertEquals(new BigDecimal("45.50"), result.getItens().get(0).getValorUnitario());
    }

    @Test
    @DisplayName("round-trip com item SERVICO (pecaId null)")
    void roundTripComServicoPecaIdNull() {
        UUID osUuid = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ItemOrcamento item = new ItemOrcamento("Troca de Óleo", 1L, new BigDecimal("89.90"));
        Orcamento orcamento = Orcamento.reconstitute(
                orcamentoUuid, "Diagnóstico", List.of(item),
                new BigDecimal("89.90"), now, StatusOrcamento.PENDENTE, osUuid, now.plusDays(7));

        OrcamentoEntity entity = mapper.toEntity(orcamento);
        Orcamento result = mapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(1, result.getItens().size());
        assertNull(result.getItens().get(0).getPecaId());
    }

    @Test
    @DisplayName("legacy 3-field JSON desserializa com pecaId null")
    void legacy3FieldDesserializaComPecaIdNull() {
        // Simula dados legados no banco (antes de pecaId)
        OrcamentoEntity entity = new OrcamentoEntity();
        entity.setUuid(UUID.randomUUID());
        entity.setDescricao("Diagnóstico");
        entity.setItensJson("Óleo Motor 5W30|2|45.50");
        entity.setValorTotal(new BigDecimal("91.00"));
        entity.setStatus(StatusOrcamento.PENDENTE.name());
        entity.setOrdemServicoUuid(UUID.randomUUID());
        entity.setDataExpiracao(LocalDateTime.now().plusDays(7));
        entity.setCreatedAt(LocalDateTime.now());

        Orcamento result = mapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(1, result.getItens().size());
        assertNull(result.getItens().get(0).getPecaId());
        assertEquals("Óleo Motor 5W30", result.getItens().get(0).getDescricao());
        assertEquals(2L, result.getItens().get(0).getQuantidade());
        assertEquals(new BigDecimal("45.50"), result.getItens().get(0).getValorUnitario());
    }

    @Test
    @DisplayName("round-trip com múltiplos itens pecaId e servico")
    void roundTripMultiplosItens() {
        UUID pecaId = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ItemOrcamento item1 = new ItemOrcamento("Óleo Motor 5W30", 2L, new BigDecimal("45.50"), pecaId);
        ItemOrcamento item2 = new ItemOrcamento("Mão de Obra", 1L, new BigDecimal("120.00"));
        Orcamento orcamento = Orcamento.reconstitute(
                orcamentoUuid, "Diagnóstico", List.of(item1, item2),
                new BigDecimal("211.00"), now, StatusOrcamento.PENDENTE, osUuid, now.plusDays(7));

        OrcamentoEntity entity = mapper.toEntity(orcamento);
        Orcamento result = mapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(2, result.getItens().size());
        assertEquals(pecaId, result.getItens().get(0).getPecaId());
        assertNull(result.getItens().get(1).getPecaId());
    }
}