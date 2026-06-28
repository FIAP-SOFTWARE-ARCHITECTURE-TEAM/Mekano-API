package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para o serviço de criação de usuário.
 *
 * Localizado no domínio (não em application) para evitar dependência cíclica.
 *
 * A senha é transportada em plaintext — o service (application) é responsável
 * por computar o hash BCrypt antes de chamar User.create().
 *
 * Sem anotações de validação (@NotBlank etc.) — validação é responsabilidade
 * do adapter que constrói este comando.
 */
public record CreateUserCommand(String name, String email, boolean active, String password) {}
