package com.fiap.mekano.domain.port.in;

/**
 * Comando de entrada para atualização de cliente.
 * CPF não está presente — não é atualizável após criação.
 * Telefone é opcional (nullable).
 * Validação dos campos é responsabilidade dos VOs no domínio.
 */
public record UpdateClienteCommand(
        String nome,
        String email,
        String telefone,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String uf,
        String cep
) {}
