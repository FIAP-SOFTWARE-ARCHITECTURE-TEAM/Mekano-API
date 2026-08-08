package com.fiap.mekano.infrastructure.whatsapp;

import com.fiap.mekano.infrastructure.whatsapp.dto.SendMessageResponse;
import com.fiap.mekano.infrastructure.whatsapp.dto.SendTextRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class EvolutionApiNotifierTest {

    @InjectMock
    @RestClient
    EvolutionApiRestClient restClient;

    @Inject
    EvolutionApiNotifier notifier;

    @Test
    void notificarOrcamento_sendsButtonsWithCorrectPhonePrefix() {
        when(restClient.sendButtons(anyString(), anyString(), any()))
                .thenReturn(new SendMessageResponse(null, null, "success"));

        notifier.notificarOrcamento("11999999999", "João", UUID.randomUUID(), BigDecimal.valueOf(150));

        verify(restClient).sendButtons(eq("mekano"), anyString(), argThat(req ->
                req.number().equals("5511999999999")
        ));
    }

    @Test
    void notificarRetirada_sendsTextWithCorrectPhonePrefix() {
        when(restClient.sendText(anyString(), anyString(), any()))
                .thenReturn(new SendMessageResponse(null, null, "success"));

        notifier.notificarRetirada("11988888888", "Maria", "ABC1234", UUID.randomUUID());

        verify(restClient).sendText(eq("mekano"), anyString(), argThat(req ->
                req.number().equals("5511999999999")
        ));
    }
}