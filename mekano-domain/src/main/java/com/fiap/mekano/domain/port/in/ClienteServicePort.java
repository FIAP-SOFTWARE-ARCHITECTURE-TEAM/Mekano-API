package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Cliente;

import java.util.List;
import java.util.UUID;

/**
 * Input port para operações de Cliente.
 * Define o contrato do serviço de domínio — implementado por {@code ClienteService}
 * na camada application.
 */
public interface ClienteServicePort {

    Cliente execute(CreateClienteCommand command);

    Cliente update(UUID id, UpdateClienteCommand command);

    Cliente findById(UUID id);

    List<Cliente> findAll(int page, int size, String sort);

    long countAll();

    void delete(UUID id);
}
