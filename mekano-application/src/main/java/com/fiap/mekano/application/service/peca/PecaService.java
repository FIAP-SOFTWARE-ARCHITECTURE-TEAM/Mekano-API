package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.repository.PecaRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class PecaService {

    @Inject
    PecaRepositoryPort pecaRepository;

    @Inject
    PecaRepositoryImpl pecaRepositoryImpl;

    @Inject
    EventPublisher eventPublisher;

    @Transactional
    public CreatePecaResponse criar(CreatePecaCommand command) {
        var peca = new Peca(
            command.descricao(),
            command.saldo(),
            command.estoqueMinimo()
        );

        var pecaCriada = pecaRepository.salvar(peca);

        return new CreatePecaResponse(
            pecaCriada.getId(),
            pecaCriada.getDescricao(),
            pecaCriada.getSaldo(),
            pecaCriada.getEstoqueMinimo()
        );
    }

    public Peca buscarPorId(UUID id) {
        return pecaRepository.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Peca não encontrada"));
    }

    @Transactional
    public boolean debitarSaldo(UUID pecaId, Integer quantidade) {
        var peca = buscarPorId(pecaId);
        var sucesso = pecaRepositoryImpl.debitarSaldo(pecaId, quantidade);

        if (sucesso && peca.getSaldo() - quantidade <= peca.getEstoqueMinimo()) {
            eventPublisher.publish(new EstoqueMinimoAtingidoEvent(
                pecaId,
                peca.getSaldo() - quantidade,
                peca.getEstoqueMinimo()
            ));
        }

        return sucesso;
    }

    @Transactional
    public void creditarSaldo(UUID pecaId, Integer quantidade) {
        pecaRepositoryImpl.creditarSaldo(pecaId, quantidade);
    }
}
