package com.fiap.mekano.infrastructure.security.jwt;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.out.AccessTokenIssuerPort;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class SmallRyeAccessTokenIssuer implements AccessTokenIssuerPort {

    @ConfigProperty(name = "smallrye.jwt.new-token.issuer", defaultValue = "mekano-api")
    String issuer;

    @ConfigProperty(name = "mekano.jwt.access-token-seconds", defaultValue = "900")
    long accessTokenSeconds;

    @Override
    public String issue(UUID userUuid, String name, Role role) {
        Instant now = Instant.now();

        return Jwt.issuer(issuer)
                .subject(userUuid.toString())
                .claim("name", name)
                .groups(Set.of(role.name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenSeconds))
                .sign();
    }
}