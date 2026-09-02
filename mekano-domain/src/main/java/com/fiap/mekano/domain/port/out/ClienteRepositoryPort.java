package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryPort {

    Cliente create(Cliente cliente);

    Cliente update(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    Optional<Cliente> findByCpf(String cpf);

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByCpf(String cpf);

    /**
     * Lista clientes paginados, opcionalmente filtrados por status.
     *
     * @param isActive quando {@code null} retorna todos; {@code true} só ativos; {@code false} só inativos
     */
    List<Cliente> findAll(int page, int size, String sort, Boolean isActive);

    long countAll(Boolean isActive);

    void markAsDeleted(UUID id);

    void reactivate(UUID id);
}
