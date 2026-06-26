package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraService {

    @Inject
    RequisicaoCompraRepositoryPort requisicaoRepository;

    public CreateRequisicaoCompraResponse criar(CreateRequisicaoCompraCommand command) {
        var requisicao = new RequisicaoCompra(
            command.pecaId(),
            command.quantidade(),
            StatusRequisicao.PENDENTE
        );

        var requisicaoCriada = requisicaoRepository.salvar(requisicao);

        return new CreateRequisicaoCompraResponse(
            requisicaoCriada.getId(),
            requisicaoCriada.getStatus().name(),
            requisicaoCriada.getQuantidade()
        );
    }

    public RequisicaoCompra buscarPorId(UUID id) {
        return requisicaoRepository.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Requisição não encontrada"));
    }

    public void marcarComoComprada(UUID id) {
        var requisicao = buscarPorId(id);
        requisicao.setStatus(StatusRequisicao.COMPRADA);
        requisicaoRepository.atualizar(requisicao);
    }

    public void marcarComoRecebida(UUID id) {
        var requisicao = buscarPorId(id);
        requisicao.setStatus(StatusRequisicao.RECEBIDA);
        requisicaoRepository.atualizar(requisicao);
    }
}
