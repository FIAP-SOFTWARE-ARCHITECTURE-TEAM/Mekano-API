package com.fiap.mekano.application.service;

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
        String normalizedNome = normalizeNome(command.nome());
        if (normalizedNome != null && !normalizedNome.isBlank() && servicoRepository.existsByNome(normalizedNome)) {
            throw new AppException(409, Messages.get("servico.already.exists", normalizedNome));
        }

        Servico servico = Servico.create(normalizedNome, command.descricao(), command.valor());
        return servicoRepository.save(servico);
    }

    @Override
    @Transactional
    public Servico update(UUID id, UpdateServicoCommand command) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("servico.not.found", id)));

        String normalizedNome = normalizeNome(command.nome());
        if (normalizedNome != null && !normalizedNome.isBlank() && servicoRepository.existsByNomeAndIdNot(normalizedNome, id)) {
            throw new AppException(409, Messages.get("servico.already.exists", normalizedNome));
        }

        servico.atualizar(normalizedNome, command.descricao(), command.valor());
        return servicoRepository.save(servico);
    }

    @Override
    public Servico findById(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("servico.not.found", id)));
    }

    @Override
    public List<Servico> findAll(int page, int size, String sort, Boolean isActive) {
        return servicoRepository.findAll(page, size, sort, isActive);
    }

    @Override
    public long countAll(Boolean isActive) {
        return servicoRepository.countAll(isActive);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        servicoRepository.markAsDeleted(id);
    }

    @Override
    @Transactional
    public void reactivate(UUID id) {
        servicoRepository.reactivate(id);
    }

    private static String normalizeNome(String nome) {
        return nome == null ? null : nome.strip();
    }
}
