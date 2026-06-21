package com.fiap.mekano.application.service.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateUserResponse(UUID id, String name, String email, LocalDateTime createdAt) {}
