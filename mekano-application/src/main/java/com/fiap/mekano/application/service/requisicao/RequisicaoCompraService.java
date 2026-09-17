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
import io.quarkus.logging.Log;
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
        Log.infof("Criando requisição de compra: itens=%d, motivo=%s", command.itens().size(), command.motivo());
        List<ItemRequisicaoCompra> itens = new ArrayList<>();
        for (ItemRequisicaoCompraCommand itemCmd : command.itens()) {
            if (pecaRepository.findById(itemCmd.pecaId()).isEmpty()) {
                Log.warnf("Validação falhou: pecaId=%s não encontrada para item da requisição", itemCmd.pecaId());
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

        Log.infof("Requisição criada: requisicaoId=%s, itens=%d, status=%s, motivo=%s",
                saved.getId(), saved.getItens().size(), saved.getStatus(), saved.getMotivo());
        return new CreateRequisicaoCompraResponse(
                saved.getId(), itensResponse,
                saved.getStatus().name(), saved.getMotivo().name(), saved.getCreatedAt());
    }

    public RequisicaoCompra buscarPorId(UUID id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> {
                    Log.warnf("Requisição de compra não encontrada: requisicaoId=%s", id);
                    return new AppException(404, Messages.get("requisicao_compra.not.found", id));
                });
    }

    @Transactional
    public void marcarComoComprada(UUID id) {
        Log.infof("Marcando requisição como compra aprovada: requisicaoId=%s", id);
        var requisicao = buscarPorId(id);
        if (!requisicao.podeSerEnviada()) {
            Log.warnf("Validação falhou: requisicaoId=%s não pode ser marcada como COMPRA_APROVADA no status %s",
                    id, requisicao.getStatus());
            throw new AppException(409,
                    "Requisição não pode ser marcada como compra aprovada no status " + requisicao.getStatus());
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getItens(),
                StatusRequisicao.COMPRA_APROVADA, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
        Log.infof("Requisição marcada como COMPRA_APROVADA: requisicaoId=%s", id);
    }

    @Transactional
    public void marcarComoRecebida(UUID id) {
        Log.infof("Marcando requisição como produto recebido: requisicaoId=%s", id);
        var requisicao = buscarPorId(id);
        if (!requisicao.podeSerRecebida()) {
            Log.warnf("Validação falhou: requisicaoId=%s não pode ser marcada como PRODUTO_RECEBIDO no status %s",
                    id, requisicao.getStatus());
            throw new AppException(409,
                    "Requisição não pode ser marcada como produto recebido no status " + requisicao.getStatus());
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getItens(),
                StatusRequisicao.PRODUTO_RECEBIDO, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
        Log.infof("Requisição marcada como PRODUTO_RECEBIDO: requisicaoId=%s", id);
    }

    @Transactional
    public void cancelar(UUID id) {
        Log.infof("Cancelando requisição de compra: requisicaoId=%s", id);
        var requisicao = buscarPorId(id);
        if (requisicao.getStatus() != StatusRequisicao.ABERTA) {
            Log.warnf("Validação falhou: requisicaoId=%s não pode ser cancelada no status %s",
                    id, requisicao.getStatus());
            throw new AppException(409, "Requisição não pode ser cancelada no status " + requisicao.getStatus());
        }
        if (requisicao.getMotivo() == MotivoRequisicao.ORDEM_SERVICO) {
            Log.warnf("Validação falhou: requisicaoId=%s vinculada a Ordem de Serviço não pode ser cancelada", id);
            throw new AppException(409, Messages.get("requisicao_compra.cancelamento.bloqueado.ordem_servico"));
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getItens(),
                StatusRequisicao.CANCELADA, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
        Log.infof("Requisição cancelada: requisicaoId=%s", id);
    }

    @Transactional
    public void enviar(UUID id) {
        Log.infof("Enviando requisição de compra: requisicaoId=%s", id);
        var requisicao = buscarPorId(id);
        if (!requisicao.podeSerEnviada()) {
            Log.warnf("Validação falhou: requisicaoId=%s não pode ser enviada no status %s",
                    id, requisicao.getStatus());
            throw new AppException(409, "Requisição não pode ser enviada no status " + requisicao.getStatus());
        }
        var atualizada = RequisicaoCompra.reconstitute(
                requisicao.getId(), requisicao.getItens(),
                StatusRequisicao.ENVIADA, requisicao.getMotivo(), requisicao.getCreatedAt());
        requisicaoRepository.atualizar(atualizada);
        Log.infof("Requisição enviada: requisicaoId=%s, status=ENVIADA", id);
    }

    public List<RequisicaoCompra> findAll(int page, int size) {
        return requisicaoRepository.findAll(page, size);
    }

    public long countAll() {
        return requisicaoRepository.countAll();
    }
}
