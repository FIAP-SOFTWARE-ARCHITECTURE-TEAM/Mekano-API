package com.fiap.mekano.domain.port.out;


import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.os.OrdemServico;

public interface OrdemServicoRepositoryPort {

    OrdemServico save(OrdemServico ordemServico);

    Optional<OrdemServico> findByUuid(UUID uuid);
}