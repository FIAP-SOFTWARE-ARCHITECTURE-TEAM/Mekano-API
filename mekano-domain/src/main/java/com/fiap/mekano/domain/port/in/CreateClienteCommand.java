package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para criação de cliente.
 * Campos são strings brutas — a validação é responsabilidade dos VOs no domínio
 * (Cpf, Email, Telefone, Endereco), invocados por {@code Cliente.create()}.
 * Telefone é opcional (nullable).
 * CPF não é atualizável após criação (ver {@link UpdateClienteCommand}).
 */
public record CreateClienteCommand(
        String nome,
        String cpf,
        String email,
        String telefone,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String uf,
        String cep
) {}
