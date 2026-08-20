package com.fiap.mekano.infrastructure.audit;

import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.io.Serializable;

/**
 * Contexto de auditoria do request: expõe o principal (subject do JWT) do usuário
 * autenticado na requisição corrente.
 *
 * <p>{@code @RequestScoped} — resolvido apenas durante um request HTTP. Fora de um request
 * (jobs, rotinas internas) a resolução falha e o {@link AuditoriaListener} usa
 * {@link AuditoriaOrigem#SISTEMA}.
 *
 * <p>Baseado em {@link SecurityIdentity} (não em {@code JsonWebToken}): com
 * {@code @TestSecurity} o principal reflete o atributo {@code user} da anotação; em produção,
 * o principal é um {@code JsonWebToken} cujo {@code getName()} é o {@code subject} (UUID)
 * emitido por {@code SmallRyeAccessTokenIssuer}.
 *
 * <p>{@code @Unremovable}: o bean é obtido via lookup programático
 * ({@code Arc.container().instance(...)}) no {@link AuditoriaListener}, o que a remoção de
 * beans não utilizados do Quarkus não consegue detectar.
 */
@RequestScoped
@Unremovable
public class AuditoriaContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Instance<SecurityIdentity> identity;

    @Inject
    public AuditoriaContext(Instance<SecurityIdentity> identity) {
        this.identity = identity;
    }

    /**
     * @return o nome do principal (subject do JWT) ou {@code null} quando anônimo/inválido
     */
    public String principalName() {
        try {
            SecurityIdentity securityIdentity = identity.get();
            if (securityIdentity == null || securityIdentity.isAnonymous()) {
                return null;
            }
            return securityIdentity.getPrincipal().getName();
        } catch (Exception e) {
            return null;
        }
    }
}