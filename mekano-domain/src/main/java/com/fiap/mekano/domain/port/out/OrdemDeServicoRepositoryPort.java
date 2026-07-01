package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.OrdemDeServico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemDeServicoRepositoryPort {

    OrdemDeServico save(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> findById(UUID id);

    List<OrdemDeServico> findAll(int page, int size, String sort);

    long countAll();

    List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
                                            LocalDateTime dataInicio, LocalDateTime dataFim,
                                            int page, int size);

    Optional<OrdemDeServico> findByIdWithItems(UUID id);

    Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim);

    boolean existsByClienteUuidAndStatusIn(UUID clienteUuid, List<String> statuses);

    Optional<UUID> findOrcamentoUuidByOsId(UUID osId);

    void markAsDeleted(UUID id);
}
