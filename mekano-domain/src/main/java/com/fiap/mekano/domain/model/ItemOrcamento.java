package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Value Object que representa um item individual dentro de um Orcamento.
 *
 * Regras:
 * - Encapsula referência a uma peça/serviço, quantidade e valor unitário
 * - Calcula o subtotal (qtd × unitario) sob demanda
 * - Quantidade deve ser > 0
 * - Valor unitário deve ser >= 0
 * - Imutável: sem setters, @EqualsAndHashCode por valor
 *
 * Nota: Este é um Value Object, não uma entidade — não tem UUID nem ciclo de vida independente.
 */
@Getter
@EqualsAndHashCode
@ToString
public class ItemOrcamento {

    private final String descricao;
    private final Long quantidade;
    private final BigDecimal valorUnitario;

    /**
     * Construtor com validação.
     *
     * @param descricao nome/descrição do item (peça ou serviço)
     * @param quantidade quantidade do item
     * @param valorUnitario valor unitário do item
     * @throws AppException se validações falharem
     */
    public ItemOrcamento(String descricao, Long quantidade, BigDecimal valorUnitario) {
        this.descricao = validateDescricao(descricao);
        this.quantidade = validateQuantidade(quantidade);
        this.valorUnitario = validateValorUnitario(valorUnitario);
    }

    private static String validateDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new AppException(400, Messages.get("item_orcamento.descricao.required"));
        }
        return descricao.strip();
    }

    private static Long validateQuantidade(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new AppException(400, Messages.get("item_orcamento.quantidade.invalida"));
        }
        return quantidade;
    }

    private static BigDecimal validateValorUnitario(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(400, Messages.get("item_orcamento.valor.invalido"));
        }
        return valor;
    }

    /**
     * Calcula o subtotal deste item: quantidade × valor unitário.
     */
    public BigDecimal calcularSubtotal() {
        return valorUnitario.multiply(new BigDecimal(quantidade));
    }
}
