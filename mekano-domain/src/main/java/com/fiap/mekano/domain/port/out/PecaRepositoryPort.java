package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Peca;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepositoryPort {
    Peca salvar(Peca peca);
    Optional<Peca> buscarPorId(UUID id);
    Optional<Peca> buscarPorDescricao(String descricao);
    List<Peca> findAll(int page, int size);
    long countAll();
    List<Peca> listarAbaixoEstoqueMinimo();

    boolean debitarSaldo(UUID pecaId, Integer quantidade);
    void creditarSaldo(UUID pecaId, Integer quantidade);
}
