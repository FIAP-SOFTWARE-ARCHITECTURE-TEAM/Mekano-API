package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class EstoqueMinimoObserver {

    @Inject
    RequisicaoCompraService requisicaoService;

    @Transactional
    void aoAtingirEstoqueMinimo(@Observes EstoqueMinimoAtingidoEvent event) {
        var command = new CreateRequisicaoCompraCommand(event.pecaId(), 100, MotivoRequisicao.ESTOQUE_MINIMO);
        requisicaoService.criar(command);
    }
}
