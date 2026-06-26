package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class EstoqueMinimoObserver {

    @Inject
    RequisicaoCompraService requisicaoService;

    void aoAtingiEstoqueMinimo(@Observes EstoqueMinimoAtingidoEvent event) {
        var command = new CreateRequisicaoCompraCommand(event.pecaId(), 100);
        requisicaoService.criar(command);
    }
}
