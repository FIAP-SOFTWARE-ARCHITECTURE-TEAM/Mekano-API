package com.fiap.mekano.domain.port.in;

import java.util.List;
import java.util.UUID;

import com.fiap.mekano.domain.model.Veiculo;

public interface VeiculoServicePort {

    Veiculo execute(CreateVeiculoCommand command);

    Veiculo update(UUID veiculoId, UpdateVeiculoCommand command);

    Veiculo findById(UUID veiculoId);

    List<Veiculo> findAll(int page, int size);

    long countAll();

    void delete(UUID veiculoId);

}
