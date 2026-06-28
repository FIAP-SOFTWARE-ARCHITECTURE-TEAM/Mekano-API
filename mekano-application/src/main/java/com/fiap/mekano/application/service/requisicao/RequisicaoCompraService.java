package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraService {

    private final RequisicaoCompraRepositoryPort requisicaoRepository;

    public RequisicaoCompraService(RequisicaoCompraRepositoryPort requisicaoRepository) {
        this.requisicaoRepository = requisicaoRepository;
    }

    @Transactional
    public CreateRequisicaoCompraResponse criar(CreateRequisicaoCompraCommand command) {
        var requisicao = RequisicaoCompra.criarParaMinimo(
                command.pecaId(),
                command.quantidade().longValue(),
                command.motivo());
        var saved = requisicaoRepository.salvar(requisicao);
        return new CreateRequisicaoCompraResponse(
                saved.getId(), saved.getPecaId(), saved.getQuantidade(),
                saved.getStatus().name(), saved.getMotivo().name(), saved.getCreatedAt());
    }

    public RequisicaoCompra buscarPorId(UUID id) {
        return requisicaoRepository.buscarPorId(id)
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
