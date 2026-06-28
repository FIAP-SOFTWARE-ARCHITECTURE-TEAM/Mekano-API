package com.fiap.mekano.infrastructure.entity;

import com.fiap.mekano.domain.model.Role;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    public UUID uuid;

    @Column(name = "jti", nullable = false, length = 120)
    public String jti;

    @Column(name = "token_hash", nullable = false, unique = true, length = 120)
    public String tokenHash;

    @Column(name = "user_uuid", nullable = false)
    public UUID userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    public Role role;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "rotated_at")
    public Instant rotatedAt;
}
