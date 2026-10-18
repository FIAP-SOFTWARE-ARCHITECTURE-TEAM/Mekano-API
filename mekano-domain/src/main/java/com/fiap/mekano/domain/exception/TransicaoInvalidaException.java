package com.fiap.mekano.domain.exception;

import com.fiap.mekano.domain.model.StatusOS;

public class TransicaoInvalidaException extends DomainException {

    private final StatusOS statusAtual;
    private final StatusOS statusTentado;

    public TransicaoInvalidaException(StatusOS statusAtual, StatusOS statusTentado) {
        super("Transição inválida: " + statusAtual + " → " + statusTentado);
        this.statusAtual = statusAtual;
        this.statusTentado = statusTentado;
    }

    public StatusOS getStatusAtual() {
        return statusAtual;
    }

    public StatusOS getStatusTentado() {
        return statusTentado;
    }
}
