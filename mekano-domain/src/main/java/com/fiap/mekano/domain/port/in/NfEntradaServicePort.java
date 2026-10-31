package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.NfEntrada;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de entrada do contexto de Notas Fiscais de Entrada.
 *
 * <p>Trabalha exclusivamente com tipos de domínio — o mapeamento para DTOs de
 * API é responsabilidade do adapter (resource).
 */
public interface NfEntradaServicePort {

    NfEntrada registrar(CreateNfEntradaCommand command);

    NfEntrada buscarPorId(UUID id);

    List<NfEntrada> findAll(int page, int size);

    long countAll();
}
