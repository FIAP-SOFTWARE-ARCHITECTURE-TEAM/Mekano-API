package com.fiap.mekano.domain.port.in;

/**
 * Interface para hash de senhas — abstração no domínio.
 *
 * <p>Permite que os casos de uso (application) dependam de uma abstração,
 * não de implementações concretas de hash (ex: BcryptUtil do Quarkus).
 *
 * <p>Implementação concreta em {@code mekano-infrastructure}
 * ({@code BcryptPasswordHasher}) — CDI resolve cross-module.
 *
 * <p>Sem imports de framework — interface pura.
 */
public interface PasswordHasher {

    /**
     * Gera o hash de uma senha em plaintext.
     *
     * @param plainPassword senha em texto puro
     * @return hash da senha (formato dependente da implementação)
     */
    String hash(String plainPassword);

    /**
     * Verifica se uma senha plaintext corresponde a um hash armazenado.
     *
     * @param plainPassword senha em texto puro
     * @param passwordHash  hash armazenado
     * @return true se a senha corresponde ao hash, false caso contrário
     */
    boolean matches(String plainPassword, String passwordHash);
}
