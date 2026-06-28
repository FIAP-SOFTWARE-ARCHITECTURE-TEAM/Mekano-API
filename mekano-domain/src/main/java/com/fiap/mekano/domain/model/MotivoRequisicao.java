package com.fiap.mekano.domain.model;

public enum MotivoRequisicao {
    ORDEM_SERVICO("Ordem de Serviço"),
    ESTOQUE_MINIMO("Estoque Mínimo");

    private final String descricao;

    MotivoRequisicao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
