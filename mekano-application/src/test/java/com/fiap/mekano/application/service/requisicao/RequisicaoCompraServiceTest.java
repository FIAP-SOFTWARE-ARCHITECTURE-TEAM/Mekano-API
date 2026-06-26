package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequisicaoCompraServiceTest {

    @Mock
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @InjectMocks
    RequisicaoCompraService requisicaoService;

    private CreateRequisicaoCompraCommand command;
    private RequisicaoCompra requisicaoMock;
    private UUID pecaId;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        command = new CreateRequisicaoCompraCommand(pecaId, 50);
        requisicaoMock = new RequisicaoCompra(pecaId, 50, StatusRequisicao.PENDENTE);
    }

    @Test
    void deveCriarRequisicaoComSucesso() {
        when(requisicaoRepository.salvar(any(RequisicaoCompra.class))).thenReturn(requisicaoMock);

        var response = requisicaoService.criar(command);

        assertThat(response).isNotNull();
        assertThat(response.quantidade()).isEqualTo(50);
        assertThat(response.status()).isEqualTo("PENDENTE");
    }
}
