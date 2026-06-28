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
}
