package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.in.RequisicaoCompraServicePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class EstoqueMinimoObserver {

    private final RequisicaoCompraServicePort requisicaoService;

    public EstoqueMinimoObserver(RequisicaoCompraServicePort requisicaoService) {
        this.requisicaoService = requisicaoService;
    }

    @Transactional
    void aoAtingirEstoqueMinimo(@Observes EstoqueMinimoAtingidoEvent event) {
        int qtd = event.estoqueMinimo() - event.saldoAtual();
        if (qtd <= 0) {
            return;
        }
        var command = new CreateRequisicaoCompraCommand(
                List.of(new ItemRequisicaoCompraCommand(event.pecaId(), qtd)),
                MotivoRequisicao.ESTOQUE_MINIMO);
        requisicaoService.criar(command);
    }
}
