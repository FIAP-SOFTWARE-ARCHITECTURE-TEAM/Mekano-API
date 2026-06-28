package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidade JPA para Ordem de Serviço.
 * {@code @Version} para optimistic locking (D-26).
 */
@Entity
@Table(name = "ordens_de_servico")
@Getter
@Setter
@NoArgsConstructor
public class OrdemDeServicoEntity extends BaseEntity {

    @Column(unique = true, nullable = false)
    UUID uuid;

    @Column(name = "cliente_id", nullable = false)
    UUID clienteId;

    @Column(name = "veiculo_id", nullable = false)
    UUID veiculoId;

    @Column(name = "descricao_problema", nullable = false, length = 1000)
    String descricaoProblema;

    @Column(nullable = false, length = 30)
    String status;

    @Column(name = "motivo_cancelamento", length = 500)
    String motivoCancelamento;

    @Version
    @Column(nullable = false)
    Long version;
}
