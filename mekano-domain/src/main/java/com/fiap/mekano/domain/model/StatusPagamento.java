package com.fiap.mekano.domain.model;

public enum StatusPagamento {
    PENDENTE("Charge emitted, awaiting confirmation"),
    CONFIRMADO("Payment confirmed by mock bank");

    private final String descricao;

    StatusPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}