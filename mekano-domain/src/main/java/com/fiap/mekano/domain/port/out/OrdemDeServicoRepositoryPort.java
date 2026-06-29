package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.OrdemDeServico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemDeServicoRepositoryPort {

    OrdemDeServico save(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> findById(UUID id);

    List<OrdemDeServico> findAll(int page, int size, String sort);

    long countAll();

    List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, int page, int size);

    Optional<OrdemDeServico> findByIdWithItems(UUID id);

    Optional<Double> calcularTempoMedioExecucao();

    void markAsDeleted(UUID id);
}
