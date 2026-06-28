package com.fiap.mekano.infrastructure.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade JPA representando a tabela {@code users} no banco de dados.
 *
 * <p>Separada intencionalmente da entidade de domínio {@code User} (Clean Architecture).
 * Esta classe conhece JPA; a entidade de domínio não.
 *
 * <p>Estende {@link BaseEntity} — herda a PK sequencial ({@code Long id}) e os campos
 * de auditoria ({@code createdAt}, {@code updatedAt}, {@code createdBy}, {@code updatedBy}).
 *
 * <p>O campo {@code uuid} (UUID) é a identidade pública do usuário exposta nos endpoints.
 * A PK interna ({@code id}) é auto-incremento para performance em joins.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    /** UUID público — identidade exposta em APIs, referenciada por User do domínio. */
    @Column(unique = true, nullable = false)
    UUID uuid;

    @Column(nullable = false)
    String name;

    /** Armazena o valor String do Email VO. Unique constraint garante unicidade. */
    @Column(unique = true, nullable = false)
    String email;
    
    /** Armazena o valor de user ativo ou inativo. */
    @Column(unique = true, nullable = false)
    boolean active;


    @Column(name = "password_hash", nullable = false)
    String passwordHash;
}
