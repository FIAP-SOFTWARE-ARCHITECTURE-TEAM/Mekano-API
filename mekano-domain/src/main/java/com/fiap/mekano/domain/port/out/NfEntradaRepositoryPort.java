package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.NfEntrada;
import java.util.Optional;
import java.util.UUID;

public interface NfEntradaRepositoryPort {
    NfEntrada salvar(NfEntrada nfEntrada);
    Optional<NfEntrada> buscarPorId(UUID id);
}
