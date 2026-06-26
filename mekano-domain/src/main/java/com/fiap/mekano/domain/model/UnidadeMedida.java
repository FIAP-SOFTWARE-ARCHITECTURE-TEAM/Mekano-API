package com.fiap.mekano.domain.model;

/**
 * Enum de unidades de medida para peças e insumos.
 *
 * Valores padrão de estoque:
 * - UNIDADE: peça unitária (pneu, corrente, etc.)
 * - LITRO: fluidos (óleo, anticongelante, etc.)
 * - METRO: materiais lineares (mangueira, corrente, etc.)
 * - QUILO: materiais a peso (abrasivos, graxa, etc.)
 * - CAIXA: embalagens comerciais (parafusos, porcas, etc.)
 */
public enum UnidadeMedida {
    UNIDADE("un", "Unidade"),
    LITRO("l", "Litro"),
    METRO("m", "Metro"),
    QUILO("kg", "Quilo"),
    CAIXA("cx", "Caixa");

    private final String sigla;
    private final String descricao;

    UnidadeMedida(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescricao() {
        return descricao;
    }
}
