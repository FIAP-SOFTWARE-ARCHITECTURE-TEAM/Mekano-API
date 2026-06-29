package com.fiap.mekano.application.service;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.os.OrdemServico;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.os.OsStatus;
import com.fiap.mekano.domain.port.out.OrdemServicoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrdemServicoServiceTest {

    private OrdemServicoRepositoryPort ordemServicoRepository;
    private OsAuditEventPublisher auditEventPublisher;
    private OrdemServicoService service;

    @BeforeEach
    void setUp() {
        ordemServicoRepository = mock(OrdemServicoRepositoryPort.class);
        auditEventPublisher = mock(OsAuditEventPublisher.class);

        service = new OrdemServicoService();
        service.ordemServicoRepository = ordemServicoRepository;
        service.auditEventPublisher = auditEventPublisher;
    }

    @Test
    @DisplayName("Deve criar OS e auditar ação CRIAR")
    void deveCriarOsEAuditarAcaoCriar() {
        when(ordemServicoRepository.save(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico os = service.criar("atendente@mekano.com", null);

        assertNotNull(os.getUuid());
        assertEquals(OsStatus.RECEBIDA, os.getStatus());

        verify(ordemServicoRepository).save(os);
        verify(auditEventPublisher).publish(
                eq(os.getUuid()),
                eq(OsAuditAction.CRIAR),
                eq("atendente@mekano.com"),
                eq("OS criada"),
                argThat(metadata -> "N/A".equals(metadata.get("statusAnterior"))
                        && "ABERTA".equals(metadata.get("statusAtual")))
        );
    }

    @Test
    @DisplayName("Deve diagnosticar OS e auditar ABERTA -> DIAGNOSTICADA")
    void deveDiagnosticarOsEAuditar() {
        UUID osUuid = UUID.randomUUID();
        OrdemServico os = OrdemServico.restaurar(osUuid, OsStatus.RECEBIDA);

        when(ordemServicoRepository.findByUuid(osUuid)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.diagnosticar(osUuid, "mecanico@mekano.com", null);

        assertEquals(OsStatus.EM_DIAGNOSTICO, result.getStatus());

        verify(auditEventPublisher).publish(
                eq(osUuid),
                eq(OsAuditAction.DIAGNOSTICAR),
                eq("mecanico@mekano.com"),
                eq("OS diagnosticada"),
                argThat(metadata -> "ABERTA".equals(metadata.get("statusAnterior"))
                        && "DIAGNOSTICADA".equals(metadata.get("statusAtual")))
        );
    }

    @Test
    @DisplayName("Deve executar fluxo completo auditando cada transição")
    void deveExecutarFluxoCompletoAuditando() {
        UUID osUuid = UUID.randomUUID();
        OrdemServico os = OrdemServico.restaurar(osUuid, OsStatus.RECEBIDA);

        when(ordemServicoRepository.findByUuid(osUuid)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.diagnosticar(osUuid, "mecanico@mekano.com", "diag");
        service.orcar(osUuid, "atendente@mekano.com", "orc");
        service.aprovar(osUuid, "cliente@mekano.com", "apr");
        service.executar(osUuid, "mecanico@mekano.com", "exec");
        OrdemServico finalizada = service.finalizar(osUuid, "mecanico@mekano.com", "fim");

        assertEquals(OsStatus.FINALIZADA, finalizada.getStatus());

        verify(auditEventPublisher).publish(eq(osUuid), eq(OsAuditAction.DIAGNOSTICAR), anyString(), eq("diag"), anyMap());
        verify(auditEventPublisher).publish(eq(osUuid), eq(OsAuditAction.ORCAR), anyString(), eq("orc"), anyMap());
        verify(auditEventPublisher).publish(eq(osUuid), eq(OsAuditAction.APROVAR), anyString(), eq("apr"), anyMap());
        verify(auditEventPublisher).publish(eq(osUuid), eq(OsAuditAction.EXECUTAR), anyString(), eq("exec"), anyMap());
        verify(auditEventPublisher).publish(eq(osUuid), eq(OsAuditAction.FINALIZAR), anyString(), eq("fim"), anyMap());
        verify(ordemServicoRepository, times(5)).save(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve cancelar OS e auditar ação CANCELAR")
    void deveCancelarOsEAuditar() {
        UUID osUuid = UUID.randomUUID();
        OrdemServico os = OrdemServico.restaurar(osUuid, OsStatus.EM_EXECUCAO);

        when(ordemServicoRepository.findByUuid(osUuid)).thenReturn(Optional.of(os));
        when(ordemServicoRepository.save(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico result = service.cancelar(osUuid, "atendente@mekano.com", null);

        assertEquals(OsStatus.CANCELADA, result.getStatus());

        verify(auditEventPublisher).publish(
                eq(osUuid),
                eq(OsAuditAction.CANCELAR),
                eq("atendente@mekano.com"),
                eq("OS cancelada"),
                argThat(metadata -> "APROVADA".equals(metadata.get("statusAnterior"))
                        && "CANCELADA".equals(metadata.get("statusAtual")))
        );
    }

    @Test
    @DisplayName("Não deve salvar nem auditar quando OS não existe")
    void naoDeveSalvarNemAuditarQuandoOsNaoExiste() {
        UUID osUuid = UUID.randomUUID();

        when(ordemServicoRepository.findByUuid(osUuid)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.aprovar(osUuid, "cliente@mekano.com", "aprovar")
        );

        assertTrue(exception.getMessage().contains("OS não encontrada"));
        verify(ordemServicoRepository, never()).save(any());
        verifyNoInteractions(auditEventPublisher);
    }

    @Test
    @DisplayName("Não deve salvar nem auditar transição inválida")
    void naoDeveSalvarNemAuditarTransicaoInvalida() {
        UUID osUuid = UUID.randomUUID();
        OrdemServico os = OrdemServico.restaurar(osUuid, OsStatus.RECEBIDA);

        when(ordemServicoRepository.findByUuid(osUuid)).thenReturn(Optional.of(os));

        assertThrows(
                IllegalStateException.class,
                () -> service.aprovar(osUuid, "cliente@mekano.com", "aprovar")
        );

        verify(ordemServicoRepository, never()).save(any());
        verifyNoInteractions(auditEventPublisher);
    }
}
