package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Value Object que representa um item dentro de uma RequisicaoCompra.
 *
 * Regras:
 * - Cada item referencia uma única peça (pecaId) e uma quantidade solicitada.
 * - pecaId é obrigatório.
 * - quantidade deve ser > 0.
 * - Imutável: sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public class ItemRequisicaoCompra {

    private final UUID pecaId;
    private final Long quantidade;

    public ItemRequisicaoCompra(UUID pecaId, Long quantidade) {
        validatePecaId(pecaId);
        validateQuantidade(quantidade);
        this.pecaId = pecaId;
        this.quantidade = quantidade;
    }

    private static void validatePecaId(UUID pecaId) {
        if (pecaId == null) {
            throw new AppException(400, Messages.get("requisicao_compra.peca_id.required"));
        }
    }

    private static void validateQuantidade(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new AppException(400, Messages.get("requisicao_compra.quantidade.invalida"));
        }
    }
}
