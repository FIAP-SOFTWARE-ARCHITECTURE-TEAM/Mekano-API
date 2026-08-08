package com.fiap.mekano.application.service.ordemdeservico;

import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdemDeServicoService")
class OrdemDeServicoServiceTest {

    @Mock
    OrdemDeServicoRepositoryPort repository;

    @Mock
    EventPublisher eventPublisher;

    @Mock
    PecaRepositoryPort pecaRepository;

    @Mock
    ServicoRepositoryPort servicoRepository;

    @Mock
    OrcamentoRepositoryPort orcamentoRepository;

    @InjectMocks
    OrdemDeServicoService service;

    private UUID osId;
    private OrdemDeServico os;

    @Captor
    ArgumentCaptor<DiagnosticoFinalizadoEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        osId = UUID.randomUUID();
        os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema no motor");
        os.iniciarDiagnostico(); // AGUARDANDO_DIAGNOSTICO → EM_DIAGNOSTICO
        // Simular save retornando a mesma OS
        when(repository.findById(osId)).thenReturn(Optional.of(os));
        when(repository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("finalizarDiagnostico item PECA deve gerar ItemOrcamento com pecaId")
    void finalizarDiagnosticoItemPecaGeraPecaId() {
        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.reconstitute(pecaId, "PEA-001", "Óleo Motor 5W30",
                new BigDecimal("45.50"), 50L, 10L, LocalDateTime.now(), 0L);

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        var command = new FinalizarDiagnosticoCommand(
                osId, "Troca de óleo",
                List.of(new FinalizarDiagnosticoCommand.ItemDiagnostico(pecaId, "PECA", 2L)));

        service.finalizarDiagnostico(command);

        verify(eventPublisher).publish(eventCaptor.capture());
        DiagnosticoFinalizadoEvent event = eventCaptor.getValue();

        assertEquals(1, event.itens().size());
        ItemOrcamento item = event.itens().get(0);
        assertEquals(pecaId, item.getPecaId());
        assertEquals("Óleo Motor 5W30", item.getDescricao());
        assertEquals(2L, item.getQuantidade());
        assertEquals(new BigDecimal("45.50"), item.getValorUnitario());
    }

    @Test
    @DisplayName("finalizarDiagnostico item SERVICO deve gerar ItemOrcamento com pecaId null")
    void finalizarDiagnosticoItemServicoPecaIdNull() {
        UUID servicoId = UUID.randomUUID();
        Servico servico = Servico.create("Troca de Óleo", "Troca com óleo sintético", new BigDecimal("89.90"));

        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));

        var command = new FinalizarDiagnosticoCommand(
                osId, "Troca de óleo",
                List.of(new FinalizarDiagnosticoCommand.ItemDiagnostico(servicoId, "SERVICO", 1L)));

        service.finalizarDiagnostico(command);

        verify(eventPublisher).publish(eventCaptor.capture());
        DiagnosticoFinalizadoEvent event = eventCaptor.getValue();

        assertEquals(1, event.itens().size());
        ItemOrcamento item = event.itens().get(0);
        assertNull(item.getPecaId());
        assertEquals("Troca de Óleo", item.getDescricao());
    }
}