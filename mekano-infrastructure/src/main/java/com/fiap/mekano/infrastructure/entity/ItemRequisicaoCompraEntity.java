package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "itens_requisicao_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRequisicaoCompraEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "requisicao_compra_id", nullable = false)
    private UUID requisicaoCompraId;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;
}
