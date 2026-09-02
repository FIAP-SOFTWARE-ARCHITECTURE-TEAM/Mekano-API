package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.ItemRequisicaoCompraRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraService {

    private final RequisicaoCompraRepositoryPort requisicaoRepository;
    private final PecaRepositoryPort pecaRepository;
    private final ItemRequisicaoCompraRepositoryPort itemRepository;

    public RequisicaoCompraService(RequisicaoCompraRepositoryPort requisicaoRepository,
                                   PecaRepositoryPort pecaRepository,
                                   ItemRequisicaoCompraRepositoryPort itemRepository) {
        this.requisicaoRepository = requisicaoRepository;
        this.pecaRepository = pecaRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public CreateRequisicaoCompraResponse criar(CreateRequisicaoCompraCommand command) {
        List<ItemRequisicaoCompra> itens = new ArrayList<>();
        for (ItemRequisicaoCompraCommand itemCmd : command.itens()) {
            if (pecaRepository.findById(itemCmd.pecaId()).isEmpty()) {
                throw new AppException(404,
                        Messages.get("requisicao_compra.peca.not.found", itemCmd.pecaId()));
            }
            itens.add(new ItemRequisicaoCompra(itemCmd.pecaId(), itemCmd.quantidade().longValue()));
        }

        var requisicao = RequisicaoCompra.criarParaMinimo(itens, command.motivo());
        var saved = requisicaoRepository.save(requisicao);

        itemRepository.saveAll(saved.getId(), saved.getItens());

        List<CreateRequisicaoCompraResponse.ItemRequisicaoCompraItemResponse> itensResponse = saved.getItens().stream()
                .map(item -> new CreateRequisicaoCompraResponse.ItemRequisicaoCompraItemResponse(
                        item.getPecaId(), item.getQuantidade()))
                .toList();

        return new CreateRequisicaoCompraResponse(
                saved.getId(), itensResponse,
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
                requisicao.getId(), requisicao.getItens(),
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
                requisicao.getId(), requisicao.getItens(),
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
                requisicao.getId(), requisicao.getItens(),
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
                requisicao.getId(), requisicao.getItens(),
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
