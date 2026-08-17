package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
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
 * Observa a finalização do diagnóstico e notifica o cliente via WhatsApp
 * quando o orçamento é gerado.
 *
 * <p><b>Sem {@code @Transactional}</b> (D-11): a chamada HTTP externa não pode
 * segurar conexão de banco. {@link TransactionPhase#AFTER_SUCCESS} garante a
 * execução apenas após o commit da transação que publicou o evento — o
 * orçamento criado por {@code DiagnosticoFinalizadoObserver} já está persistido.
 *
 * <p>Fluxo de resolução: OS → cliente (telefone) → orçamento (valor total).
 * Cliente sem telefone é ignorado (log warn — PII mascarada).
 */
@ApplicationScoped
public class WhatsAppOrcamentoObserver {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppOrcamentoObserver.class);

    private final WhatsAppNotifierPort notifier;
    private final OrdemDeServicoRepositoryPort osRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final OrcamentoRepositoryPort orcamentoRepository;
    private final VeiculoRepositoryPort veiculoRepository;

    @Inject
    public WhatsAppOrcamentoObserver(WhatsAppNotifierPort notifier,
                                     OrdemDeServicoRepositoryPort osRepository,
                                     ClienteRepositoryPort clienteRepository,
                                     OrcamentoRepositoryPort orcamentoRepository,
                                     VeiculoRepositoryPort veiculoRepository) {
        this.notifier = notifier;
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.veiculoRepository = veiculoRepository;
    }

    void aoFinalizarDiagnostico(@Observes(during = TransactionPhase.AFTER_SUCCESS) DiagnosticoFinalizadoEvent event) {
        var os = osRepository.findById(event.osUuid())
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", event.osUuid())));

        var cliente = clienteRepository.findById(os.getClienteId())
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", os.getClienteId())));

        if (cliente.getTelefone() == null) {
            log.warn("Cliente {} não possui telefone cadastrado — ignorando notificação WhatsApp", cliente.getId());
            return;
        }

        var orcamento = orcamentoRepository.findByOrdemServicoUuid(event.osUuid())
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", event.osUuid())));

        var veiculo = veiculoRepository.findById(os.getVeiculoId())
                .orElseThrow(() -> new AppException(404, Messages.get("veiculo.not.found", os.getVeiculoId())));

        notifier.notificarOrcamento(
                cliente.getTelefone().getValue(),
                cliente.getNome(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getPlaca().getValue(),
                orcamento.getValorTotal());
    }
}