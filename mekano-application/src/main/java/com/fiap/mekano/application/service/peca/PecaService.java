package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class PecaService {

    @Inject
    PecaRepositoryPort pecaRepository;

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
}
