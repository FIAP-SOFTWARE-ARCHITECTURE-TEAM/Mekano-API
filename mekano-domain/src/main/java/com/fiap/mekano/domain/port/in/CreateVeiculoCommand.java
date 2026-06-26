package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record CreateVeiculoCommand(UUID clienteUuid, String placa, String marca, String modelo, Integer ano) {

}
