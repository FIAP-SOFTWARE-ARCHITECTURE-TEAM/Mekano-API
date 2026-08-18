package com.fiap.mekano.application.service.whatsapp;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsAppOrcamentoRespostaService")
class WhatsAppOrcamentoRespostaServiceTest {

    @Mock
    ClienteRepositoryPort clienteRepository;

    @Mock
    OrdemDeServicoRepositoryPort osRepository;

    @Mock
    OrcamentoRepositoryPort orcamentoRepository;

    @Mock
    OrcamentoServicePort orcamentoService;

    @Mock
    WhatsAppNotifierPort notifier;

    @InjectMocks
    WhatsAppOrcamentoRespostaService service;

    private static final UUID CLIENTE_UUID = UUID.randomUUID();
    private static final UUID OS_UUID = UUID.randomUUID();
    private static final UUID ORCAMENTO_UUID = UUID.randomUUID();
    private static final String TELEFONE = "91984847811";
    private static final String TELEFONE_NORMALIZADO = "9184847811";

    private Cliente clienteComTelefone() {
        var cliente = mock(Cliente.class);
        lenient().when(cliente.getId()).thenReturn(CLIENTE_UUID);
        lenient().when(cliente.getNome()).thenReturn("Teste 91");
        var telefone = mock(com.fiap.mekano.domain.valueobject.Telefone.class);
        lenient().when(telefone.getValue()).thenReturn(TELEFONE);
        lenient().when(cliente.getTelefone()).thenReturn(telefone);
        return cliente;
    }

    private OrdemDeServico osAguardandoAprovacao() {
        var os = mock(OrdemDeServico.class);
        when(os.getId()).thenReturn(OS_UUID);
        return os;
    }

    private com.fiap.mekano.domain.model.Orcamento orcamentoComId() {
        var orcamento = mock(com.fiap.mekano.domain.model.Orcamento.class);
        when(orcamento.getId()).thenReturn(ORCAMENTO_UUID);
        return orcamento;
    }

    @Test
    @DisplayName("'sim' deve aprovar o orçamento pendente e confirmar ao cliente")
    void deveAprovarOrcamentoQuandoClienteRespondeSim() {
        Cliente cliente = clienteComTelefone();
        OrdemDeServico os = osAguardandoAprovacao();
        com.fiap.mekano.domain.model.Orcamento orcamento = orcamentoComId();
        when(clienteRepository.findByTelefone(TELEFONE_NORMALIZADO)).thenReturn(Optional.of(cliente));
        when(osRepository.findAllWithFilters("AGUARDANDO_APROVACAO", CLIENTE_UUID, null, null, null, 0, 1))
                .thenReturn(List.of(os));
        when(orcamentoRepository.findByOrdemServicoUuid(OS_UUID))
                .thenReturn(Optional.of(orcamento));

        boolean processado = service.processarResposta("559184847811@s.whatsapp.net", "sim");

        assertTrue(processado);
        var captor = ArgumentCaptor.forClass(AprovarOrcamentoCommand.class);
        verify(orcamentoService).aprovar(captor.capture());
        assertEquals(ORCAMENTO_UUID, captor.getValue().orcamentoUuid());
        verify(notifier).notificarRespostaOrcamento(TELEFONE, true);
        verify(orcamentoService, never()).reprovar(any());
    }

    @Test
    @DisplayName("'não' deve reprovar o orçamento pendente e confirmar ao cliente")
    void deveReprovarOrcamentoQuandoClienteRespondeNao() {
        Cliente cliente = clienteComTelefone();
        OrdemDeServico os = osAguardandoAprovacao();
        com.fiap.mekano.domain.model.Orcamento orcamento = orcamentoComId();
        when(clienteRepository.findByTelefone(TELEFONE_NORMALIZADO)).thenReturn(Optional.of(cliente));
        when(osRepository.findAllWithFilters("AGUARDANDO_APROVACAO", CLIENTE_UUID, null, null, null, 0, 1))
                .thenReturn(List.of(os));
        when(orcamentoRepository.findByOrdemServicoUuid(OS_UUID))
                .thenReturn(Optional.of(orcamento));

        boolean processado = service.processarResposta("559184847811@s.whatsapp.net", "NÃO");

        assertTrue(processado);
        var captor = ArgumentCaptor.forClass(ReprovarOrcamentoCommand.class);
        verify(orcamentoService).reprovar(captor.capture());
        assertEquals(ORCAMENTO_UUID, captor.getValue().orcamentoUuid());
        assertEquals("Reprovado via WhatsApp", captor.getValue().motivo());
        verify(notifier).notificarRespostaOrcamento(TELEFONE, false);
        verify(orcamentoService, never()).aprovar(any());
    }

    @Test
    @DisplayName("texto não reconhecido deve ser ignorado")
    void deveIgnorarTextoNaoReconhecido() {
        boolean processado = service.processarResposta("559184847811@s.whatsapp.net", "talvez amanhã");

        assertFalse(processado);
        verifyNoInteractions(clienteRepository);
        verifyNoInteractions(orcamentoService);
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("telefone sem cliente cadastrado deve ser ignorado")
    void deveIgnorarTelefoneSemCliente() {
        when(clienteRepository.findByTelefone(TELEFONE_NORMALIZADO)).thenReturn(Optional.empty());

        boolean processado = service.processarResposta("559184847811@s.whatsapp.net", "sim");

        assertFalse(processado);
        verifyNoInteractions(orcamentoService);
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("cliente sem OS aguardando aprovação deve ser ignorado")
    void deveIgnorarClienteSemOSAguardandoAprovacao() {
        Cliente cliente = clienteComTelefone();
        when(clienteRepository.findByTelefone(TELEFONE_NORMALIZADO)).thenReturn(Optional.of(cliente));
        when(osRepository.findAllWithFilters("AGUARDANDO_APROVACAO", CLIENTE_UUID, null, null, null, 0, 1))
                .thenReturn(List.of());

        boolean processado = service.processarResposta("559184847811@s.whatsapp.net", "sim");

        assertFalse(processado);
        verifyNoInteractions(orcamentoService);
        verifyNoInteractions(notifier);
    }
}