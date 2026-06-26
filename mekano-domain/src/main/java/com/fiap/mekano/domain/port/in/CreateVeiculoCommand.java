package com.fiap.mekano.domain.port.in;

import java.util.UUID;

/**
 * Comando de entrada para criação de um veículo.
 *
 * Transporta os dados necessários para registrar um veículo
 * associado a um cliente identificado pelo UUID.
 */
public record CreateVeiculoCommand(UUID clienteUuid, String placa, String marca, String modelo, Integer ano) {

}
