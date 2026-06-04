package com.fiap.mekano.infrastructure.security;

import com.fiap.mekano.domain.port.in.PasswordHasher;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementação concreta de {@link PasswordHasher} usando
 * {@link BcryptUtil} do Quarkus Elytron Security.
 *
 * <p>{@code @ApplicationScoped}: injetável em qualquer bean CDI
 * que dependa da interface {@link PasswordHasher}.
 */
@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String plainPassword) {
        return BcryptUtil.bcryptHash(plainPassword);
    }

    @Override
    public boolean matches(String plainPassword, String passwordHash) {
        return BcryptUtil.matches(plainPassword, passwordHash);
    }
}
