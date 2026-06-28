package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Orcamento;

public interface OrcamentoServicePort {

    Orcamento gerarOrcamento(GerarOrcamentoCommand command);

    Orcamento aprovar(AprovarOrcamentoCommand command);

    Orcamento reprovar(ReprovarOrcamentoCommand command);
}
