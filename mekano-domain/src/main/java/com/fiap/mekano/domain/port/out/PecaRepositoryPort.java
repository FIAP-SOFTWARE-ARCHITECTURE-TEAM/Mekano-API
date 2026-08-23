package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Peca;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepositoryPort {
    Peca salvar(Peca peca);
    Optional<Peca> buscarPorId(UUID id);
    Optional<Peca> buscarPorDescricao(String descricao);
    /**
     * Lista peças paginadas, opcionalmente filtradas por status.
     *
     * @param isActive quando {@code null} retorna todas; {@code true} só ativas; {@code false} só inativas
     */
    List<Peca> findAll(int page, int size, Boolean isActive);
    long countAll(Boolean isActive);
    List<Peca> listarAbaixoEstoqueMinimo();

    boolean debitarSaldo(UUID pecaId, Integer quantidade);
    void creditarSaldo(UUID pecaId, Integer quantidade);

    boolean reservarSaldo(UUID pecaId, Integer quantidade);
    boolean debitarSaldoReservado(UUID pecaId, Integer quantidade);
    boolean liberarReserva(UUID pecaId, Integer quantidade);

    void remover(UUID id);
    void reativar(UUID id);
}
