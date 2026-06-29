package com.fiap.mekano.domain.os;

public enum OsAuditAction {

    CRIAR(
            OsStatus.RECEBIDA,
            "OS recebida"
    ),

    DIAGNOSTICAR(
            OsStatus.EM_DIAGNOSTICO,
            "OS em diagnóstico"
    ),

    ORCAR(
            OsStatus.AGUARDANDO_APROVACAO,
            "OS aguardando aprovação"
    ),

    APROVAR(
            OsStatus.EM_EXECUCAO,
            "OS aprovada e enviada para execução"
    ),

    EXECUTAR(
            OsStatus.EM_EXECUCAO,
            "OS em execução"
    ),

    FINALIZAR(
            OsStatus.FINALIZADA,
            "OS finalizada"
    ),

    ENTREGAR(
            OsStatus.ENTREGUE,
            "OS entregue"
    ),

    CANCELAR(
            OsStatus.CANCELADA,
            "OS cancelada"
    );

    private final OsStatus statusDestino;
    private final String observacaoDefault;

    OsAuditAction(OsStatus statusDestino, String observacaoDefault) {
        this.statusDestino = statusDestino;
        this.observacaoDefault = observacaoDefault;
    }

    public OsStatus getStatusDestino() {
        return statusDestino;
    }

    public String getObservacaoDefault() {
        return observacaoDefault;
    }
}