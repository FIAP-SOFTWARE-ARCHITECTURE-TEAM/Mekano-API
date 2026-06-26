package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.OrcamentoAprovadoEvent;
import com.fiap.mekano.application.service.peca.PecaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PecaOrcamentoObserver {

    @Inject
    PecaService pecaService;

    void aoOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event) {
        for (var item : event.itens()) {
            pecaService.debitarSaldo(item.pecaId(), item.quantidade());
        }
    }
}
