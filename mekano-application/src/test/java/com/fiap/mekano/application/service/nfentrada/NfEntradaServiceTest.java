package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfEntradaServiceTest {

    @Mock
    NfEntradaRepositoryPort nfRepository;

    @Mock
    PecaRepositoryPort pecaRepository;

    @Mock
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @InjectMocks
    NfEntradaService nfService;

    private CreateNfEntradaCommand command;
    private UUID pecaId;
    private UUID requisicaoId;
    private NfEntrada nfMock;
    private Peca pecaMock;
    private RequisicaoCompra requisicaoMock;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        requisicaoId = UUID.randomUUID();
        command = new CreateNfEntradaCommand(pecaId, requisicaoId, 25);

        pecaMock = new Peca("Peca A", 100, 10);
        requisicaoMock = new RequisicaoCompra(pecaId, 25, StatusRequisicao.COMPRADA);
        nfMock = new NfEntrada(pecaId, requisicaoId, 25, LocalDateTime.now());
    }

    @Test
    void deveRegistrarNfComSucesso() {
        when(nfRepository.salvar(any(NfEntrada.class))).thenReturn(nfMock);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(pecaMock));
        when(requisicaoRepository.buscarPorId(requisicaoId)).thenReturn(Optional.of(requisicaoMock));
        when(pecaRepository.salvar(any(Peca.class))).thenReturn(pecaMock);
        when(requisicaoRepository.atualizar(any(RequisicaoCompra.class))).thenReturn(requisicaoMock);

        var response = nfService.registrar(command);

        assertThat(response).isNotNull();
        assertThat(response.quantidade()).isEqualTo(25);
    }
}
