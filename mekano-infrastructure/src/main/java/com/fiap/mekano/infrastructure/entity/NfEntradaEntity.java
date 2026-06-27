package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nf_entradas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfEntradaEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    public UUID uuid = UUID.randomUUID();

    @Column(name = "peca_id", nullable = false)
    public UUID pecaId;

    @Column(name = "requisicao_compra_id", nullable = false)
    public UUID requisicaoCompraId;

    @Column(name = "quantidade", nullable = false)
    public Integer quantidade;

    @Column(name = "data_recebimento", nullable = false)
    public LocalDateTime dataRecebimento;
}
