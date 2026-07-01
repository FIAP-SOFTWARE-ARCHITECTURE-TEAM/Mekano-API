package com.fiap.mekano.domain.port.in;

import java.util.List;
import java.util.UUID;

public interface AdminUserServicePort {

    AdminCreatedUser criarUsuario(CreateAdminUserCommand command);

    List<AdminUserSummary> listar(int page, int size);

    void deletar(UUID uuid);
}