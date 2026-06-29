package com.fiap.mekano.application.listener;

import com.fiap.mekano.domain.event.CobrancaEmitidaEvent;
import com.fiap.mekano.domain.event.OSFinalizadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CobrancaEmitidaListener {

    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final OrcamentoRepositoryPort orcamentoRepository;
    private final ProcessedEventsRepositoryPort processedEventsRepository;
    private final EventPublisher eventPublisher;

    // TODO(#33): substituir ProcessedEventsRepositoryPort pela implementação real
    //            (processed_events table + ProcessedEventEntity + ProcessedEventRepository)
    public CobrancaEmitidaListener(OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                                   OrcamentoRepositoryPort orcamentoRepository,
                                   ProcessedEventsRepositoryPort processedEventsRepository,
                                   EventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.processedEventsRepository = processedEventsRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    void on(@Observes OSFinalizadaEvent event) {
        if (processedEventsRepository.existsFor("COBRANCA_EMITIDA", event.ordemServicoId())) {
            return;
        }

        OrdemDeServico os = ordemDeServicoRepository.findById(event.ordemServicoId())
                .orElseThrow(() -> new AppException(404, "OS não encontrada: " + event.ordemServicoId()));

        Orcamento orcamento = orcamentoRepository.findByOrdemServicoUuid(event.ordemServicoId())
                .orElseThrow(() -> new AppException(404, "Orçamento não encontrado para OS: " + event.ordemServicoId()));

        os.emitirCobranca(orcamento.getValorTotal());
        ordemDeServicoRepository.save(os);
        processedEventsRepository.save("COBRANCA_EMITIDA", event.ordemServicoId());
        eventPublisher.publish(CobrancaEmitidaEvent.of(event.ordemServicoId(), orcamento.getValorTotal()));
    }
}