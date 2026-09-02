package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pecas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PecaEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "valor_unitario", nullable = false)
    private BigDecimal valorUnitario;

    @Column(name = "saldo", nullable = false)
    private Integer saldo;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;

    @Column(name = "saldo_reservado", nullable = false)
    private Integer saldoReservado = 0;
}
