package com.fiap.mekano.infrastructure.whatsapp;

import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.infrastructure.whatsapp.dto.SendTextRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("EvolutionApiNotifier")
class EvolutionApiNotifierTest {

    private static final String INSTANCE = "mekano";
    private static final String API_KEY = "test-key";

    private final EvolutionApiRestClient restClient = mock(EvolutionApiRestClient.class);
    private final EvolutionApiNotifier notifier = new EvolutionApiNotifier(restClient, API_KEY, INSTANCE);

    @Test
    @DisplayName("notificarOrcamento deve chamar sendText com veículo, valor e telefone 55+")
    void notificarOrcamento_chamaSendTextComTextoEPrefixo55() {
        notifier.notificarOrcamento("11999999999", "João", "Fiat", "Uno", "ABC1D23", BigDecimal.valueOf(150),
                List.of(new ItemOrcamento("Troca de óleo", 1L, BigDecimal.valueOf(100)),
                        new ItemOrcamento("Alinhamento", 1L, BigDecimal.valueOf(50))));

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());

        SendTextRequest request = captor.getValue();
        assertEquals("5511999999999", request.number());
        assertTrue(request.text().contains("João"));
        assertTrue(request.text().contains("Fiat"));
        assertTrue(request.text().contains("Uno"));
        assertTrue(request.text().contains("ABC1D23"));
        assertTrue(request.text().contains("150"));
        assertFalse(request.text().contains("Orçamento #"));
        assertTrue(request.text().contains("Peças e serviços do orçamento:\n- Troca de óleo\n- Alinhamento"));
        assertTrue(request.text().contains("Responda a esta mensagem com:\n1️⃣ Confirmar\n2️⃣ Recusar"));
    }

    @Test
    @DisplayName("notificarRespostaOrcamento deve enviar confirmação de aprovação")
    void notificarRespostaOrcamento_aprovado_enviaConfirmacao() {
        notifier.notificarRespostaOrcamento("11999999999", true);

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());

        SendTextRequest request = captor.getValue();
        assertEquals("5511999999999", request.number());
        assertTrue(request.text().contains("aprovado"));
        assertFalse(request.text().contains("Olá"));
    }

    @Test
    @DisplayName("notificarRespostaOrcamento deve enviar confirmação de reprovação")
    void notificarRespostaOrcamento_reprovado_enviaConfirmacao() {
        notifier.notificarRespostaOrcamento("11999999999", false);

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());

        SendTextRequest request = captor.getValue();
        assertEquals("5511999999999", request.number());
        assertTrue(request.text().contains("não aprovado"));
        assertFalse(request.text().contains("Olá"));
    }

    @Test
    @DisplayName("notificarRetirada deve chamar sendText com placa e número 55")
    void notificarRetirada_chamaSendText() {
        UUID osUuid = UUID.randomUUID();

        notifier.notificarRetirada("11988888888", "Maria", "ABC1D23", osUuid);

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());

        SendTextRequest request = captor.getValue();
        assertEquals("5511988888888", request.number());
        assertTrue(request.text().contains("ABC1D23"));
        assertTrue(request.text().contains(osUuid.toString()));
    }

    @Test
    @DisplayName("telefone com 13 dígitos (DDI 55 já incluído) não deve duplicar prefixo")
    void telefoneCom55_naoDuplicaPrefixo() {
        notifier.notificarRetirada("5511999999999", "João", "ABC1D23", UUID.randomUUID());

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());
        assertEquals("5511999999999", captor.getValue().number());
    }

    @Test
    @DisplayName("celular com DDD 55 (11 dígitos) deve receber prefixo 55 — não é DDI")
    void celularDDD55_deveReceberPrefixo55() {
        notifier.notificarRetirada("55991234567", "João", "ABC1D23", UUID.randomUUID());

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());
        assertEquals("5555991234567", captor.getValue().number());
    }

    @Test
    @DisplayName("fixo com DDD 55 (10 dígitos) deve receber prefixo 55 — não é DDI")
    void fixoDDD55_deveReceberPrefixo55() {
        notifier.notificarRetirada("5533344556", "Maria", "ABC1D23", UUID.randomUUID());

        ArgumentCaptor<SendTextRequest> captor = ArgumentCaptor.forClass(SendTextRequest.class);
        verify(restClient).sendText(eq(INSTANCE), eq(API_KEY), captor.capture());
        assertEquals("555533344556", captor.getValue().number());
    }
}
