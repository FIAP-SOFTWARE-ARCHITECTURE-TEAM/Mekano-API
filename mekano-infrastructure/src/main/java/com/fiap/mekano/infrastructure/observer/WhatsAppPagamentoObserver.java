package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
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
 * Observa a confirmação de pagamento e notifica o cliente via WhatsApp
 * quando o veículo está pronto para retirada.
 *
 * <p><b>Sem {@code @Transactional}</b> (D-11): a chamada HTTP externa não pode
 * segurar conexão de banco. {@link TransactionPhase#AFTER_SUCCESS} garante a
 * execução apenas após o commit da transação que publicou o evento.
 *
 * <p>Fluxo de resolução: OS → cliente (telefone) → veículo (placa).
 * Cliente sem telefone é ignorado (log warn — PII mascarada).
 */
@ApplicationScoped
public class WhatsAppPagamentoObserver {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppPagamentoObserver.class);

    private final WhatsAppNotifierPort notifier;
    private final OrdemDeServicoRepositoryPort osRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;

    @Inject
    public WhatsAppPagamentoObserver(WhatsAppNotifierPort notifier,
                                     OrdemDeServicoRepositoryPort osRepository,
                                     ClienteRepositoryPort clienteRepository,
                                     VeiculoRepositoryPort veiculoRepository) {
        this.notifier = notifier;
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
    }

    void aoConfirmarPagamento(@Observes(during = TransactionPhase.AFTER_SUCCESS) PagamentoConfirmadoEvent event) {
        // WR-05: observer roda APÓS o commit — nenhuma exceção pode propagar
        // ao caller (MockPaymentService.confirmarPagamento) e transformar
        // uma operação já commitada em erro 4xx/5xx. Falha de notificação é
        // logada como warn (side effect não afeta o fluxo primário).
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
            log.warn("Falha ao notificar retirada via WhatsApp (evento {}): {}", event.osUuid(), ex.getMessage());
        }
    }
}