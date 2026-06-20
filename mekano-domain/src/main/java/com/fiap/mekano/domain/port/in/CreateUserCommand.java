package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para o caso de uso de criação de usuário.
 *
 * Localizado no domínio (não em application) para evitar dependência cíclica:
 * CreateUserInputPort (domain) não pode importar tipos de mekano-application.
 *
 * A senha é transportada em plaintext — o use case (application) é responsável
 * por computar o hash BCrypt antes de chamar User.create().
 *
 * Sem anotações de validação (@NotBlank etc.) — validação é responsabilidade
 * do adapter que constrói este comando.
 */
public record CreateUserCommand(String name, String email, String password) {}
