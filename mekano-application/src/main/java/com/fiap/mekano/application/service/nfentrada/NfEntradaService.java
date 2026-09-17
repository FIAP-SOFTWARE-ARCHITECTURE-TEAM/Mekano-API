package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaService {

    private final NfEntradaRepositoryPort nfRepository;
    private final PecaRepositoryPort pecaRepository;
    private final RequisicaoCompraRepositoryPort requisicaoRepository;
    private final EventPublisher eventPublisher;

    public NfEntradaService(NfEntradaRepositoryPort nfRepository, PecaRepositoryPort pecaRepository,
                            RequisicaoCompraRepositoryPort requisicaoRepository, EventPublisher eventPublisher) {
        this.nfRepository = nfRepository;
        this.pecaRepository = pecaRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreateNfEntradaResponse registrar(CreateNfEntradaCommand command) {
        Log.infof("Registrando NFe de entrada: requisicaoCompraId=%s", command.requisicaoCompraId());
        RequisicaoCompra requisicao = requisicaoRepository.findById(command.requisicaoCompraId())
                .orElseThrow(() -> {
                    Log.warnf("Validação falhou: requisicaoCompraId=%s não encontrada", command.requisicaoCompraId());
                    return new AppException(404,
                            Messages.get("requisicao_compra.not.found", command.requisicaoCompraId()));
                });

        if (!StatusRequisicao.PRODUTO_RECEBIDO.equals(requisicao.getStatus())) {
            Log.warnf("Validação falhou: requisicaoCompraId=%s não está em PRODUTO_RECEBIDO (status atual=%s)",
                    command.requisicaoCompraId(), requisicao.getStatus());
            throw new AppException(409, "NFe de entrada só pode ser registrada para requisições no status "
                    + StatusRequisicao.PRODUTO_RECEBIDO);
        }

        Optional<NfEntrada> existente = nfRepository.buscarPorChaveAcesso(command.chaveAcesso());
        if (existente.isPresent()) {
            if (command.requisicaoCompraId().equals(existente.get().getRequisicaoCompraId())) {
                Log.warnf("Validação falhou: chave de acesso da NFe já registrada para requisicaoCompraId=%s",
                        command.requisicaoCompraId());
                throw new AppException(409, Messages.get("nf_entrada.chave_acesso.duplicada.mesma_requisicao"));
            }
            Log.warnf("Validação falhou: chave de acesso da NFe já registrada para outra requisição (requisicaoCompraId=%s)",
                    command.requisicaoCompraId());
            throw new AppException(409, Messages.get("nf_entrada.chave_acesso.duplicada.outra_requisicao"));
        }

        var nfEntrada = NfEntrada.create(
                command.chaveAcesso(), command.valorTotal(),
                command.requisicaoCompraId());
        var saved = nfRepository.save(nfEntrada);
        Log.infof("NFe registrada: nfId=%s, requisicaoCompraId=%s, valorTotal=%.2f",
                saved.getId(), saved.getRequisicaoCompraId(), saved.getValorTotal());

        for (ItemRequisicaoCompra item : requisicao.getItens()) {
            pecaRepository.creditarSaldo(item.getPecaId(), item.getQuantidade().intValue());
            Log.infof("Saldo creditado: pecaId=%s, quantidade=%d", item.getPecaId(), item.getQuantidade());

            Optional<Peca> pecaOpt = pecaRepository.findById(item.getPecaId());
            pecaOpt.ifPresent(peca -> {
                if (peca.isEstoqueMinimoAtingido()) {
                    Log.infof("Estoque mínimo atingido: pecaId=%s, disponivel=%d, minimo=%d",
                            peca.getId(), peca.disponivel().intValue(), peca.getEstoqueMinimo().intValue());
                    eventPublisher.publish(new EstoqueMinimoAtingidoEvent(
                            peca.getId(), peca.disponivel().intValue(), peca.getEstoqueMinimo().intValue()));
                }
            });
        }

        return new CreateNfEntradaResponse(
                saved.getId(), saved.getChaveAcesso(), saved.getValorTotal(),
                saved.getRequisicaoCompraId(), saved.getCreatedAt());
    }

    public NfEntrada buscarPorId(UUID id) {
        return nfRepository.findById(id)
                .orElseThrow(() -> {
                    Log.warnf("NFe de entrada não encontrada: nfId=%s", id);
                    return new AppException(404, Messages.get("nf_entrada.not.found", id));
                });
    }

    public List<NfEntrada> findAll(int page, int size) {
        return nfRepository.findAll(page, size);
    }

    public long countAll() {
        return nfRepository.countAll();
    }
}
