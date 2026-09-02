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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nf_entradas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfEntradaEntity extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "chave_acesso", nullable = false)
    private String chaveAcesso;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "requisicao_compra_id", nullable = false)
    private UUID requisicaoCompraId;

    @Column(name = "data_recebimento", nullable = false)
    private LocalDateTime dataRecebimento;
}
