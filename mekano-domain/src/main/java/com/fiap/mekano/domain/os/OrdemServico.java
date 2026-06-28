package com.fiap.mekano.domain.os;

import java.util.Objects;
import java.util.UUID;

public class OrdemServico {

    private UUID uuid;
    private OsStatus status;

    private OrdemServico(UUID uuid, OsStatus status) {
        this.uuid = Objects.requireNonNull(uuid);
        this.status = Objects.requireNonNull(status);
    }

    public static OrdemServico criarNova() {
        return new OrdemServico(UUID.randomUUID(), OsStatus.ABERTA);
    }

    public static OrdemServico restaurar(UUID uuid, OsStatus status) {
        return new OrdemServico(uuid, status);
    }

    public UUID getUuid() {
        return uuid;
    }

    public OsStatus getStatus() {
        return status;
    }

    public void diagnosticar() {
        validarStatus(OsStatus.ABERTA);
        this.status = OsStatus.DIAGNOSTICADA;
    }

    public void orcar() {
        validarStatus(OsStatus.DIAGNOSTICADA);
        this.status = OsStatus.ORCADA;
    }

    public void aprovar() {
        validarStatus(OsStatus.ORCADA);
        this.status = OsStatus.APROVADA;
    }

    public void executar() {
        validarStatus(OsStatus.APROVADA);
        this.status = OsStatus.EM_EXECUCAO;
    }

    public void finalizar() {
        validarStatus(OsStatus.EM_EXECUCAO);
        this.status = OsStatus.FINALIZADA;
    }

    public void cancelar() {
        if (this.status == OsStatus.FINALIZADA) {
            throw new IllegalStateException("OS finalizada não pode ser cancelada");
        }

        if (this.status == OsStatus.CANCELADA) {
            throw new IllegalStateException("OS já está cancelada");
        }

        this.status = OsStatus.CANCELADA;
    }

    private void validarStatus(OsStatus statusEsperado) {
        if (this.status != statusEsperado) {
            throw new IllegalStateException(
                    "Transição inválida. Status atual: " + this.status +
                    ", esperado: " + statusEsperado
            );
        }
    }
}
