package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraService {
    @Inject RequisicaoCompraRepositoryPort requisicaoRepository;
    public CreateRequisicaoCompraResponse criar(com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand command) {
        return new CreateRequisicaoCompraResponse(UUID.randomUUID(), "TODO", 0);
    }
}
