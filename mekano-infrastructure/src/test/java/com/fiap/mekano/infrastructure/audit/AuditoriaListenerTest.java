package com.fiap.mekano.infrastructure.audit;

import com.fiap.mekano.infrastructure.entity.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica o {@link AuditoriaListener} fora do runtime Quarkus (sem contexto de
 * requisição): qualquer operação cai em {@link AuditoriaOrigem#SISTEMA}.
 */
class AuditoriaListenerTest {

    static class EntidadeAuditavel extends BaseEntity {
    }

    @Test
    void onCreate_semContextoQuarkus_devePreencherCreatedByComSISTEMA() {
        EntidadeAuditavel entity = new EntidadeAuditavel();

        new AuditoriaListener().onCreate(entity);

        assertThat(entity.getCreatedBy()).isEqualTo(AuditoriaOrigem.SISTEMA.getCodigo());
        assertThat(entity.getUpdatedBy()).isNull();
    }

    @Test
    void onUpdate_semContextoQuarkus_deveAtualizarUpdatedBy_preservandoCreatedBy() {
        EntidadeAuditavel entity = new EntidadeAuditavel();
        new AuditoriaListener().onCreate(entity);
        UUID criadoPor = entity.getCreatedBy();

        new AuditoriaListener().onUpdate(entity);

        assertThat(entity.getCreatedBy()).isEqualTo(criadoPor);
        assertThat(entity.getUpdatedBy()).isEqualTo(AuditoriaOrigem.SISTEMA.getCodigo());
    }
}