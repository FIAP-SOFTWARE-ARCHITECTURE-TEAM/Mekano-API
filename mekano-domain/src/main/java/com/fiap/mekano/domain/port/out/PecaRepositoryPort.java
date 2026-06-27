package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Peca;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepositoryPort {
    Peca salvar(Peca peca);
    Optional<Peca> buscarPorId(UUID id);
    Optional<Peca> buscarPorDescricao(String descricao);
}
