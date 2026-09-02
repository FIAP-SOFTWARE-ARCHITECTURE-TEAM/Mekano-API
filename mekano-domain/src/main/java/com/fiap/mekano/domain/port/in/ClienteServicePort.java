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

    /**
     * Lista clientes paginados, opcionalmente filtrados por status.
     *
     * @param isActive quando {@code null} retorna todos; {@code true} só ativos; {@code false} só inativos
     */
    List<Cliente> findAllClientes(int page, int size, String sort, Boolean isActive);

    long countAllClientes(Boolean isActive);

    void deleteCliente(UUID id);

    void reactivate(UUID id);
}
