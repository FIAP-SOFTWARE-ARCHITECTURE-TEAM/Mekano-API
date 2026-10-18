package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.RequisicaoCompra;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de entrada do contexto de Requisições de Compra.
 *
 * <p>Trabalha exclusivamente com tipos de domínio — o mapeamento para DTOs de
 * API é responsabilidade do adapter (resource).
 */
public interface RequisicaoCompraServicePort {

    RequisicaoCompra criar(CreateRequisicaoCompraCommand command);

    RequisicaoCompra buscarPorId(UUID id);

    void marcarComoComprada(UUID id);

    void marcarComoRecebida(UUID id);

    void cancelar(UUID id);

    void enviar(UUID id);

    List<RequisicaoCompra> findAll(int page, int size);

    long countAll();
}
