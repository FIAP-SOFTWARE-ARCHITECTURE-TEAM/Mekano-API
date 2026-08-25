package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.NfEntrada;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NfEntradaRepositoryPort {
    NfEntrada salvar(NfEntrada nfEntrada);
    Optional<NfEntrada> buscarPorId(UUID id);
    Optional<NfEntrada> buscarPorChaveAcesso(String chaveAcesso);
    List<NfEntrada> findAll(int page, int size);
    long countAll();
}
