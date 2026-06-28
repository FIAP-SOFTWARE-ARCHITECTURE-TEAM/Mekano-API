package com.fiap.mekano.application.service.orcamento;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.GerarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrcamentoService")
class OrcamentoServiceTest {

    @Mock
    OrcamentoRepositoryPort orcamentoRepository;

    @Mock
    OrdemDeServicoRepositoryPort ordemDeServicoRepository;

    @InjectMocks
    OrcamentoService orcamentoService;

    @Test
    @DisplayName("gerarOrcamento() deve criar orçamento e transicionar OS para AGUARDANDO_APROVACAO")
    void deveGerarOrcamento() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema no motor");
        os.iniciarDiagnostico();
        var osUuid = os.getId();

        var command = new GerarOrcamentoCommand(osUuid, "Orçamento",
                List.of(new ItemOrcamento("Serviço", 1L, new BigDecimal("100.00"))));

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordemDeServicoRepository.save(any(OrdemDeServico.class))).thenAnswer(inv -> inv.getArgument(0));

        Orcamento result = orcamentoService.gerarOrcamento(command);

        assertNotNull(result);
        assertEquals(StatusOrcamento.PENDENTE, result.getStatus());
        assertEquals(osUuid, result.getOrdemServicoUuid());
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
        verify(orcamentoRepository, times(1)).save(any(Orcamento.class));
        verify(ordemDeServicoRepository, times(1)).save(any(OrdemDeServico.class));
    }

    @Test
    @DisplayName("gerarOrcamento() deve lançar 404 se OS não existe")
    void deveLancar404SeOSNaoExiste() {
        var osUuid = UUID.randomUUID();
        var command = new GerarOrcamentoCommand(osUuid, "Orçamento",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> orcamentoService.gerarOrcamento(command));
    }

    @Test
    @DisplayName("gerarOrcamento() deve lançar 422 se OS não está EM_DIAGNOSTICO")
    void deveLancar422SeOSNaoEstaEmDiagnostico() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        var command = new GerarOrcamentoCommand(os.getId(), "Orçamento",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        when(ordemDeServicoRepository.findById(os.getId())).thenReturn(Optional.of(os));

        var ex = assertThrows(AppException.class, () -> orcamentoService.gerarOrcamento(command));
        assertEquals(422, ex.getStatus());
    }

    @Test
    @DisplayName("aprovar() deve aprovar orçamento e transicionar OS para EM_EXECUCAO")
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
        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
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
}
