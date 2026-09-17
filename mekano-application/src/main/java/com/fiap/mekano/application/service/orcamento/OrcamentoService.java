package com.fiap.mekano.application.service.orcamento;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.event.OSCanceladaEvent;
import com.fiap.mekano.domain.event.OrcamentoAprovadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.DomainException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OSMetricsPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class OrcamentoService implements OrcamentoServicePort {

    private final OrcamentoRepositoryPort orcamentoRepository;
    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final EventPublisher eventPublisher;
    private final OsAuditEventPublisher osAuditEventPublisher;
    private final OSMetricsPort osMetrics;

    public OrcamentoService(OrcamentoRepositoryPort orcamentoRepository,
                            OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                            EventPublisher eventPublisher,
                            OsAuditEventPublisher osAuditEventPublisher,
                            OSMetricsPort osMetrics) {
        this.orcamentoRepository = orcamentoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
        this.osAuditEventPublisher = osAuditEventPublisher;
        this.osMetrics = osMetrics;
    }

    @Override
    @Transactional
    public Orcamento aprovar(AprovarOrcamentoCommand command) {
        Log.infof("Aprovando orçamento: orcamentoUuid=%s", command.orcamentoUuid());
        Orcamento orcamento = orcamentoRepository.findByUuid(command.orcamentoUuid())
                .orElseThrow(() -> {
                    Log.warnf("Orçamento não encontrado: orcamentoUuid=%s", command.orcamentoUuid());
                    return new AppException(404, Messages.get("orcamento.not.found", command.orcamentoUuid()));
                });

        executarTransicao("aprovar", orcamento.getId(), () -> orcamento.aprovar());

        if (orcamento.getOrdemServicoUuid() != null) {
            OrdemDeServico os = ordemDeServicoRepository.findById(orcamento.getOrdemServicoUuid())
                    .orElseThrow(() -> {
                        Log.warnf("Ordem de Serviço não encontrada: osId=%s", orcamento.getOrdemServicoUuid());
                        return new AppException(404, Messages.get("os.not.found", orcamento.getOrdemServicoUuid()));
                    });
            executarTransicao("aprovarOrcamento", os.getId(), () -> os.aprovarOrcamento(orcamento.getId()));
            ordemDeServicoRepository.save(os);
            osMetrics.registrarTransicaoStatus(
                    com.fiap.mekano.domain.model.StatusOS.AGUARDANDO_APROVACAO,
                    com.fiap.mekano.domain.model.StatusOS.AGUARDANDO_EXECUCAO);
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

        Log.infof("Orçamento aprovado: orcamentoUuid=%s, osId=%s", saved.getId(), orcamento.getOrdemServicoUuid());
        return saved;
    }

    @Override
    @Transactional
    public Orcamento reprovar(ReprovarOrcamentoCommand command) {
        Log.infof("Reprovando orçamento: orcamentoUuid=%s", command.orcamentoUuid());
        Orcamento orcamento = orcamentoRepository.findByUuid(command.orcamentoUuid())
                .orElseThrow(() -> {
                    Log.warnf("Orçamento não encontrado: orcamentoUuid=%s", command.orcamentoUuid());
                    return new AppException(404, Messages.get("orcamento.not.found", command.orcamentoUuid()));
                });

        executarTransicao("reprovar", orcamento.getId(), () -> orcamento.reprovar());

        UUID osUuid = orcamento.getOrdemServicoUuid();

        if (orcamento.getOrdemServicoUuid() != null) {
            OrdemDeServico os = ordemDeServicoRepository.findById(orcamento.getOrdemServicoUuid())
                    .orElseThrow(() -> {
                        Log.warnf("Ordem de Serviço não encontrada: osId=%s", orcamento.getOrdemServicoUuid());
                        return new AppException(404, Messages.get("os.not.found", orcamento.getOrdemServicoUuid()));
                    });
            executarTransicao("reprovarOrcamento", os.getId(), () -> os.reprovarOrcamento(command.motivo()));
            ordemDeServicoRepository.save(os);
            osMetrics.registrarTransicaoStatus(
                    com.fiap.mekano.domain.model.StatusOS.AGUARDANDO_APROVACAO,
                    com.fiap.mekano.domain.model.StatusOS.CANCELADA);
            osMetrics.registrarTempoFase("TOTAL", Duration.between(os.getCreatedAt(), LocalDateTime.now()));
        }

        Orcamento saved = orcamentoRepository.save(orcamento);

        if (osUuid != null) {
            osAuditEventPublisher.publish(osUuid, OsAuditAction.CANCELAR, null,
                    "Orçamento reprovado pelo cliente", Map.of());
            eventPublisher.publish(OSCanceladaEvent.of(osUuid, "Orçamento reprovado pelo cliente"));
        }

        Log.infof("Orçamento reprovado: orcamentoUuid=%s, osId=%s", saved.getId(), osUuid);
        return saved;
    }

    @Override
    public Orcamento buscarPorId(UUID orcamentoUuid) {
        return orcamentoRepository.findByUuid(orcamentoUuid)
                .orElseThrow(() -> {
                    Log.warnf("Orçamento não encontrado: orcamentoUuid=%s", orcamentoUuid);
                    return new AppException(404, Messages.get("orcamento.not.found", orcamentoUuid));
                });
    }

    @Override
    public Orcamento buscarPorOrdemServico(UUID osUuid) {
        return orcamentoRepository.findByOrdemServicoUuid(osUuid)
                .orElseThrow(() -> {
                    Log.warnf("Nenhum orçamento encontrado para a OS: osId=%s", osUuid);
                    return new AppException(404, "Nenhum orçamento encontrado para a OS: " + osUuid);
                });
    }

    /**
     * Executa uma transição de estado de Orçamento/OrdemDeServico.
     * Regras de negócio violadas (DomainException → HTTP 422) são registradas como
     * WARN e relançadas — erros de usuário não são ERROR.
     */
    private void executarTransicao(String acao, UUID idContexto, Runnable transicao) {
        try {
            transicao.run();
        } catch (DomainException ex) {
            Log.warnf("Transição rejeitada: acao=%s, id=%s, detalhe=%s", acao, idContexto, ex.getMessage());
            throw ex;
        }
    }
}
