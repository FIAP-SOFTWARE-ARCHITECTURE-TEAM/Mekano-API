package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Orcamento;

import java.util.List;
import java.util.UUID;

public interface OrcamentoServicePort {

    Orcamento aprovar(AprovarOrcamentoCommand command);

    Orcamento reprovar(ReprovarOrcamentoCommand command);

    Orcamento buscarPorId(UUID orcamentoUuid);

    List<Orcamento> buscarPorOrdemServico(UUID osUuid);
}
