package com.fiap.mekano.application.usecase.user;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta do caso de uso de criação de usuário.
 *
 * <p>Substitui a exposição direta da entidade {@code User} (D-04):
 * em vez de retornar a entidade de domínio completa, o use case
 * retorna um record contendo apenas os dados relevantes para o caller.
 *
 * <p>Expõe apenas id, name, email e createdAt — sem passwordHash
 * ou outros campos internos da entidade de domínio.
 *
 * @param id        UUID do usuário criado
 * @param name      nome do usuário
 * @param email     email do usuário (string, não o VO)
 * @param createdAt timestamp de criação
 */
public record CreateUserResponse(UUID id, String name, String email, LocalDateTime createdAt) {}
