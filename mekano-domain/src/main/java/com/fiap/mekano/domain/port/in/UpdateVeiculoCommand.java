package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para atualização de um veículo.
 *
 * A placa não está presente — não é atualizável após criação (imutável),
 * mesmo que o cliente envie o valor ele deve ser ignorado.
 */
public record UpdateVeiculoCommand(String marca, String modelo, Integer ano) {

}