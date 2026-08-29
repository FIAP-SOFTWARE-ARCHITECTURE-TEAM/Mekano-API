package com.fiap.mekano.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.model.Veiculo;

/**
 * Output port — contrato de persistência de veículos.
 *
 * Define as operações de leitura, escrita e exclusão lógica
 * esperadas pelo domínio para o aggregate Veiculo.
 * A implementação concreta fica no módulo infrastructure.
 */
public interface VeiculoRepositoryPort {

    Veiculo create(Veiculo veiculo);

    Veiculo update(Veiculo veiculo);

    Optional<Veiculo> findById(UUID id);

    Optional<Veiculo> findByPlaca(String placa);

    boolean existsByPlaca(String placa);

    /**
     * Lista veículos paginados, opcionalmente filtrados por status.
     *
     * @param isActive quando {@code null} retorna todos; {@code true} só ativos; {@code false} só inativos
     */
    List<Veiculo> findAll(int page, int size, String sort, Boolean isActive);

    long countAll(Boolean isActive);

    void markAsDeleted(UUID id);

    void reactivate(UUID id);

}