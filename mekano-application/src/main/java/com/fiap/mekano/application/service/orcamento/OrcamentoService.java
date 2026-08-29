package com.fiap.mekano.application.service.orcamento;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.event.OrcamentoAprovadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class OrcamentoService implements OrcamentoServicePort {

    private final OrcamentoRepositoryPort orcamentoRepository;
    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final EventPublisher eventPublisher;
    private final OsAuditEventPublisher osAuditEventPublisher;

    public OrcamentoService(OrcamentoRepositoryPort orcamentoRepository,
                            OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                            EventPublisher eventPublisher,
                            OsAuditEventPublisher osAuditEventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
        this.osAuditEventPublisher = osAuditEventPublisher;
    }

    @Override
    @Transactional
    public Orcamento aprovar(AprovarOrcamentoCommand command) {
        Orcamento orcamento = orcamentoRepository.findByUuid(command.orcamentoUuid())
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", command.orcamentoUuid())));

        orcamento.aprovar();

        if (orcamento.getOrdemServicoUuid() != null) {
            OrdemDeServico os = ordemDeServicoRepository.findById(orcamento.getOrdemServicoUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", orcamento.getOrdemServicoUuid())));
            os.aprovarOrcamento(orcamento.getId());
            ordemDeServicoRepository.save(os);
        }

        Orcamento saved = orcamentoRepository.save(orcamento);

        List<OrcamentoAprovadoEvent.ItemOrcamento> itens = saved.getItens().stream()
                .filter(i -> i.getPecaId() != null)
                .map(i -> new OrcamentoAprovadoEvent.ItemOrcamento(i.getPecaId(), i.getQuantidade().intValue()))
                .toList();
        if (!itens.isEmpty()) {
            eventPublisher.publish(new OrcamentoAprovadoEvent(saved.getId(), itens));
        }

        if (orcamento.getOrdemServicoUuid() != null) {
            osAuditEventPublisher.publish(orcamento.getOrdemServicoUuid(), OsAuditAction.APROVAR, null,
                    OsAuditAction.APROVAR.getObservacaoDefault(), Map.of());
        }

        return saved;
    }

    @Override
    @Transactional
    public Orcamento reprovar(ReprovarOrcamentoCommand command) {
        Orcamento orcamento = orcamentoRepository.findByUuid(command.orcamentoUuid())
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", command.orcamentoUuid())));

        orcamento.reprovar();

        UUID osUuid = orcamento.getOrdemServicoUuid();

        if (orcamento.getOrdemServicoUuid() != null) {
            OrdemDeServico os = ordemDeServicoRepository.findById(orcamento.getOrdemServicoUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", orcamento.getOrdemServicoUuid())));
            os.reprovarOrcamento(command.motivo());
            ordemDeServicoRepository.save(os);
        }

        Orcamento saved = orcamentoRepository.save(orcamento);

        if (osUuid != null) {
            osAuditEventPublisher.publish(osUuid, OsAuditAction.CANCELAR, null,
                    "Orçamento reprovado pelo cliente", Map.of());
        }

        return saved;
    }

    @Override
    public Orcamento buscarPorId(UUID orcamentoUuid) {
        return orcamentoRepository.findByUuid(orcamentoUuid)
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", orcamentoUuid)));
    }

    @Override
    public Orcamento buscarPorOrdemServico(UUID osUuid) {
        return orcamentoRepository.findByOrdemServicoUuid(osUuid)
                .orElseThrow(() -> new AppException(404, "Nenhum orçamento encontrado para a OS: " + osUuid));
    }
}
