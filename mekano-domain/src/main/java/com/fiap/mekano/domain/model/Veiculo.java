package com.fiap.mekano.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fiap.mekano.domain.valueobject.PlacaVeiculo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Entidade de domínio Veiculo — POJO puro sem anotações JPA.
 *
 * Representa um veículo associado a um cliente por meio do UUID do aggregate.
 * A validação da placa é responsabilidade do value object PlacaVeiculo,
 * enquanto a criação e a reconstrução da entidade são centralizadas em
 * factory methods para manter o estado consistente.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Veiculo {
    private final UUID id;

    /** * Referência ao aggregate Cliente. * O domínio referencia apenas o UUID. */
    private final UUID clienteUuid;

    private final PlacaVeiculo placa;
    private final String marca;
    private final String modelo;
    private final Integer ano;
    private final LocalDateTime createdAt;
    private final Boolean isActive;

    /**
     * Factory method — único ponto de criação de um novo veículo.
     *
     * @param clienteUuid identificador do cliente associado ao veículo
     * @param placaValue  valor da placa a ser validado pelo value object
     * @param marca       marca do veículo
     * @param modelo      modelo do veículo
     * @param ano         ano de fabricação ou modelo do veículo
     * @return nova instância de Veiculo com id e createdAt populados
     *         automaticamente
     */
    public static Veiculo create(UUID clienteUuid, String placaValue, String marca, String modelo, Integer ano) {
        return Veiculo.builder().id(UUID.randomUUID()).clienteUuid(clienteUuid).placa(new PlacaVeiculo(placaValue))
                .marca(marca).modelo(modelo).ano(ano).createdAt(LocalDateTime.now()).isActive(true).build();
    }

    /**
     * Factory method para reconstrução de um Veiculo a partir de dados persistidos.
     *
     * @param id          identificador original do veículo
     * @param clienteUuid identificador do cliente associado
     * @param placaValue  placa já persistida e validada
     * @param marca       marca do veículo
     * @param modelo      modelo do veículo
     * @param ano         ano do veículo
     * @param createdAt   data e hora original de criação
     * @return instância de Veiculo com os valores exatos recebidos
     */
    public static Veiculo reconstitute(UUID id, UUID clienteUuid, String placaValue, String marca, String modelo,
            Integer ano, LocalDateTime createdAt) {
        return reconstitute(id, clienteUuid, placaValue, marca, modelo, ano, createdAt, true);
    }

    public static Veiculo reconstitute(UUID id, UUID clienteUuid, String placaValue, String marca, String modelo,
            Integer ano, LocalDateTime createdAt, Boolean isActive) {
        return Veiculo.builder().id(id).clienteUuid(clienteUuid).placa(new PlacaVeiculo(placaValue)).marca(marca)
                .modelo(modelo).ano(ano).createdAt(createdAt).isActive(isActive).build();
    }

}
