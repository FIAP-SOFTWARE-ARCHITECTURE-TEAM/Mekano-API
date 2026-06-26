package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
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
class PecaServiceTest {

    @Mock
    PecaRepositoryPort pecaRepository;

    @InjectMocks
    PecaService pecaService;

    private CreatePecaCommand command;
    private Peca pecaMock;

    @BeforeEach
    void setUp() {
        command = new CreatePecaCommand("Peca A", 100, 10);
        pecaMock = new Peca("Peca A", 100, 10);
    }

    @Test
    void deveCriarPecaComSucesso() {
        when(pecaRepository.salvar(any(Peca.class))).thenReturn(pecaMock);

        var response = pecaService.criar(command);

        assertThat(response).isNotNull();
        assertThat(response.descricao()).isEqualTo("Peca A");
        assertThat(response.saldo()).isEqualTo(100);
    }
}
