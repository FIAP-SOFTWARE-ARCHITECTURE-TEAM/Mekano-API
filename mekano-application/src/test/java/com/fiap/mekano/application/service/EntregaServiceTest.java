package com.fiap.mekano.application.service;

import com.fiap.mekano.domain.event.OSEntregueEvent;
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

import java.math.BigDecimal;
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

        var ex = assertThrows(AppException.class, () -> entregaService.registrarEntrega(osUuid, null));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("registrarEntrega deve bloquear se OS não está FINALIZADA")
    void registrarEntrega_bloqueiaSeNaoFinalizada() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.emitirCobranca(new BigDecimal("100"));
        os.confirmarPagamento();

        when(ordemDeServicoRepository.findById(os.getId())).thenReturn(Optional.of(os));

        var ex = assertThrows(AppException.class, () -> entregaService.registrarEntrega(os.getId(), null));
        assertEquals(422, ex.getStatus());
    }

    @Test
    @DisplayName("registrarEntrega deve transicionar para ENTREGUE se pago e finalizado")
    void registrarEntrega_sucessoSePagoEFinalizada() {
        var os = criarOSFinalizada();
        os.emitirCobranca(new BigDecimal("100"));
        os.confirmarPagamento();
        var osUuid = os.getId();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(ordemDeServicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        entregaService.registrarEntrega(osUuid, "Veículo entregue com sucesso");

        assertEquals(StatusOS.ENTREGUE, os.getStatus());
        assertEquals("Veículo entregue com sucesso", os.getObservacaoEntrega());
        verify(eventPublisher, times(1)).publish(any(OSEntregueEvent.class));
    }

    @Test
    @DisplayName("registrarEntrega deve lançar 404 se OS não existe")
    void registrarEntrega_lanca404SeOSNaoExiste() {
        var osUuid = UUID.randomUUID();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> entregaService.registrarEntrega(osUuid, null));
    }

    private static OrdemDeServico criarOSFinalizada() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.finalizar();
        return os;
    }
}