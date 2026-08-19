package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — contrato de persistência de serviços.
 *
 * Interface definida no domínio e implementada pelo módulo infrastructure.
 * O domínio não conhece JPA, Panache ou qualquer tecnologia de banco de dados.
 */
public interface ServicoRepositoryPort {

    Servico save(Servico servico);

    Optional<Servico> findById(UUID id);

    boolean existsByNome(String nome);

    /**
     * Verifica se existe outro serviço ativo com o mesmo nome, excluindo o próprio.
     */
    boolean existsByNomeAndIdNot(String nome, UUID id);

    List<Servico> findAll(int page, int size, String sort);

    long countAll();

    void markAsDeleted(UUID id);

    void reactivate(UUID id);
}
