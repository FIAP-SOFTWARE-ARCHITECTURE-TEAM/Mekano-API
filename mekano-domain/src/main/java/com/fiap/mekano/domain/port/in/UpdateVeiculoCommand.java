package com.fiap.mekano.domain.port.in;

public record UpdateVeiculoCommand(String placa, String marca, String modelo, Integer ano) {

}
