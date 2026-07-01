package com.fiap.mekano.domain.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.model.OrdemDeServico;

public interface OrdemDeServicoServicePort {
    OrdemDeServico create(CreateOrdemDeServicoCommand command);
    OrdemDeServico update(UUID id, CreateOrdemDeServicoCommand command);
    OrdemDeServico findById(UUID id);
    List<OrdemDeServico> findAll(int page, int size, String sort);
    long countAll();
    OrdemDeServico iniciarDiagnostico(UUID id);
    OrdemDeServico finalizarDiagnostico(FinalizarDiagnosticoCommand command);
    OrdemDeServico cancelar(UUID id, String motivo);
    OrdemDeServico entregar(UUID id, String recebidoPor);

    OrdemDeServico iniciarExecucao(UUID id, UUID mecanicoUuid, String observacao);
    OrdemDeServico finalizarExecucao(UUID id, String observacao);
    List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
                                            LocalDateTime dataInicio, LocalDateTime dataFim,
                                            int page, int size);
    Optional<OrdemDeServico> findByIdWithItems(UUID id);
    Optional<UUID> findOrcamentoUuidByOsId(UUID osId);
    Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim);
    boolean clientePossuiOsAtiva(UUID clienteUuid);
}
