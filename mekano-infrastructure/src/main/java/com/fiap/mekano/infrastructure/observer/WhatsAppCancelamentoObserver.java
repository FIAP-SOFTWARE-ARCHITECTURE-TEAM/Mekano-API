package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.OSCanceladaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observa o cancelamento da OS e notifica o cliente via WhatsApp
 * com a mesma mensagem de retirada (reaproveita {@link WhatsAppNotifierPort#notificarRetirada}).
 *
 * <p><b>Sem {@code @Transactional}</b> (D-11): a chamada HTTP externa não pode
 * segurar conexão de banco. {@link TransactionPhase#AFTER_SUCCESS} garante a
 * execução apenas após o commit da transação que publicou o evento.
 *
 * <p>Fluxo de resolução: OS → cliente (telefone) → veículo (placa).
 * Cliente sem telefone é ignorado (log warn — PII mascarada).
 */
@ApplicationScoped
public class WhatsAppCancelamentoObserver {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCancelamentoObserver.class);

    private final WhatsAppNotifierPort notifier;
    private final OrdemDeServicoRepositoryPort osRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;

    @Inject
    public WhatsAppCancelamentoObserver(WhatsAppNotifierPort notifier,
                                         OrdemDeServicoRepositoryPort osRepository,
                                         ClienteRepositoryPort clienteRepository,
                                         VeiculoRepositoryPort veiculoRepository) {
        this.notifier = notifier;
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
    }

    void aoCancelarOS(@Observes(during = TransactionPhase.AFTER_SUCCESS) OSCanceladaEvent event) {
        try {
            var os = osRepository.findById(event.osUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", event.osUuid())));

            var cliente = clienteRepository.findById(os.getClienteId())
                    .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", os.getClienteId())));

            if (cliente.getTelefone() == null) {
                log.warn("Cliente {} não possui telefone cadastrado — ignorando notificação WhatsApp", cliente.getId());
                return;
            }

            var veiculo = veiculoRepository.findById(os.getVeiculoId())
                    .orElseThrow(() -> new AppException(404, Messages.get("veiculo.not.found", os.getVeiculoId())));

            notifier.notificarRetirada(
                    cliente.getTelefone().getValue(),
                    cliente.getNome(),
                    veiculo.getPlaca().getValue(),
                    os.getId());
        } catch (Exception ex) {
            log.warn("Falha ao notificar cancelamento via WhatsApp (evento {}): {}", event.osUuid(), ex.getMessage());
        }
    }
}