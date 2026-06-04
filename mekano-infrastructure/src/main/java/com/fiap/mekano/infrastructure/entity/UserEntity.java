package com.fiap.mekano.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA representando a tabela {@code users} no banco de dados.
 *
 * <p>Separada intencionalmente da entidade de domínio {@code User} (Clean Architecture).
 * Esta classe conhece JPA; a entidade de domínio não.
 *
 * <p>Estende {@code PanacheEntityBase} (não {@code PanacheEntity}) porque o ID é UUID,
 * não Long. {@code PanacheEntity} declara {@code @Id @GeneratedValue Long id} internamente,
 * o que conflita com UUID.
 *
 * <p>Sem {@code @GeneratedValue}: o UUID é gerado pelo domain em {@code User.create()} e
 * repassado aqui via mapper. Adicionar @GeneratedValue quebraria a invariância de identidade. (D-01)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends PanacheEntityBase {

    /** Chave primária UUID. Sem @GeneratedValue — domain gera o UUID (D-01). */
    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    /** Armazena o valor String do Email VO. Unique constraint garante unicidade. */
    @Column(unique = true, nullable = false)
    String email;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;
}
