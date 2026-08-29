package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.infrastructure.entity.OrcamentoEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class OrcamentoEntityMapperImpl implements OrcamentoEntityMapper {

    private static final Pattern FIELD_PATTERN = Pattern.compile("\\|");
    private static final Pattern ITEM_PATTERN = Pattern.compile(";");

    @Override
    public OrcamentoEntity toEntity(Orcamento orcamento) {
        if (orcamento == null) {
            return null;
        }
        OrcamentoEntity entity = new OrcamentoEntity();
        entity.setUuid(orcamento.getId());
        entity.setDescricao(orcamento.getDescricao());
        entity.setStatus(orcamento.getStatus().name());
        entity.setValorTotal(orcamento.getValorTotal());
        entity.setOrdemServicoUuid(orcamento.getOrdemServicoUuid());
        entity.setDataExpiracao(orcamento.getDataExpiracao());
        entity.setItensJson(serializarItens(orcamento.getItens()));
        entity.setCreatedAt(orcamento.getCreatedAt());
        return entity;
    }

    @Override
    public Orcamento toDomain(OrcamentoEntity entity) {
        if (entity == null) {
            return null;
        }
        return Orcamento.reconstitute(
                entity.getUuid(),
                entity.getDescricao(),
                desserializarItens(entity.getItensJson()),
                entity.getValorTotal(),
                entity.getCreatedAt(),
                StatusOrcamento.valueOf(entity.getStatus()),
                entity.getOrdemServicoUuid(),
                entity.getDataExpiracao()
        );
    }

    private String serializarItens(List<ItemOrcamento> itens) {
        if (itens == null || itens.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ItemOrcamento item : itens) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(escape(item.getDescricao()))
                    .append('|')
                    .append(item.getQuantidade())
                    .append('|')
                    .append(item.getValorUnitario())
                    .append('|')
                    .append(item.getPecaId() != null ? item.getPecaId() : "");
        }
        return sb.toString();
    }

    private List<ItemOrcamento> desserializarItens(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<ItemOrcamento> itens = new ArrayList<>();
        for (String part : ITEM_PATTERN.split(json)) {
            String[] fields = FIELD_PATTERN.split(part, 4);
            if (fields.length >= 3) {
                UUID pecaId = null;
                if (fields.length == 4 && fields[3] != null && !fields[3].isBlank()) {
                    try {
                        pecaId = UUID.fromString(fields[3].strip());
                    } catch (IllegalArgumentException e) {
                        throw new com.fiap.mekano.domain.exception.AppException(400,
                                "UUID de peça inválido no item do orçamento: " + fields[3]);
                    }
                }
                itens.add(new ItemOrcamento(
                        unescape(fields[0]),
                        Long.parseLong(fields[1]),
                        new BigDecimal(fields[2]),
                        pecaId
                ));
            }
        }
        return itens;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\p").replace(";", "\\s");
    }

    private String unescape(String s) {
        return s.replace("\\s", ";").replace("\\p", "|").replace("\\\\", "\\");
    }
}
