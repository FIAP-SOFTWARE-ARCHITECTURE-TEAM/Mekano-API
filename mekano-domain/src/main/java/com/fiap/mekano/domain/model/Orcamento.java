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

/**
 * Entidade de domínio Orcamento — representa um orçamento de trabalho/peças.
 *
 * Regras:
 * - Criação APENAS via factory method {@link #create} ou {@link #reconstitute}.
 * - O builder é privado para forçar o uso dos factory methods.
 * - Contém uma lista de ItemOrcamento (peças/serviços)
 * - valorTotal é calculado automaticamente: Σ(item.subtotal) para todos os itens
 * - Imutável após criação: campos final, sem setters.
 *
 * Mapeamento JPA (OrcamentoEntity) é responsabilidade do módulo infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Orcamento {

    private final UUID id;
    private final String descricao;
    private final List<ItemOrcamento> itens;
    private final BigDecimal valorTotal;
    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de um novo orçamento.
     *
     * Calcula valorTotal internamente a partir dos itens.
     *
     * @param descricao identificação/nome do orçamento
     * @param itens lista de ItemOrcamento com pelo menos 1 item
     */
    public static Orcamento create(String descricao, List<ItemOrcamento> itens) {
        validateDescricao(descricao);
        validateItens(itens);

        BigDecimal total = calcularValorTotal(itens);

        return Orcamento.builder()
                .id(UUID.randomUUID())
                .descricao(descricao.strip())
                .itens(Collections.unmodifiableList(itens))
                .valorTotal(total)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp — preserva exatamente os valores do banco.
     * Valida que o valorTotal corresponde ao calculado a partir dos itens.
     */
    public static Orcamento reconstitute(UUID id, String descricao, List<ItemOrcamento> itens,
                                         BigDecimal valorTotal, LocalDateTime createdAt) {
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
                .build();
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

    /**
     * Calcula o valor total do orçamento como Σ(subtotal de cada item).
     */
    private static BigDecimal calcularValorTotal(List<ItemOrcamento> itens) {
        return itens.stream()
                .map(ItemOrcamento::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retorna uma cópia imutável da lista de itens.
     */
    public List<ItemOrcamento> getItens() {
        return itens;
    }

    /**
     * Retorna a quantidade de itens no orçamento.
     */
    public int getQuantidadeItens() {
        return itens.size();
    }
}
