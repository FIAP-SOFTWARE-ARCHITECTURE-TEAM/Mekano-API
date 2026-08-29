package com.fiap.mekano.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface AdminUserServicePort {

    AdminCreatedUser criarUsuario(CreateAdminUserCommand command);

    /**
     * Lista usuários paginados, opcionalmente filtrados por status.
     *
     * @param isActive quando {@code null} retorna todos; {@code true} só ativos; {@code false} só inativos
     */
    List<AdminUserSummary> listar(int page, int size, Boolean isActive);

    void deletar(UUID uuid);
}