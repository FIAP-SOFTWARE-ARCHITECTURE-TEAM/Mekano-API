package com.fiap.mekano.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class para entidades JPA que possuem campos de auditoria.
 *
 * <p>{@code @MappedSuperclass}: as colunas são mapeadas na tabela da entidade concreta.
 * {@code PanacheEntityBase}: mantém acesso aos métodos Panache na entidade concreta.
 *
 * <p>Campos:
 * <ul>
 *   <li>{@code createdAt} — preenchido no momento da criação da entidade</li>
 *   <li>{@code updatedAt} — atualizado automaticamente via {@code @PreUpdate}</li>
 *   <li>{@code createdBy} — UUID do usuário que criou o registro</li>
 *   <li>{@code updatedBy} — UUID do usuário que atualizou o registro</li>
 * </ul>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "updated_by")
    UUID updatedBy;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
