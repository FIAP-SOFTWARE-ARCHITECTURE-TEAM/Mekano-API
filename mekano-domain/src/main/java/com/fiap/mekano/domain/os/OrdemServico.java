package com.fiap.mekano.domain.os;

import java.util.Objects;
import java.util.UUID;

public class OrdemServico {

    private final UUID uuid;
    private OsStatus status;

    private OrdemServico(UUID uuid, OsStatus status) {
        this.uuid = Objects.requireNonNull(uuid, "uuid não pode ser nulo");
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
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
        transicionarPara(OsStatus.EM_DIAGNOSTICO);
    }

    public void orcar() {
        transicionarPara(OsStatus.AGUARDANDO_APROVACAO);
    }

    public void aprovar() {
        transicionarPara(OsStatus.EM_EXECUCAO);
    }

    /**
     * Registra a ação EXECUTAR sem mudar status.
     *
     * Pela máquina de estados atual:
     * AGUARDANDO_APROVACAO -> EM_EXECUCAO acontece em aprovar().
     *
     * Então executar() só é válido quando a OS já está EM_EXECUCAO.
     */
    public void executar() {
        if (this.status != OsStatus.EM_EXECUCAO) {
            throw new IllegalStateException(
                    "Ação EXECUTAR inválida. Status atual: " + this.status +
                    ", esperado: " + OsStatus.EM_EXECUCAO
            );
        }
    }

    public void finalizar() {
        transicionarPara(OsStatus.FINALIZADA);
    }

    public void entregar() {
        transicionarPara(OsStatus.ENTREGUE);
    }

    public void cancelar() {
        transicionarPara(OsStatus.CANCELADA);
    }

    private void transicionarPara(OsStatus destino) {
        if (!this.status.podeTransicionarPara(destino)) {
            throw new IllegalStateException(
                    "Transição inválida de " + this.status + " para " + destino
            );
        }

        this.status = destino;
    }
}