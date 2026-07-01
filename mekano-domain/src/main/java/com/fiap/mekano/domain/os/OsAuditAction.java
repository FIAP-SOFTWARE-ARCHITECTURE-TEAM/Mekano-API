package com.fiap.mekano.domain.os;

import com.fiap.mekano.domain.model.StatusOS;

public enum OsAuditAction {

    CRIAR(
    		StatusOS.RECEBIDA,
            "OS recebida"
    ),

    DIAGNOSTICAR(
    		StatusOS.EM_DIAGNOSTICO,
            "OS em diagnóstico"
    ),

    ORCAR(
    		StatusOS.AGUARDANDO_APROVACAO,
            "OS aguardando aprovação"
    ),

    APROVAR(
    		StatusOS.EM_EXECUCAO,
            "OS aprovada e enviada para execução"
    ),

    EXECUTAR(
    		StatusOS.EM_EXECUCAO,
            "OS em execução"
    ),

    FINALIZAR(
    		StatusOS.FINALIZADA,
            "OS finalizada"
    ),

    ENTREGAR(
    		StatusOS.ENTREGUE,
            "OS entregue"
    ),

    CANCELAR(
    		StatusOS.CANCELADA,
            "OS cancelada"
    ),

    PAGAMENTO_CONFIRMADO(
            StatusOS.FINALIZADA,
            "Pagamento confirmado"
    ),

    ENTREGA_REALIZADA(
            StatusOS.ENTREGUE,
            "Entrega realizada ao cliente"
    );

    private final StatusOS statusDestino;
    private final String observacaoDefault;

    OsAuditAction(StatusOS statusDestino, String observacaoDefault) {
        this.statusDestino = statusDestino;
        this.observacaoDefault = observacaoDefault;
    }

    public StatusOS getStatusDestino() {
        return statusDestino;
    }

    public String getObservacaoDefault() {
        return observacaoDefault;
    }
}