package com.fiap.mekano.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "uuid", unique = true, nullable = false)
    public UUID uuid = UUID.randomUUID();

    @Column(name = "peca_id", nullable = false)
    public UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    public Integer quantidade;

    @Column(name = "status", nullable = false)
    public String status;
}
