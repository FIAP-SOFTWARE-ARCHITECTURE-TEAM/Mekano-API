package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class PecaService {
    @Inject PecaRepositoryPort pecaRepository;
    public CreatePecaResponse criar(com.fiap.mekano.domain.port.in.CreatePecaCommand command) {
        return new CreatePecaResponse(UUID.randomUUID(), "TODO", 0, 0);
    }
}
