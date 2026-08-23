package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Servico;

import java.util.List;
import java.util.UUID;

/**
 * Input port — contrato dos serviços de gerenciamento de Servico.
 *
 * Interface definida no domínio e implementada pelo módulo application.
 */
public interface ServicoServicePort {

    Servico create(CreateServicoCommand command);

    Servico update(UUID id, UpdateServicoCommand command);

    Servico findById(UUID id);

    /**
     * Lista serviços paginados, opcionalmente filtrados por status.
     *
     * @param isActive quando {@code null} retorna todos; {@code true} só ativos; {@code false} só inativos
     */
    List<Servico> findAll(int page, int size, String sort, Boolean isActive);

    long countAll(Boolean isActive);

    void delete(UUID id);

    void reactivate(UUID id);
}
