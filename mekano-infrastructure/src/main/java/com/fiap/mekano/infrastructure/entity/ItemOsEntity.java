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
@Table(name = "os_itens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOsEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "os_uuid", nullable = false)
    private UUID osUuid;

    @Column(name = "referencia_uuid", nullable = false)
    private UUID referenciaUuid;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "quantidade", nullable = false)
    private Long quantidade;
}
