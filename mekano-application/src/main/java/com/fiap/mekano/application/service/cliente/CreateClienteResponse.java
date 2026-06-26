package com.fiap.mekano.application.service.cliente;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateClienteResponse(
        UUID id,
        String nome,
        String cpf,
        String email,
        String telefone,
        LocalDateTime createdAt
) {}
