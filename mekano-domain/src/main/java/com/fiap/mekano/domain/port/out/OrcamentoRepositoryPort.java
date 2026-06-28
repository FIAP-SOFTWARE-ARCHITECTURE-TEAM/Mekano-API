package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Orcamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepositoryPort {

    Orcamento save(Orcamento orcamento);

    Optional<Orcamento> findByUuid(UUID uuid);

    Optional<Orcamento> findByOrdemServicoUuid(UUID ordemServicoUuid);

    List<Orcamento> findExpiradosPendentes();
}
