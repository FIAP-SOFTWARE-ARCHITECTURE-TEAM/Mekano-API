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
        return new OrdemServico(UUID.randomUUID(), OsStatus.RECEBIDA);
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
        validarStatus(OsStatus.EM_DIAGNOSTICO);
        this.status = OsStatus.EM_EXECUCAO;
    }

    public void orcar() {
        validarStatus(OsStatus.RECEBIDA);
        this.status = OsStatus.AGUARDANDO_APROVACAO;
    }

    public void aprovar() {
        validarStatus(OsStatus.AGUARDANDO_APROVACAO);
        this.status = OsStatus.EM_EXECUCAO;
    }

    public void executar() {
        validarStatus(OsStatus.AGUARDANDO_APROVACAO);
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
