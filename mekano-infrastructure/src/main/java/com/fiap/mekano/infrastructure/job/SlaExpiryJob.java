package com.fiap.mekano.infrastructure.job;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SlaExpiryJob {

    private final OrcamentoRepositoryPort orcamentoRepository;
    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final OsAuditEventPublisher osAuditEventPublisher;
    private final PecaRepositoryPort pecaRepository;

    public SlaExpiryJob(OrcamentoRepositoryPort orcamentoRepository,
                        OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                        OsAuditEventPublisher osAuditEventPublisher,
                        PecaRepositoryPort pecaRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.osAuditEventPublisher = osAuditEventPublisher;
        this.pecaRepository = pecaRepository;
    }

    @Scheduled(every = "12h")
    @Transactional
    public void expirarOrcamentosVencidos() {
        List<Orcamento> expirados = orcamentoRepository.findExpiradosPendentes();
        for (Orcamento orcamento : expirados) {
            orcamento.expirar();
            orcamentoRepository.save(orcamento);

            UUID osUuid = orcamento.getOrdemServicoUuid();
            if (osUuid != null) {
                ordemDeServicoRepository.findById(osUuid).ifPresent(os -> {
                    if (os.getStatus() == StatusOS.AGUARDANDO_APROVACAO) {
                        os.cancelarPorSLA();
                        ordemDeServicoRepository.save(os);

                        osAuditEventPublisher.publish(osUuid, OsAuditAction.CANCELAR, "sistema",
                                "Cancelamento automático por SLA (72h)", Map.of());
                    }
                });

                // D-14: liberar reserva de estoque para cada item de peça do orçamento
                for (ItemOrcamento item : orcamento.getItens()) {
                    if (item.getPecaId() != null) {
                        pecaRepository.liberarReserva(item.getPecaId(), item.getQuantidade().intValue());
                    }
                }
            }
        }
    }
}