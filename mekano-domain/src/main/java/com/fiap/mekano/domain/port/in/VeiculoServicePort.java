package com.fiap.mekano.domain.port.in;

import java.util.List;
import java.util.UUID;

import com.fiap.mekano.domain.model.Veiculo;

/**
 * Input port — contrato de serviços para o gerenciamento de veículos.
 *
 * Este contrato define as operações de aplicação que o domínio
 * expõe para criar, atualizar, consultar e remover veículos.
 */
public interface VeiculoServicePort {

    Veiculo execute(CreateVeiculoCommand command);

    Veiculo update(UUID veiculoId, UpdateVeiculoCommand command);

    Veiculo findById(UUID veiculoId);

    List<Veiculo> findAll(int page, int size, String sort);

    long countAll();

    void delete(UUID veiculoId);

    void reactivate(UUID veiculoId);

}
