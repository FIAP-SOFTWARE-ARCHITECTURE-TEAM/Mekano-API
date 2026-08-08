package com.fiap.mekano.infrastructure.job;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlaExpiryJob")
class SlaExpiryJobTest {

    @Mock
    OrcamentoRepositoryPort orcamentoRepository;

    @Mock
    OrdemDeServicoRepositoryPort ordemDeServicoRepository;

    @Mock
    OsAuditEventPublisher osAuditEventPublisher;

    @Mock
    PecaRepositoryPort pecaRepository;

    @InjectMocks
    SlaExpiryJob job;

    @Captor
    ArgumentCaptor<Map<String, Object>> metadataCaptor;

    private Orcamento criarOrcamentoComOS(UUID osUuid, List<ItemOrcamento> itens) {
        return Orcamento.create("Orçamento teste", itens, osUuid);
    }

    private OrdemDeServico criarOSNoStatus(StatusOS status) {
        OrdemDeServico os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        if (status == StatusOS.RECEBIDA) return os;

        os.iniciarDiagnostico(); // → EM_DIAGNOSTICO
        if (status == StatusOS.EM_DIAGNOSTICO) return os;

        os.finalizarDiagnostico(); // → AGUARDANDO_APROVACAO
        if (status == StatusOS.AGUARDANDO_APROVACAO) return os;

        os.aprovarOrcamento(UUID.randomUUID()); // → AGUARDANDO_EXECUCAO
        if (status == StatusOS.AGUARDANDO_EXECUCAO) return os;

        os.iniciarExecucao(UUID.randomUUID(), null); // → EM_EXECUCAO
        if (status == StatusOS.EM_EXECUCAO) return os;

        os.finalizarExecucao(null); // → FINALIZADA
        return os;
    }

    @Test
    @DisplayName("orcamento expirado com OS AGUARDANDO_APROVACAO deve cancelar e auditar")
    void deveCancelarOSQuandoOrcamentoExpirado() {
        UUID osUuid = UUID.randomUUID();
        OrdemDeServico os = criarOSNoStatus(StatusOS.AGUARDANDO_APROVACAO);
        Orcamento orcamento = criarOrcamentoComOS(osUuid, List.of(
                new ItemOrcamento("Peça X", 1L, BigDecimal.TEN)));

        when(orcamentoRepository.findExpiradosPendentes()).thenReturn(List.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        job.expirarOrcamentosVencidos();

        verify(ordemDeServicoRepository).save(argThat(savedOs -> savedOs.getStatus() == StatusOS.CANCELADA));
        verify(osAuditEventPublisher).publish(osUuid, OsAuditAction.CANCELAR, "sistema",
                "Cancelamento automático por SLA (72h)", Map.of());
    }

    @Test
    @DisplayName("orcamento expirado com OS EM_EXECUCAO não deve cancelar OS")
    void naoDeveCancelarOSEstandoEmExecucao() {
        UUID osUuid = UUID.randomUUID();
        OrdemDeServico os = criarOSNoStatus(StatusOS.EM_EXECUCAO);
        Orcamento orcamento = criarOrcamentoComOS(osUuid, List.of(
                new ItemOrcamento("Serviço Y", 1L, BigDecimal.TEN)));

        when(orcamentoRepository.findExpiradosPendentes()).thenReturn(List.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        job.expirarOrcamentosVencidos();

        verify(ordemDeServicoRepository, never()).save(argThat(
                savedOs -> savedOs.getStatus() == StatusOS.CANCELADA));
        verify(osAuditEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("orcamento sem ordemServicoUuid não deve chamar ordemDeServicoRepository")
    void orcamentoSemOSNaoChamaOsRepo() {
        Orcamento orcamento = Orcamento.create("Orçamento sem OS",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        when(orcamentoRepository.findExpiradosPendentes()).thenReturn(List.of(orcamento));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        job.expirarOrcamentosVencidos();

        verify(ordemDeServicoRepository, never()).findById(any());
        verify(osAuditEventPublisher, never()).publish(any(), any(), any(), any(), any());
        verify(pecaRepository, never()).liberarReserva(any(), anyInt());
    }

    @Test
    @DisplayName("OS cancelada com itens de peça deve liberar reserva por item")
    void deveLiberarReservaParaCadaItemPeca() {
        UUID osUuid = UUID.randomUUID();
        UUID pecaId1 = UUID.randomUUID();
        UUID pecaId2 = UUID.randomUUID();
        OrdemDeServico os = criarOSNoStatus(StatusOS.AGUARDANDO_APROVACAO);

        Orcamento orcamento = criarOrcamentoComOS(osUuid, List.of(
                new ItemOrcamento("Peça A", 3L, BigDecimal.TEN, pecaId1),
                new ItemOrcamento("Peça B", 2L, BigDecimal.valueOf(25), pecaId2),
                new ItemOrcamento("Serviço", 1L, BigDecimal.valueOf(50)) // sem pecaId
        ));

        when(orcamentoRepository.findExpiradosPendentes()).thenReturn(List.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        job.expirarOrcamentosVencidos();

        verify(pecaRepository).liberarReserva(pecaId1, 3);
        verify(pecaRepository).liberarReserva(pecaId2, 2);
        verify(pecaRepository, never()).liberarReserva(null, 1);
    }
}