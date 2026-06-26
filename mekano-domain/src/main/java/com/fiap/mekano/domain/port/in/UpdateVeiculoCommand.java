package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para atualização de um veículo.
 *
 * Contém os campos alteráveis do aggregate Veiculo.
 */
public record UpdateVeiculoCommand(String placa, String marca, String modelo, Integer ano) {

}
