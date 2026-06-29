package com.fiap.mekano.application.service;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.os.OrdemServico;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.os.OsStatus;
import com.fiap.mekano.domain.port.out.OrdemServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class OrdemServicoService {

    @Inject
    OrdemServicoRepositoryPort ordemServicoRepository;

    @Inject
    OsAuditEventPublisher auditEventPublisher;

    @Transactional
    public OrdemServico criar(String usuarioEmail, String observacao) {
        OrdemServico os = OrdemServico.criarNova();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.CRIAR,
                usuarioEmail,
                observacao,
                "N/A",
                os.getStatus().name()
        );

        return os;
    }

    @Transactional
    public OrdemServico diagnosticar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.diagnosticar();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.DIAGNOSTICAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    @Transactional
    public OrdemServico orcar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.orcar();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.ORCAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    @Transactional
    public OrdemServico aprovar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.aprovar();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.APROVAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    /**
     * Com os novos status, APROVAR já muda:
     *
     * AGUARDANDO_APROVACAO -> EM_EXECUCAO
     *
     * Portanto, EXECUTAR aqui registra o início/andamento da execução,
     * mas não muda status novamente.
     */
    @Transactional
    public OrdemServico executar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        if (os.getStatus() != OsStatus.EM_EXECUCAO) {
            throw new IllegalStateException(
                    "Ação EXECUTAR só pode ser registrada quando a OS está EM_EXECUCAO. Status atual: "
                            + os.getStatus()
            );
        }

        OsStatus statusAnterior = os.getStatus();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.EXECUTAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    @Transactional
    public OrdemServico finalizar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.finalizar();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.FINALIZAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    @Transactional
    public OrdemServico entregar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.entregar();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.ENTREGAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    @Transactional
    public OrdemServico cancelar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.cancelar();

        ordemServicoRepository.save(os);

        auditar(
                os,
                OsAuditAction.CANCELAR,
                usuarioEmail,
                observacao,
                statusAnterior.name(),
                os.getStatus().name()
        );

        return os;
    }

    private OrdemServico buscarOs(UUID osUuid) {
        return ordemServicoRepository.findByUuid(osUuid)
                .orElseThrow(() -> new IllegalArgumentException("OS não encontrada: " + osUuid));
    }

    private void auditar(
            OrdemServico os,
            OsAuditAction acao,
            String usuarioEmail,
            String observacao,
            String statusAnterior,
            String statusAtual
    ) {
        auditEventPublisher.publish(
                os.getUuid(),
                acao,
                usuarioEmail,
                observacao != null ? observacao : acao.getObservacaoDefault(),
                Map.of(
                        "statusAnterior", statusAnterior,
                        "statusAtual", statusAtual
                )
        );
    }
}