package com.fiap.mekano.application.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.os.OrdemServico;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.os.OsStatus;
import com.fiap.mekano.domain.port.out.OrdemServicoRepositoryPort;

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

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.CRIAR,
                usuarioEmail,
                observacao != null ? observacao : "OS criada",
                Map.of(
                        "statusAnterior", "N/A",
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    @Transactional
    public OrdemServico diagnosticar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.diagnosticar();

        ordemServicoRepository.save(os);

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.DIAGNOSTICAR,
                usuarioEmail,
                observacao != null ? observacao : "OS diagnosticada",
                Map.of(
                        "statusAnterior", statusAnterior.name(),
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    @Transactional
    public OrdemServico orcar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.orcar();

        ordemServicoRepository.save(os);

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.ORCAR,
                usuarioEmail,
                observacao != null ? observacao : "Orçamento gerado",
                Map.of(
                        "statusAnterior", statusAnterior.name(),
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    @Transactional
    public OrdemServico aprovar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.aprovar();

        ordemServicoRepository.save(os);

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.APROVAR,
                usuarioEmail,
                observacao != null ? observacao : "Orçamento aprovado pelo cliente",
                Map.of(
                        "statusAnterior", statusAnterior.name(),
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    @Transactional
    public OrdemServico executar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.executar();

        ordemServicoRepository.save(os);

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.EXECUTAR,
                usuarioEmail,
                observacao != null ? observacao : "Execução da OS iniciada",
                Map.of(
                        "statusAnterior", statusAnterior.name(),
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    @Transactional
    public OrdemServico finalizar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.finalizar();

        ordemServicoRepository.save(os);

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.FINALIZAR,
                usuarioEmail,
                observacao != null ? observacao : "OS finalizada",
                Map.of(
                        "statusAnterior", statusAnterior.name(),
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    @Transactional
    public OrdemServico cancelar(UUID osUuid, String usuarioEmail, String observacao) {
        OrdemServico os = buscarOs(osUuid);

        OsStatus statusAnterior = os.getStatus();

        os.cancelar();

        ordemServicoRepository.save(os);

        auditEventPublisher.publish(
                os.getUuid(),
                OsAuditAction.CANCELAR,
                usuarioEmail,
                observacao != null ? observacao : "OS cancelada",
                Map.of(
                        "statusAnterior", statusAnterior.name(),
                        "statusAtual", os.getStatus().name()
                )
        );

        return os;
    }

    private OrdemServico buscarOs(UUID osUuid) {
        return ordemServicoRepository.findByUuid(osUuid)
                .orElseThrow(() -> new IllegalArgumentException("OS não encontrada: " + osUuid));
    }
}