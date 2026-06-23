package com.fiap.mekano.application.service.cliente;

import com.fiap.mekano.domain.event.ClienteCriadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.in.ClienteServicePort;
import com.fiap.mekano.domain.port.in.CreateClienteCommand;
import com.fiap.mekano.domain.port.in.UpdateClienteCommand;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ClienteService implements ClienteServicePort {

    private final ClienteRepositoryPort clienteRepository;
    private final EventPublisher eventPublisher;

    public ClienteService(ClienteRepositoryPort clienteRepository, EventPublisher eventPublisher) {
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Cliente execute(CreateClienteCommand command) {
        if (command.nome() == null || command.nome().isBlank()) {
            throw new AppException(400, Messages.get("cliente.name.required"));
        }

        if (clienteRepository.existsByCpf(command.cpf())) {
            throw new AppException(409, Messages.get("cliente.already.exists", command.cpf()));
        }

        Cliente cliente = Cliente.create(
                command.nome(), command.cpf(), command.email(), command.telefone(),
                command.logradouro(), command.numero(), command.bairro(),
                command.cidade(), command.uf(), command.cep()
        );

        Cliente saved = clienteRepository.save(cliente);

        eventPublisher.publish(ClienteCriadoEvent.of(saved));

        return saved;
    }

    @Override
    @Transactional
    public Cliente update(UUID id, UpdateClienteCommand command) {
        Cliente existing = clienteRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));

        Cliente updated = Cliente.reconstitute(
                existing.getId(),
                command.nome() != null ? command.nome() : existing.getNome(),
                existing.getCpf().getValue(),
                command.email() != null ? command.email() : existing.getEmail().getValue(),
                command.telefone() != null ? command.telefone() : existing.getTelefone().getValue(),
                command.logradouro() != null ? command.logradouro() : existing.getEnderecoLogradouro(),
                command.numero() != null ? command.numero() : existing.getEnderecoNumero(),
                command.bairro() != null ? command.bairro() : existing.getEnderecoBairro(),
                command.cidade() != null ? command.cidade() : existing.getEnderecoCidade(),
                command.uf() != null ? command.uf() : existing.getEnderecoUf(),
                command.cep() != null ? command.cep() : existing.getEnderecoCep(),
                existing.getCreatedAt()
        );

        return clienteRepository.save(updated);
    }

    @Override
    public Cliente findById(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));
    }

    @Override
    public List<Cliente> findAll(int page, int size, String sort) {
        return clienteRepository.findAll(page, size, sort);
    }

    @Override
    public long countAll() {
        return clienteRepository.countAll();
    }

    @Override
    public void delete(UUID id) {
        clienteRepository.markAsDeleted(id);
    }
}
