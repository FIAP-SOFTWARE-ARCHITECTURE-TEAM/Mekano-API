package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
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
        RequisicaoCompra requisicao = requisicaoRepository.buscarPorId(command.requisicaoCompraId())
                .orElseThrow(() -> new AppException(404,
                        Messages.get("requisicao_compra.not.found", command.requisicaoCompraId())));

        if (!StatusRequisicao.PRODUTO_RECEBIDO.equals(requisicao.getStatus())) {
            throw new AppException(409, "NFe de entrada só pode ser registrada para requisições no status "
                    + StatusRequisicao.PRODUTO_RECEBIDO);
        }

        Optional<NfEntrada> existente = nfRepository.buscarPorChaveAcesso(command.chaveAcesso());
        if (existente.isPresent()) {
            if (command.requisicaoCompraId().equals(existente.get().getRequisicaoCompraId())) {
                throw new AppException(409, Messages.get("nf_entrada.chave_acesso.duplicada.mesma_requisicao"));
            }
            throw new AppException(409, Messages.get("nf_entrada.chave_acesso.duplicada.outra_requisicao"));
        }

        var nfEntrada = NfEntrada.create(
                command.chaveAcesso(), command.valorTotal(),
                requisicao.getPecaId(), command.requisicaoCompraId());
        var saved = nfRepository.salvar(nfEntrada);

        pecaRepository.creditarSaldo(requisicao.getPecaId(), requisicao.getQuantidade().intValue());

        Optional<Peca> pecaOpt = pecaRepository.buscarPorId(requisicao.getPecaId());
        pecaOpt.ifPresent(peca -> {
            if (peca.isEstoqueMinimoAtingido()) {
                eventPublisher.publish(new EstoqueMinimoAtingidoEvent(
                        peca.getId(), peca.disponivel().intValue(), peca.getEstoqueMinimo().intValue()));
            }
        });

        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getPecaId(), requisicao.getQuantidade(),
                StatusRequisicao.PRODUTO_RECEBIDO, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);

        return new CreateNfEntradaResponse(
                saved.getId(), saved.getChaveAcesso(), saved.getValorTotal(),
                saved.getPecaId(), saved.getRequisicaoCompraId(), saved.getCreatedAt());
    }

    public NfEntrada buscarPorId(UUID id) {
        return nfRepository.buscarPorId(id)
                .orElseThrow(() -> new AppException(404, Messages.get("nf_entrada.not.found", id)));
    }

    public List<NfEntrada> findAll(int page, int size) {
        return nfRepository.findAll(page, size);
    }

    public long countAll() {
        return nfRepository.countAll();
    }
}
