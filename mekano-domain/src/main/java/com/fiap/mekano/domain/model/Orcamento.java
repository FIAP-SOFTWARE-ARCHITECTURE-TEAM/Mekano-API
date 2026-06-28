package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Orcamento {

    private static final long SLA_HORAS = 72;

    private final UUID id;
    private final String descricao;
    private final List<ItemOrcamento> itens;
    private final BigDecimal valorTotal;
    private final LocalDateTime createdAt;

    private StatusOrcamento status;
    private UUID ordemServicoUuid;
    private LocalDateTime dataExpiracao;

    public static Orcamento create(String descricao, List<ItemOrcamento> itens) {
        return create(descricao, itens, null);
    }

    public static Orcamento create(String descricao, List<ItemOrcamento> itens, UUID ordemServicoUuid) {
        validateDescricao(descricao);
        validateItens(itens);

        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = calcularValorTotal(itens);

        return Orcamento.builder()
                .id(UUID.randomUUID())
                .descricao(descricao.strip())
                .itens(Collections.unmodifiableList(itens))
                .valorTotal(total)
                .createdAt(now)
                .status(StatusOrcamento.PENDENTE)
                .ordemServicoUuid(ordemServicoUuid)
                .dataExpiracao(ordemServicoUuid != null ? now.plusHours(SLA_HORAS) : null)
                .build();
    }

    public static Orcamento reconstitute(UUID id, String descricao, List<ItemOrcamento> itens,
                                         BigDecimal valorTotal, LocalDateTime createdAt) {
        return reconstitute(id, descricao, itens, valorTotal, createdAt,
                StatusOrcamento.PENDENTE, null, null);
    }

    public static Orcamento reconstitute(UUID id, String descricao, List<ItemOrcamento> itens,
                                         BigDecimal valorTotal, LocalDateTime createdAt,
                                         StatusOrcamento status, UUID ordemServicoUuid,
                                         LocalDateTime dataExpiracao) {
        validateDescricao(descricao);
        validateItens(itens);

        BigDecimal calculado = calcularValorTotal(itens);
        if (valorTotal.compareTo(calculado) != 0) {
            throw new AppException(400, Messages.get("orcamento.valor_total.inconsistente", valorTotal, calculado));
        }

        return Orcamento.builder()
                .id(id)
                .descricao(descricao.strip())
                .itens(Collections.unmodifiableList(itens))
                .valorTotal(valorTotal)
                .createdAt(createdAt)
                .status(status)
                .ordemServicoUuid(ordemServicoUuid)
                .dataExpiracao(dataExpiracao)
                .build();
    }

    public void aprovar() {
        if (status != StatusOrcamento.PENDENTE) {
            throw new AppException(422, Messages.get("orcamento.status.invalido.aprovar", status));
        }
        this.status = StatusOrcamento.APROVADO;
    }

    public void reprovar() {
        if (status != StatusOrcamento.PENDENTE) {
            throw new AppException(422, Messages.get("orcamento.status.invalido.reprovar", status));
        }
        this.status = StatusOrcamento.REPROVADO;
    }

    public void expirar() {
        if (status != StatusOrcamento.PENDENTE) {
            throw new AppException(422, Messages.get("orcamento.status.invalido.expirar", status));
        }
        this.status = StatusOrcamento.EXPIRADO;
    }

    public boolean isExpirado() {
        return status == StatusOrcamento.PENDENTE
                && dataExpiracao != null
                && LocalDateTime.now().isAfter(dataExpiracao);
    }

    private static void validateDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new AppException(400, Messages.get("orcamento.descricao.required"));
        }
    }

    private static void validateItens(List<ItemOrcamento> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new AppException(400, Messages.get("orcamento.itens.required"));
        }
    }

    private static BigDecimal calcularValorTotal(List<ItemOrcamento> itens) {
        return itens.stream()
                .map(ItemOrcamento::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<ItemOrcamento> getItens() {
        return itens;
    }

    public int getQuantidadeItens() {
        return itens.size();
    }
}
