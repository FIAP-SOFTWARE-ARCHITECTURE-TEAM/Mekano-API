package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.User;

/**
 * Input port — contrato do caso de uso de criação de usuário.
 *
 * Esta interface é definida no domínio e implementada pelo módulo application.
 * O adapter chama este port para iniciar o caso de uso.
 *
 * Implementação concreta: CreateUserUseCase em mekano-application (Fase 3).
 *
 * NOTA DE EVOLUÇÃO (Fase 3):
 * A assinatura atual usa parâmetros primitivos para evitar dependência cíclica
 * domain → application (CreateUserCommand ainda não existe).
 * Na Fase 3, após criar {@code CreateUserCommand} em mekano-application, este método
 * será substituído por: {@code User execute(CreateUserCommand command)}
 */
public interface CreateUserInputPort {

    /**
     * Executa o caso de uso de criação de usuário.
     *
     * @param name         nome do usuário
     * @param email        endereço de email (será validado pelo Email VO internamente)
     * @param passwordHash hash da senha pré-computado pela camada que chama este port
     * @return User criado e persistido
     */
    User execute(String name, String email, String passwordHash);
}
