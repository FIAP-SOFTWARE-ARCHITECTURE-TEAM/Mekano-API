package com.fiap.mekano.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.model.Veiculo;

public interface VeiculoRepositoryPort {

    Veiculo save(Veiculo veiculo);

    Optional<Veiculo> findById(UUID id);

    Optional<Veiculo> findByPlaca(String placa);

    boolean existsByPlaca(String placa);

    List<Veiculo> findAll(int page, int size);

    long countAll();

    void markAsDeleted(UUID id);

}
