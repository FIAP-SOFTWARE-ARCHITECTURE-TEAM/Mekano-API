package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;

import com.fiap.mekano.domain.model.Veiculo;

/**
 * Evento de domínio disparado quando um novo veículo é criado.
 *
 * Representa a ocorrência de cadastro de um veículo e registra
 * o momento em que o evento foi gerado.
 */
public record VeiculoCriadoEvent(Veiculo veiculo, LocalDateTime occurredAt) {

    public static VeiculoCriadoEvent of(Veiculo veiculo) {
        return new VeiculoCriadoEvent(veiculo, LocalDateTime.now());
    }

}
