package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaService {

    @Inject
    NfEntradaRepositoryPort nfRepository;

    @Inject
    PecaRepositoryPort pecaRepository;

    @Inject
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @Transactional
    public CreateNfEntradaResponse registrar(CreateNfEntradaCommand command) {
        var nfEntrada = new NfEntrada(
            command.pecaId(),
            command.requisicaoCompraId(),
            command.quantidade(),
            LocalDateTime.now()
        );

        var nfCriada = nfRepository.salvar(nfEntrada);

        var peca = pecaRepository.buscarPorId(command.pecaId())
            .orElseThrow(() -> new RuntimeException("Peca não encontrada"));

        peca.creditarSaldo(command.quantidade());
        pecaRepository.salvar(peca);

        var requisicao = requisicaoRepository.buscarPorId(command.requisicaoCompraId())
            .orElseThrow(() -> new RuntimeException("Requisição não encontrada"));

        requisicao.setStatus(com.fiap.mekano.domain.model.StatusRequisicao.RECEBIDA);
        requisicaoRepository.atualizar(requisicao);

        return new CreateNfEntradaResponse(
            nfCriada.getId(),
            nfCriada.getQuantidade(),
            nfCriada.getDataRecebimento()
        );
    }

    public NfEntrada buscarPorId(UUID id) {
        return nfRepository.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("NF Entrada não encontrada"));
    }
}
