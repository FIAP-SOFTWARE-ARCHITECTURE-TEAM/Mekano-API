package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraService {

    private final RequisicaoCompraRepositoryPort requisicaoRepository;
    private final PecaRepositoryPort pecaRepository;

    public RequisicaoCompraService(RequisicaoCompraRepositoryPort requisicaoRepository,
                                   PecaRepositoryPort pecaRepository) {
        this.requisicaoRepository = requisicaoRepository;
        this.pecaRepository = pecaRepository;
    }

    @Transactional
    public CreateRequisicaoCompraResponse criar(CreateRequisicaoCompraCommand command) {
        if (pecaRepository.findById(command.pecaId()).isEmpty()) {
            throw new AppException(404, Messages.get("requisicao_compra.peca.not.found", command.pecaId()));
        }
        var requisicao = RequisicaoCompra.criarParaMinimo(
                command.pecaId(),
                command.quantidade().longValue(),
                command.motivo());
        var saved = requisicaoRepository.save(requisicao);
        return new CreateRequisicaoCompraResponse(
                saved.getId(), saved.getPecaId(), saved.getQuantidade(),
                saved.getStatus().name(), saved.getMotivo().name(), saved.getCreatedAt());
    }

    public RequisicaoCompra buscarPorId(UUID id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("requisicao_compra.not.found", id)));
    }

    @Transactional
    public void marcarComoComprada(UUID id) {
        var requisicao = buscarPorId(id);
        if (!requisicao.podeSerEnviada()) {
            throw new AppException(409,
                    "Requisição não pode ser marcada como compra aprovada no status " + requisicao.getStatus());
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getPecaId(), requisicao.getQuantidade(),
                StatusRequisicao.COMPRA_APROVADA, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
    }

    @Transactional
    public void marcarComoRecebida(UUID id) {
        var requisicao = buscarPorId(id);
        if (!requisicao.podeSerRecebida()) {
            throw new AppException(409,
                    "Requisição não pode ser marcada como produto recebido no status " + requisicao.getStatus());
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getPecaId(), requisicao.getQuantidade(),
                StatusRequisicao.PRODUTO_RECEBIDO, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
    }

    @Transactional
    public void cancelar(UUID id) {
        var requisicao = buscarPorId(id);
        if (requisicao.getStatus() != StatusRequisicao.ABERTA) {
            throw new AppException(409, "Requisição não pode ser cancelada no status " + requisicao.getStatus());
        }
        if (requisicao.getMotivo() == MotivoRequisicao.ORDEM_SERVICO) {
            throw new AppException(409, Messages.get("requisicao_compra.cancelamento.bloqueado.ordem_servico"));
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getPecaId(), requisicao.getQuantidade(),
                StatusRequisicao.CANCELADA, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
    }

    @Transactional
    public void enviar(UUID id) {
        var requisicao = buscarPorId(id);
        if (!requisicao.podeSerEnviada()) {
            throw new AppException(409, "Requisição não pode ser enviada no status " + requisicao.getStatus());
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getPecaId(), requisicao.getQuantidade(),
                StatusRequisicao.ENVIADA, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
    }

    public List<RequisicaoCompra> findAll(int page, int size) {
        return requisicaoRepository.findAll(page, size);
    }

    public long countAll() {
        return requisicaoRepository.countAll();
    }
}
