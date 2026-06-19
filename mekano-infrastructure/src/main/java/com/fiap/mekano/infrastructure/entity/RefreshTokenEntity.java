package com.fiap.mekano.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA representando a tabela {@code refresh_tokens} no banco de dados.
 *
 * <p>Separada intencionalmente dos records de domínio (Clean Architecture).
 * Esta classe conhece JPA; os objetos de domínio ({@code RefreshTokenData}) não.
 *
 * <p>A PK sequencial ({@code Long id}) é uso interno do banco.
 * O UUID público ({@code uuid}) é gerado pelo serviço de infraestrutura.
 *
 * <p>Sem {@code @ManyToOne} para {@code UserEntity}: apenas UUID da FK mantém o acoplamento
 * mínimo entre as entidades JPA.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity extends PanacheEntityBase {

    /** PK sequencial — auto-incremento interno. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** UUID público — identidade do token gerada pelo serviço. */
    @Column(unique = true, nullable = false)
    UUID uuid;

    /** Identificador único do token (UUID v4). */
    @Column(nullable = false, unique = true)
    String jti;

    /** SHA-256 do token em hexadecimal. */
    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash;

    /** UUID do usuário proprietário (FK para users.uuid). */
    @Column(name = "user_uuid", nullable = false)
    UUID userUuid;

    /** Momento em que o token expira. */
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    /** Momento em que o token foi rotacionado (null se ativo). */
    @Column(name = "rotated_at")
    Instant rotatedAt;

    /** Momento de criação do registro. */
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
