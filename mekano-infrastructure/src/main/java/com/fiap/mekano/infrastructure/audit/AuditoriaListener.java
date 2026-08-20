package com.fiap.mekano.infrastructure.audit;

import com.fiap.mekano.infrastructure.entity.BaseEntity;
import io.quarkus.arc.Arc;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.UUID;

/**
 * Listener JPA responsável pelo preenchimento automático dos campos de auditoria
 * {@code createdBy}/{@code updatedBy} de qualquer entidade que estenda {@link BaseEntity}.
 *
 * <p>Resolve o ator na seguinte ordem:
 * <ol>
 *   <li>request HTTP ativo + subject do JWT é um UUID válido → UUID do usuário autenticado;</li>
 *   <li>request HTTP ativo mas sem usuário autenticado → {@link AuditoriaOrigem#PUBLICO};</li>
 *   <li>sem contexto de request (rotina interna/job) → {@link AuditoriaOrigem#SISTEMA}.</li>
 * </ol>
 *
 * <p>Na criação apenas {@code createdBy} é preenchido; {@code updatedBy} fica nulo até a
 * primeira alteração (mantendo {@code createdBy} intacto).
 */
public class AuditoriaListener {

    @PrePersist
    void onCreate(BaseEntity entity) {
        entity.setCreatedBy(atorAtual());
    }

    @PreUpdate
    void onUpdate(BaseEntity entity) {
        entity.setUpdatedBy(atorAtual());
    }

    private UUID atorAtual() {
        try {
            AuditoriaContext context = Arc.container().instance(AuditoriaContext.class).get();
            return AuditoriaOrigem.resolver(context.principalName());
        } catch (Exception e) {
            return AuditoriaOrigem.SISTEMA.getCodigo();
        }
    }
}