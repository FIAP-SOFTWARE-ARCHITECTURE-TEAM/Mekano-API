package com.fiap.mekano.infrastructure.event;

import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DiagnosticoFinalizadoObserver {

    @Inject
    OrdemDeServicoRepositoryPort ordemDeServicoRepository;

    @Inject
    OrcamentoRepositoryPort orcamentoRepository;

    @Transactional
    void onDiagnosticoFinalizado(@Observes DiagnosticoFinalizadoEvent event) {
        OrdemDeServico os = ordemDeServicoRepository.findById(event.osUuid())
                .orElseThrow(() -> new AppException(404, "OS não encontrada: " + event.osUuid()));

        Orcamento orcamento = Orcamento.create(event.descricao(), event.itens(), event.osUuid());
        orcamentoRepository.save(orcamento);

        os.associarOrcamento(orcamento.getId());
        ordemDeServicoRepository.save(os);
    }
}