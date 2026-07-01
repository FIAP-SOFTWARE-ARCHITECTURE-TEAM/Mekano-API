package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordens_de_servico")
@Getter
@Setter
@NoArgsConstructor
public class OrdemDeServicoEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    UUID uuid;

    @Column(name = "cliente_uuid", nullable = false)
    UUID clienteUuid;

    @Column(name = "veiculo_uuid", nullable = false)
    UUID veiculoUuid;

    @Column(name = "descricao_problema", nullable = false, length = 500)
    String descricaoProblema;

    @Column(nullable = false, length = 30)
    String status;

    @Column(name = "motivo_cancelamento", length = 500)
    String motivoCancelamento;

    @Column(name = "orcamento_uuid")
    UUID orcamentoUuid;

    @Column(name = "mecanico_uuid")
    UUID mecanicoUuid;

    @Column(name = "execucao_iniciada_em")
    LocalDateTime execucaoIniciadaEm;

    @Column(name = "execucao_finalizada_em")
    LocalDateTime execucaoFinalizadaEm;

    @Column(name = "observacao_execucao", length = 1000)
    String observacaoExecucao;

    @Column(name = "data_aprovacao")
    LocalDateTime dataAprovacao;

    @Column(name = "status_pagamento", nullable = false, length = 50)
    String statusPagamento;

    @Column(name = "data_pagamento")
    LocalDateTime dataPagamento;

    @Column(name = "forma_pagamento", length = 50)
    String formaPagamento;

    @Column(name = "valor_pago")
    java.math.BigDecimal valorPago;

    @Column(name = "status_entrega", nullable = false, length = 50)
    String statusEntrega;

    @Column(name = "data_entrega")
    LocalDateTime dataEntrega;

    @Column(name = "endereco_entrega", length = 500)
    String enderecoEntrega;

    @Version
    @Column(nullable = false)
    Long version;

    @Column(name = "cobranca_gerada_em")
    private LocalDateTime cobrancaGeradaEm;

    @Column(name = "pagamento_confirmado_em")
    private LocalDateTime pagamentoConfirmadoEm;

    @Column(name = "referencia_pagamento")
    private String referenciaPagamento;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @Column(name = "recebido_por")
    private String recebidoPor;

    @PrePersist
    protected void onCreate() {
        if (statusPagamento == null) {
            statusPagamento = "PENDENTE";
        }
        if (statusEntrega == null) {
            statusEntrega = "PENDENTE";
        }
    }
}
