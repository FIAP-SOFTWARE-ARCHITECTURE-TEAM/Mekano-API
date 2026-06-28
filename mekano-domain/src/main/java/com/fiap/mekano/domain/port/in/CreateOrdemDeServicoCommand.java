package com.fiap.mekano.domain.port.in;

import java.util.UUID;

/**
 * Comando de entrada para criação de uma Ordem de Serviço.
 */
public record CreateOrdemDeServicoCommand(UUID clienteId, UUID veiculoId, String descricaoProblema) {}
