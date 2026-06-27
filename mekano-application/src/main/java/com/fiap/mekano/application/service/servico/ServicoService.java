package com.fiap.mekano.application.service.servico;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.port.in.CreateServicoCommand;
import com.fiap.mekano.domain.port.in.ServicoServicePort;
import com.fiap.mekano.domain.port.in.UpdateServicoCommand;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementação do use case de gerenciamento de serviços.
 *
 * <p>{@code @Transactional} no método de escrita (D-01).
 * Valida regras de negócio: nome não duplicado, valor > 0 (delegado ao domain model).
 */
@ApplicationScoped
public class ServicoService implements ServicoServicePort {

    private final ServicoRepositoryPort servicoRepository;

    public ServicoService(ServicoRepositoryPort servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Override
    @Transactional
    public Servico create(CreateServicoCommand command) {
        if (servicoRepository.existsByNome(command.nome())) {
            throw new AppException(409, Messages.get("servico.already.exists", command.nome()));
        }

        Servico servico = Servico.create(command.nome(), command.descricao(), command.valor());
        return servicoRepository.save(servico);
    }

    @Override
    @Transactional
    public Servico update(UUID id, UpdateServicoCommand command) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("servico.not.found", id)));

        if (servicoRepository.existsByNomeAndIdNot(command.nome(), id)) {
            throw new AppException(409, Messages.get("servico.already.exists", command.nome()));
        }

        servico.atualizar(command.nome(), command.descricao(), command.valor());
        return servicoRepository.save(servico);
    }

    @Override
    public Servico findById(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("servico.not.found", id)));
    }

    @Override
    public List<Servico> findAll(int page, int size, String sort) {
        return servicoRepository.findAll(page, size, sort);
    }

    @Override
    public long countAll() {
        return servicoRepository.countAll();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        servicoRepository.markAsDeleted(id);
    }
}
