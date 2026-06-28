package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
@NoArgsConstructor
public class OrcamentoEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    UUID uuid;

    @Column(nullable = false, length = 255)
    String descricao;

    @Column(nullable = false, length = 20)
    String status;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    BigDecimal valorTotal;

    @Column(name = "ordem_servico_uuid")
    UUID ordemServicoUuid;

    @Column(name = "data_expiracao")
    LocalDateTime dataExpiracao;

    @Column(name = "itens_json", columnDefinition = "TEXT")
    String itensJson;
}
