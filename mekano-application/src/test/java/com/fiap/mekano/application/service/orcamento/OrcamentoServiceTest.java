package com.fiap.mekano.application.service.orcamento;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.event.OrcamentoAprovadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("OrcamentoService")
class OrcamentoServiceTest {

    @Mock
    OrcamentoRepositoryPort orcamentoRepository;

    @Mock
    OrdemDeServicoRepositoryPort ordemDeServicoRepository;

    @Mock
    EventPublisher eventPublisher;

    @Mock
    OsAuditEventPublisher osAuditEventPublisher;

    @InjectMocks
    OrcamentoService orcamentoService;

    @Test
    @DisplayName("aprovar() deve publicar OrcamentoAprovadoEvent com itens de peça")
    void devePublicarEventoAprovacaoComItensPeca() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        var osUuid = os.getId();
        UUID pecaId = UUID.randomUUID();
        var orcamento = Orcamento.create("Teste",
                List.of(
                        new ItemOrcamento("Peça A", 2L, BigDecimal.TEN, pecaId),
                        new ItemOrcamento("Serviço B", 1L, BigDecimal.valueOf(50))
                ), osUuid);

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamento.getId()));

        ArgumentCaptor<OrcamentoAprovadoEvent> captor = ArgumentCaptor.forClass(OrcamentoAprovadoEvent.class);
        verify(eventPublisher).publish(captor.capture());

        OrcamentoAprovadoEvent evento = captor.getValue();
        assertEquals(orcamento.getId(), evento.orcamentoId());
        assertEquals(1, evento.itens().size());
        assertEquals(pecaId, evento.itens().get(0).pecaId());
        assertEquals(2, evento.itens().get(0).quantidade());
    }

    @Test
    @DisplayName("aprovar() não deve publicar evento quando orçamento não tem itens de peça")
    void naoDevePublicarEventoSeSemItensPeca() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        var osUuid = os.getId();
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Serviço", 1L, BigDecimal.TEN)), osUuid);

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamento.getId()));

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("aprovar() deve aprovar orçamento e transicionar OS para AGUARDANDO_EXECUCAO")
    void deveAprovarOrcamento() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        var osUuid = os.getId();
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)), osUuid);

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamento.getId()));

        assertEquals(StatusOrcamento.APROVADO, result.getStatus());
        assertEquals(StatusOS.AGUARDANDO_EXECUCAO, os.getStatus());
    }

    @Test
    @DisplayName("aprovar() deve lançar 422 se orçamento já está APROVADO")
    void deveLancar422SeOrcamentoJaAprovado() {
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)));
        orcamento.aprovar();

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));

        var ex = assertThrows(AppException.class,
                () -> orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamento.getId())));
        assertEquals(422, ex.getStatus());
    }

    @Test
    @DisplayName("reprovar() deve reprovar orçamento e transicionar OS para CANCELADA")
    void deveReprovarOrcamento() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        var osUuid = os.getId();
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)), osUuid);

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orcamentoService.reprovar(new ReprovarOrcamentoCommand(orcamento.getId(), "Cliente desistiu"));

        assertEquals(StatusOrcamento.REPROVADO, result.getStatus());
        assertEquals(StatusOS.CANCELADA, os.getStatus());
        assertEquals("Cliente desistiu", os.getMotivoCancelamento());
    }

    @Test
    @DisplayName("reprovar() deve lançar 422 se orçamento já está REPROVADO")
    void deveLancar422SeOrcamentoJaReprovado() {
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)));
        orcamento.reprovar();

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));

        var ex = assertThrows(AppException.class,
                () -> orcamentoService.reprovar(new ReprovarOrcamentoCommand(orcamento.getId(), "motivo")));
        assertEquals(422, ex.getStatus());
    }

    @Test
    @DisplayName("aprovar() deve lançar 404 se orçamento não existe")
    void deveLancar404SeOrcamentoNaoExiste() {
        var uuid = UUID.randomUUID();
        when(orcamentoRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> orcamentoService.aprovar(new AprovarOrcamentoCommand(uuid)));
    }

    @Test
    @DisplayName("reprovar() deve lançar 404 se orçamento não existe")
    void deveLancar404SeOrcamentoNaoExisteReprovar() {
        var uuid = UUID.randomUUID();
        when(orcamentoRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> orcamentoService.reprovar(new ReprovarOrcamentoCommand(uuid, "motivo")));
    }

    // ─────────────── Testes de auditoria (D-11) ───────────────

    @Test
    @DisplayName("aprovar() deve auditar APROVAR quando OS associada")
    void aprovarAuditaAPROVAR() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        var osUuid = os.getId();
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)), osUuid);

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamento.getId()));

        verify(osAuditEventPublisher).publish(osUuid, OsAuditAction.APROVAR, null,
                OsAuditAction.APROVAR.getObservacaoDefault(), Map.of());
    }

    @Test
    @DisplayName("aprovar() não deve auditar APROVAR quando OS não associada")
    void aprovarNaoAuditaSemOS() {
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

        orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamento.getId()));

        verify(osAuditEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reprovar() deve auditar CANCELAR com observação de reprovação")
    void reprovarAuditaCANCELAR() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        var osUuid = os.getId();
        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)), osUuid);

        when(orcamentoRepository.findByUuid(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        orcamentoService.reprovar(new ReprovarOrcamentoCommand(orcamento.getId(), "Cliente desistiu"));

        verify(osAuditEventPublisher).publish(osUuid, OsAuditAction.CANCELAR, null,
                "Orçamento reprovado pelo cliente", Map.of());
    }
}
