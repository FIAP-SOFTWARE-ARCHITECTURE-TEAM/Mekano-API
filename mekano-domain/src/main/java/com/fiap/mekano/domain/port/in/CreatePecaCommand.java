package com.fiap.mekano.domain.port.in;

public record CreatePecaCommand(
    String descricao,
    Integer saldo,
    Integer estoqueMinimo
) {}
