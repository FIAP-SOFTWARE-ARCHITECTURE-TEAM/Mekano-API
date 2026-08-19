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

    Cliente updateCliente(UUID id, UpdateClienteCommand command);

    Cliente findClienteById(UUID id);

    List<Cliente> findAllClientes(int page, int size, String sort);

    long countAllClientes();

    void deleteCliente(UUID id);

    void reactivate(UUID id);
}
