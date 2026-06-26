package com.fiap.mekano.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fiap.mekano.domain.valueobject.PlacaVeiculo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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

    public static Veiculo create(UUID clienteUuid, String placaValue, String marca, String modelo, Integer ano) {
        return Veiculo.builder().id(UUID.randomUUID()).clienteUuid(clienteUuid).placa(new PlacaVeiculo(placaValue))
                .marca(marca).modelo(modelo).ano(ano).createdAt(LocalDateTime.now()).build();
    }

    public static Veiculo reconstitute(UUID id, UUID clienteUuid, String placaValue, String marca, String modelo,
            Integer ano, LocalDateTime createdAt) {
        return Veiculo.builder().id(id).clienteUuid(clienteUuid).placa(new PlacaVeiculo(placaValue)).marca(marca)
                .modelo(modelo).ano(ano).createdAt(createdAt).build();
    }

}
