package com.fiap.mekano.application.service;

import com.fiap.mekano.domain.event.EntregaConfirmadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntregaService")
class EntregaServiceTest {

    @Mock OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    @Mock EventPublisher eventPublisher;

    @InjectMocks
    EntregaService entregaService;

    @Test
    @DisplayName("registrarEntrega deve bloquear se pagamento não está CONFIRMADO")
    void registrarEntrega_bloqueiaSeNaoPago() {
        var os = criarOSFinalizada();
        var osUuid = os.getId();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));

        var ex = assertThrows(AppException.class, () -> entregaService.registrarEntrega(osUuid, "João"));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("registrarEntrega deve transicionar para ENTREGUE se pago e finalizado")
    void registrarEntrega_sucessoSePagoEFinalizada() {
        var os = criarOSComCobrancaConfirmada();
        var osUuid = os.getId();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(ordemDeServicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        entregaService.registrarEntrega(osUuid, "João");

        assertEquals(StatusOS.ENTREGUE, os.getStatus());
        verify(eventPublisher, times(1)).publish(any(EntregaConfirmadaEvent.class));
    }

    @Test
    @DisplayName("registrarEntrega deve lançar 404 se OS não existe")
    void registrarEntrega_lanca404SeOSNaoExiste() {
        var osUuid = UUID.randomUUID();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> entregaService.registrarEntrega(osUuid, "João"));
    }

    private static OrdemDeServico criarOSFinalizada() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.finalizarExecucao(null);
        return os;
    }

    private static OrdemDeServico criarOSComCobrancaConfirmada() {
        var os = criarOSFinalizada();
        os.gerarCobranca();
        os.confirmarPagamento("PIX-123");
        return os;
    }
}