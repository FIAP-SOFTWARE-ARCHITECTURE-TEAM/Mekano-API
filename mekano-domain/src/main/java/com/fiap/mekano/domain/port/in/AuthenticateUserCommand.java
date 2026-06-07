package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para o caso de uso de autenticação.
 *
 * Localizado no domínio (não em application) para evitar dependência cíclica:
 * {@link AuthenticateUserInputPort} (domain) não pode importar tipos de mekano-application.
 *
 * A senha trafega em plaintext — o use case (application) é responsável por
 * compará-la ao hash BCrypt persistido. Sem anotações de validação aqui;
 * validação é responsabilidade do adapter que constrói o comando.
 */
public record AuthenticateUserCommand(String email, String password) {}
