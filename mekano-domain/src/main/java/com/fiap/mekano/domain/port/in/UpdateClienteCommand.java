package com.fiap.mekano.domain.port.in;

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
