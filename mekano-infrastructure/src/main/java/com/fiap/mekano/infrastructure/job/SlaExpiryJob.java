package com.fiap.mekano.infrastructure.job;

import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SlaExpiryJob {

    @Inject
    OrcamentoRepositoryPort orcamentoRepository;

    @Scheduled(every = "60s")
    @Transactional
    public void expirarOrcamentosVencidos() {
        List<Orcamento> expirados = orcamentoRepository.findExpiradosPendentes();
        for (Orcamento orcamento : expirados) {
            orcamento.expirar();
            orcamentoRepository.save(orcamento);
        }
    }
}
