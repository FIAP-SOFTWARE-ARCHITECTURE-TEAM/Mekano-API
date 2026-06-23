package com.fiap.mekano.domain.port.in;

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
