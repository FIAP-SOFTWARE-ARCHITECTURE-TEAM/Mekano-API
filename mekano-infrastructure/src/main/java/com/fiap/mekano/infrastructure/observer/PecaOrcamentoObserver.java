package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import com.fiap.mekano.domain.event.OrcamentoAprovadoEvent;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PecaOrcamentoObserver {

    private final PecaService pecaService;
    private final RequisicaoCompraService requisicaoService;

    public PecaOrcamentoObserver(PecaService pecaService, RequisicaoCompraService requisicaoService) {
        this.pecaService = pecaService;
        this.requisicaoService = requisicaoService;
    }

    @Transactional
    void aoOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event) {
        for (OrcamentoAprovadoEvent.ItemOrcamento item : event.itens()) {
            Peca peca = pecaService.buscarPorId(item.pecaId());
            pecaService.reservarSaldo(item.pecaId(), item.quantidade());
            long novoDisponivel = peca.disponivel() - item.quantidade();
            if (novoDisponivel < 0) {
                int faltante = (int) Math.abs(novoDisponivel);
                requisicaoService.criar(new CreateRequisicaoCompraCommand(
                        item.pecaId(), faltante, MotivoRequisicao.ORDEM_SERVICO));
            }
        }
    }
}