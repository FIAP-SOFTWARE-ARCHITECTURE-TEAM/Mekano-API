package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Peca;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de entrada do contexto de Peças.
 *
 * <p>Trabalha exclusivamente com tipos de domínio — o mapeamento para DTOs de
 * API é responsabilidade do adapter (resource).
 */
public interface PecaServicePort {

    Peca criar(CreatePecaCommand command);

    Peca updatePeca(UUID id, UpdatePecaCommand command);

    Peca buscarPorId(UUID id);

    boolean debitarSaldo(UUID pecaId, Integer quantidade);

    void creditarSaldo(UUID pecaId, Integer quantidade);

    boolean reservarSaldo(UUID pecaId, Integer quantidade);

    boolean debitarSaldoReservado(UUID pecaId, Integer quantidade);

    boolean liberarReserva(UUID pecaId, Integer quantidade);

    List<Peca> findAll(int page, int size, Boolean isActive);

    long countAll(Boolean isActive);

    List<Peca> listarAbaixoEstoqueMinimo();

    void excluir(UUID pecaId);

    void reativar(UUID pecaId);
}
