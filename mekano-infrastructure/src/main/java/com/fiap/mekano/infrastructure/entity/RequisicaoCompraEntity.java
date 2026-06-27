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
@Table(name = "requisicoes_compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisicaoCompraEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    public UUID uuid = UUID.randomUUID();

    @Column(name = "peca_id", nullable = false)
    public UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    public Integer quantidade;

    @Column(name = "status", nullable = false)
    public String status;
}
