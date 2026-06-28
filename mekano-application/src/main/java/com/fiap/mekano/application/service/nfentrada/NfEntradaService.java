package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaService {
    @Inject NfEntradaRepositoryPort nfRepository;
    public CreateNfEntradaResponse registrar(com.fiap.mekano.domain.port.in.CreateNfEntradaCommand command) {
        return new CreateNfEntradaResponse(UUID.randomUUID(), 0, null);
    }
}
