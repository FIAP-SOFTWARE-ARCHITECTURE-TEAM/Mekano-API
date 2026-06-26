package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;

import com.fiap.mekano.domain.model.Veiculo;

public record VeiculoCriadoEvent(Veiculo veiculo, LocalDateTime occurredAt) {

    public static VeiculoCriadoEvent of(Veiculo veiculo) {
        return new VeiculoCriadoEvent(veiculo, LocalDateTime.now());
    }

}
