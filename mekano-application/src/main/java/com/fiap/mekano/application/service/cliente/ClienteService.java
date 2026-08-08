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
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ClienteService implements ClienteServicePort {

    private final ClienteRepositoryPort clienteRepository;
    private final EventPublisher eventPublisher;
    private final OrdemDeServicoRepositoryPort osRepository;

    public ClienteService(ClienteRepositoryPort clienteRepository, EventPublisher eventPublisher,
                          OrdemDeServicoRepositoryPort osRepository) {
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
        this.osRepository = osRepository;
    }

    @Override
    @Transactional
    public Cliente execute(CreateClienteCommand command) {
        Cliente cliente = Cliente.create(command.nome(), command.cpf(), command.email(),
            command.telefone(), command.logradouro(), command.numero(), command.bairro(),
            command.cidade(), command.uf(), command.cep());
        Cliente saved = clienteRepository.save(cliente);
        eventPublisher.publish(ClienteCriadoEvent.of(saved));
        return saved;
    }

    @Override
    @Transactional
    public Cliente updateCliente(UUID id, UpdateClienteCommand command) {
        Cliente existente = clienteRepository.findById(id)
            .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));

        // Validar campos obrigatórios
        validarUpdate(command);

        Cliente atualizado = Cliente.reconstitute(
                id,
                command.nome(),
                existente.getCpf().getValue(),
                command.email(),
                command.telefone(),
                command.logradouro(),
                command.numero(),
                command.bairro(),
                command.cidade(),
                command.uf(),
                command.cep(),
                existente.getCreatedAt()
        );

        return clienteRepository.save(atualizado);
    }

    private void validarUpdate(UpdateClienteCommand command) {
        if (command.nome() == null || command.nome().isBlank()) {
            throw new AppException(400, Messages.get("cliente.name.required"));
        }
        if (command.email() == null || command.email().isBlank()) {
            throw new AppException(400, "E-mail é obrigatório");
        }
        if (command.logradouro() == null || command.logradouro().isBlank()) {
            throw new AppException(400, "Logradouro é obrigatório");
        }
        if (command.numero() == null || command.numero().isBlank()) {
            throw new AppException(400, "Número é obrigatório");
        }
        if (command.bairro() == null || command.bairro().isBlank()) {
            throw new AppException(400, "Bairro é obrigatório");
        }
        if (command.cidade() == null || command.cidade().isBlank()) {
            throw new AppException(400, "Cidade é obrigatória");
        }
        if (command.uf() == null || command.uf().isBlank()) {
            throw new AppException(400, "UF é obrigatória");
        }
        if (command.cep() == null || command.cep().isBlank()) {
            throw new AppException(400, "CEP é obrigatório");
        }
    }

    @Override
    public Cliente findClienteById(UUID id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));
    }

    @Override
    public List<Cliente> findAllClientes(int page, int size, String sort) {
        return clienteRepository.findAll(page, size, sort);
    }

    @Override
    public long countAllClientes() {
        return clienteRepository.countAll();
    }

    @Override
    public void deleteCliente(UUID id) {
        if (osRepository.existsByClienteUuidAndStatusIn(id, List.of("EM_EXECUCAO", "AGUARDANDO_APROVACAO"))) {
            throw new AppException(409, Messages.get("os.cliente.possui.os.ativa"));
        }
        clienteRepository.markAsDeleted(id);
    }
}
