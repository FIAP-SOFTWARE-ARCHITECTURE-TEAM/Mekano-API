package com.fiap.mekano.application.service;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.os.StatusPagamento;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class EntregaService {

    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final EventPublisher eventPublisher;

    public EntregaService(OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                          EventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void registrarEntrega(UUID osUuid, String recebidoPor) {
        var os = ordemDeServicoRepository.findById(osUuid)
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", osUuid)));

        if (os.getStatusPagamento() != StatusPagamento.CONFIRMADO) {
            throw new AppException(409, "Não é possível entregar veículo sem pagamento confirmado");
        }

        var entregaEvent = os.entregar(recebidoPor);
        ordemDeServicoRepository.save(os);
        eventPublisher.publish(entregaEvent);
    }
}