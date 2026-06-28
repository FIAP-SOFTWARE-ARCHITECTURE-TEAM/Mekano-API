package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "pecas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PecaEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    public UUID uuid = UUID.randomUUID();

    @Column(name = "descricao", nullable = false)
    public String descricao;

    @Column(name = "saldo", nullable = false)
    public Integer saldo;

    @Column(name = "estoque_minimo", nullable = false)
    public Integer estoqueMinimo;
}
