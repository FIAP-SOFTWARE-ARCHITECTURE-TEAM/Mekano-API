package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidade JPA representando a tabela {@code servicos} no banco de dados.
 *
 * <p>Separada intencionalmente da entidade de domínio {@code Servico} (Clean Architecture).
 * Estende {@link BaseEntity} — herda PK sequencial ({@code Long id}) e campos de auditoria.
 *
 * <p>O campo {@code uuid} é a identidade pública exposta nos endpoints.
 */
@Entity
@Table(name = "servicos")
@Getter
@Setter
@NoArgsConstructor
public class ServicoEntity extends BaseEntity {

    @Column(unique = true, nullable = false)
    UUID uuid;

    /** Unicidade garantida por partial unique index (WHERE is_active = TRUE) no banco — não por JPA. */
    @Column(nullable = false, length = 150)
    String nome;

    @Column(length = 500)
    String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal valor;
}
