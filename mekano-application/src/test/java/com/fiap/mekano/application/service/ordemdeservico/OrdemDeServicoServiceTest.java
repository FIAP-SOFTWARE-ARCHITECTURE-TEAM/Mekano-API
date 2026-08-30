package com.fiap.mekano.application.service.ordemdeservico;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.ItemOs;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.in.CreateItemOsCommand;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.ItemOsRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    OsAuditEventPublisher osAuditEventPublisher;

    @Mock
    ClienteRepositoryPort clienteRepository;

    @Mock
    VeiculoRepositoryPort veiculoRepository;

    @Mock
    ItemOsRepositoryPort itemOsRepository;

    @InjectMocks
    OrdemDeServicoService service;

    private UUID osId;
    private OrdemDeServico os;
    private Cliente clienteAtivo;
    private Veiculo veiculoAtivo;

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
        // Simular cliente e veiculo existentes e ativos para validação no create/update
        clienteAtivo = mock(Cliente.class);
        when(clienteAtivo.getIsActive()).thenReturn(true);
        veiculoAtivo = mock(Veiculo.class);
        when(veiculoAtivo.getIsActive()).thenReturn(true);
        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(any(UUID.class))).thenReturn(Optional.of(veiculoAtivo));
        when(itemOsRepository.findByOsUuid(any(UUID.class))).thenReturn(List.of());
    }

    @Test
    @DisplayName("finalizarDiagnostico item PECA deve gerar ItemOrcamento com pecaId")
    void finalizarDiagnosticoItemPecaGeraPecaId() {
        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.reconstitute(pecaId, "PEA-001", "Óleo Motor 5W30",
                new BigDecimal("45.50"), 50L, 10L, LocalDateTime.now(), 0L);

        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        var itemPersisted = ItemOs.create(os.getId(), pecaId, "PECA", "Troca de óleo", 2L);
        when(itemOsRepository.findByOsUuid(os.getId())).thenReturn(List.of(itemPersisted));

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
        var itemPersisted = ItemOs.create(os.getId(), servicoId, "SERVICO", "Troca de óleo", 1L);
        when(itemOsRepository.findByOsUuid(os.getId())).thenReturn(List.of(itemPersisted));

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

    @Test
    @DisplayName("iniciarExecucao deve debitar reserva dos itens de peça do orçamento")
    void iniciarExecucaoDebitaReserva() {
        UUID pecaId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();

        OrdemDeServico osExec = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osExec.iniciarDiagnostico();
        osExec.finalizarDiagnostico();
        // Simular aprovação de orçamento que define orcamentoUuid
        osExec.aprovarOrcamento(orcamentoUuid);
        assertEquals(StatusOS.AGUARDANDO_EXECUCAO, osExec.getStatus());

        var orcamento = Orcamento.create("Orçamento",
                List.of(
                        new ItemOrcamento("Peça A", 2L, BigDecimal.TEN, pecaId),
                        new ItemOrcamento("Serviço B", 1L, BigDecimal.valueOf(50))
                ));
        orcamento.aprovar();

        when(repository.findById(osId)).thenReturn(Optional.of(osExec));
        when(pecaRepository.debitarSaldoReservado(pecaId, 2)).thenReturn(true);
        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(orcamento));

        service.iniciarExecucao(osId, UUID.randomUUID(), "Iniciando execução");

        verify(pecaRepository).debitarSaldoReservado(pecaId, 2);
    }

    @Test
    @DisplayName("iniciarExecucao com reserva insuficiente deve lançar AppException 409 e save nunca chamado")
    void iniciarExecucaoReservaInsuficienteLanca409() {
        UUID pecaId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();

        OrdemDeServico osExec = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osExec.iniciarDiagnostico();
        osExec.finalizarDiagnostico();
        osExec.aprovarOrcamento(orcamentoUuid);

        var orcamento = Orcamento.create("Orçamento",
                List.of(new ItemOrcamento("Peça A", 5L, BigDecimal.TEN, pecaId)));
        orcamento.aprovar();

        Peca peca = Peca.reconstitute(
                pecaId, "PEA-001", "Peça A", BigDecimal.TEN,
                0L, 5L, LocalDateTime.now(), 0L
        );

        when(repository.findById(osId)).thenReturn(Optional.of(osExec));
        when(pecaRepository.debitarSaldoReservado(pecaId, 5)).thenReturn(false);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(orcamento));

        var ex = assertThrows(AppException.class,
                () -> service.iniciarExecucao(osId, UUID.randomUUID(), "Iniciando"));
        assertEquals(409, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar deve liberar reserva e nunca creditar saldo")
    void cancelarLiberaReservaNaoCredita() {
        UUID pecaId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();

        OrdemDeServico osCancel = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osCancel.iniciarDiagnostico();
        osCancel.finalizarDiagnostico();
        osCancel.aprovarOrcamento(orcamentoUuid);

        var orcamento = Orcamento.create("Orçamento",
                List.of(new ItemOrcamento("Peça A", 3L, BigDecimal.TEN, pecaId)));
        orcamento.aprovar();

        when(repository.findById(osId)).thenReturn(Optional.of(osCancel));
        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(orcamento));

        service.cancelar(osId, "Cliente desistiu");

        verify(pecaRepository).liberarReserva(pecaId, 3);
        verify(pecaRepository, never()).creditarSaldo(any(), any());
        verify(eventPublisher).publish(any(com.fiap.mekano.domain.event.OSCanceladaEvent.class));
    }

    @Test
    @DisplayName("cancelar sem orçamento não deve chamar repo de peça")
    void cancelarSemOrcamentoNaoChamaPecaRepo() {
        OrdemDeServico osSemOrc = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osSemOrc.iniciarDiagnostico();
        osSemOrc.finalizarDiagnostico();

        when(repository.findById(osId)).thenReturn(Optional.of(osSemOrc));

        service.cancelar(osId, "Desistiu");

        verify(pecaRepository, never()).liberarReserva(any(), anyInt());
    }

    // ─────────────── Testes de auditoria (D-11) ───────────────

    @Test
    @DisplayName("create deve auditar CRIAR")
    void createAuditaCRIAR() {
        var cmd = new CreateOrdemDeServicoCommand(
                UUID.randomUUID(), UUID.randomUUID(), "Teste", List.of());
        var novaOs = OrdemDeServico.create(cmd.clienteId(), cmd.veiculoId(),
                cmd.descricaoProblema());
        when(repository.save(any())).thenReturn(novaOs);

        service.create(cmd);

        verify(osAuditEventPublisher).publish(novaOs.getId(), OsAuditAction.CRIAR, null,
                OsAuditAction.CRIAR.getObservacaoDefault(), Map.of());
    }

    // ─────────────── Testes de validação OS-07 (create) ───────────────

    @Test
    @DisplayName("create com cliente inexistente deve lançar AppException 404 e save nunca chamado")
    void createComClienteInexistenteLanca404() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor", List.of());

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(404, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create com veiculo inexistente deve lançar AppException 404 e save nunca chamado")
    void createComVeiculoInexistenteLanca404() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.empty());

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor", List.of());

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(404, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create com cliente e veiculo existentes deve chamar save")
    void createComClienteEVeiculoExistentesChamaSave() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor", List.of());
        var novaOs = OrdemDeServico.create(clienteId, veiculoId, "Problema no motor");
        when(repository.save(any())).thenReturn(novaOs);

        service.create(cmd);

        verify(repository).save(any());
    }

    @Test
    @DisplayName("create com itens de peca deve chamar itemOsRepository.save para cada item")
    void createComItensPecaChamaItemOsSave() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));
        var pecaAtiva = mock(Peca.class);
        when(pecaAtiva.getIsActive()).thenReturn(true);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(pecaAtiva));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor",
                List.of(new CreateItemOsCommand(pecaId, "PECA", 2L)));
        var novaOs = OrdemDeServico.create(clienteId, veiculoId, "Problema no motor");
        when(repository.save(any())).thenReturn(novaOs);

        service.create(cmd);

        verify(itemOsRepository).save(any());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("create com itens de servico deve chamar itemOsRepository.save para cada item")
    void createComItensServicoChamaItemOsSave() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));
        var servicoAtivo = mock(Servico.class);
        when(servicoAtivo.getIsActive()).thenReturn(true);
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servicoAtivo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor",
                List.of(new CreateItemOsCommand(servicoId, "SERVICO", 1L)));
        var novaOs = OrdemDeServico.create(clienteId, veiculoId, "Problema no motor");
        when(repository.save(any())).thenReturn(novaOs);

        service.create(cmd);

        verify(itemOsRepository).save(any());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("create com item de peca inexistente deve lançar AppException 404")
    void createComItemPecaInexistenteLanca404() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.empty());

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor",
                List.of(new CreateItemOsCommand(pecaId, "PECA", 1L)));

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(404, ex.getStatus());
        verify(itemOsRepository, never()).save(any());
    }

    @Test
    @DisplayName("create com item de servico inexistente deve lançar AppException 404")
    void createComItemServicoInexistenteLanca404() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor",
                List.of(new CreateItemOsCommand(servicoId, "SERVICO", 1L)));

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(404, ex.getStatus());
        verify(itemOsRepository, never()).save(any());
    }

    @Test
    @DisplayName("create com item de peca inativo deve lançar AppException 422")
    void createComItemPecaInativoLanca422() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));
        var pecaInativa = mock(Peca.class);
        when(pecaInativa.getIsActive()).thenReturn(false);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(pecaInativa));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor",
                List.of(new CreateItemOsCommand(pecaId, "PECA", 1L)));

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(422, ex.getStatus());
        verify(itemOsRepository, never()).save(any());
    }

    @Test
    @DisplayName("create com item de servico inativo deve lançar AppException 422")
    void createComItemServicoInativoLanca422() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));
        var servicoInativo = mock(Servico.class);
        when(servicoInativo.getIsActive()).thenReturn(false);
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servicoInativo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor",
                List.of(new CreateItemOsCommand(servicoId, "SERVICO", 1L)));

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(422, ex.getStatus());
        verify(itemOsRepository, never()).save(any());
    }

    @Test
    @DisplayName("create sem itens deve salvar OS sem chamar itemOsRepository")
    void createSemItensNaoChamaItemOsRepository() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteAtivo));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoAtivo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor", List.of());
        var novaOs = OrdemDeServico.create(clienteId, veiculoId, "Problema no motor");
        when(repository.save(any())).thenReturn(novaOs);

        service.create(cmd);

        verify(repository).save(any());
        verify(itemOsRepository, never()).save(any());
    }

    @Test
    @DisplayName("create com cliente inativo deve lançar AppException 422 e save nunca chamado")
    void createComClienteInativoLanca422() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        var clienteInativo = mock(Cliente.class);
        when(clienteInativo.getIsActive()).thenReturn(false);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteInativo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor", List.of());

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(422, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create com veiculo inativo deve lançar AppException 422 e save nunca chamado")
    void createComVeiculoInativoLanca422() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        var veiculoInativo = mock(Veiculo.class);
        when(veiculoInativo.getIsActive()).thenReturn(false);
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoInativo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Problema no motor", List.of());

        var ex = assertThrows(AppException.class, () -> service.create(cmd));
        assertEquals(422, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update com cliente inativo deve lançar AppException 422 e save nunca chamado")
    void updateComClienteInativoLanca422() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        var clienteInativo = mock(Cliente.class);
        when(clienteInativo.getIsActive()).thenReturn(false);
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteInativo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Novo problema", List.of());

        var ex = assertThrows(AppException.class, () -> service.update(osId, cmd));
        assertEquals(422, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update com veiculo inativo deve lançar AppException 422 e save nunca chamado")
    void updateComVeiculoInativoLanca422() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        var veiculoInativo = mock(Veiculo.class);
        when(veiculoInativo.getIsActive()).thenReturn(false);
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculoInativo));

        var cmd = new CreateOrdemDeServicoCommand(clienteId, veiculoId, "Novo problema", List.of());

        var ex = assertThrows(AppException.class, () -> service.update(osId, cmd));
        assertEquals(422, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("finalizarDiagnostico com peça inativa deve lançar AppException 422 e evento nunca publicado")
    void finalizarDiagnosticoComPecaInativaLanca422() {
        UUID pecaId = UUID.randomUUID();
        Peca pecaInativa = Peca.reconstitute(pecaId, "PEA-001", "Óleo Motor 5W30",
                new BigDecimal("45.50"), 50L, 10L, LocalDateTime.now(), 0L, false);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(pecaInativa));

        var command = new FinalizarDiagnosticoCommand(
                osId, "Troca de óleo",
                List.of(new FinalizarDiagnosticoCommand.ItemDiagnostico(pecaId, "PECA", 2L)));

        var ex = assertThrows(AppException.class, () -> service.finalizarDiagnostico(command));
        assertEquals(422, ex.getStatus());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("finalizarDiagnostico com serviço inativo deve lançar AppException 422 e evento nunca publicado")
    void finalizarDiagnosticoComServicoInativoLanca422() {
        UUID servicoId = UUID.randomUUID();
        Servico servicoInativo = Servico.reconstitute(servicoId, "Troca de Óleo", "Troca com óleo sintético",
                new BigDecimal("89.90"), LocalDateTime.now(), false);
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servicoInativo));

        var command = new FinalizarDiagnosticoCommand(
                osId, "Troca de óleo",
                List.of(new FinalizarDiagnosticoCommand.ItemDiagnostico(servicoId, "SERVICO", 1L)));

        var ex = assertThrows(AppException.class, () -> service.finalizarDiagnostico(command));
        assertEquals(422, ex.getStatus());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("iniciarDiagnostico deve auditar DIAGNOSTICAR")
    void iniciarDiagnosticoAuditaDIAGNOSTICAR() {
        // OS precisa estar em RECEBIDA
        OrdemDeServico osNova = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Teste");
        UUID novoId = UUID.randomUUID();
        when(repository.findById(novoId)).thenReturn(Optional.of(osNova));

        service.iniciarDiagnostico(novoId);

        verify(osAuditEventPublisher).publish(any(), eq(OsAuditAction.DIAGNOSTICAR), isNull(),
                eq(OsAuditAction.DIAGNOSTICAR.getObservacaoDefault()), eq(Map.of()));
    }

    @Test
    @DisplayName("finalizarDiagnostico deve auditar ORCAR com metadata itens.size")
    void finalizarDiagnosticoAuditaORCAR() {
        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.reconstitute(pecaId, "PEA-001", "Óleo Motor 5W30",
                new BigDecimal("45.50"), 50L, 10L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        var itemPersisted = ItemOs.create(os.getId(), pecaId, "PECA", "Troca de óleo", 2L);
        when(itemOsRepository.findByOsUuid(os.getId())).thenReturn(List.of(itemPersisted));

        var command = new FinalizarDiagnosticoCommand(
                osId, "Troca de óleo",
                List.of(new FinalizarDiagnosticoCommand.ItemDiagnostico(pecaId, "PECA", 2L)));

        service.finalizarDiagnostico(command);

        verify(osAuditEventPublisher).publish(any(), eq(OsAuditAction.ORCAR), isNull(),
                eq(OsAuditAction.ORCAR.getObservacaoDefault()), eq(Map.of("itens", 1)));
    }

    @Test
    @DisplayName("iniciarExecucao deve auditar EXECUTAR com metadata mecanico")
    void iniciarExecucaoAuditaEXECUTAR() {
        UUID mecanicoId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        UUID pecaId = UUID.randomUUID();

        OrdemDeServico osExec = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osExec.iniciarDiagnostico();
        osExec.finalizarDiagnostico();
        osExec.aprovarOrcamento(orcamentoUuid);

        var orcamento = Orcamento.create("Orçamento",
                List.of(new ItemOrcamento("Peça A", 2L, BigDecimal.TEN, pecaId)));
        orcamento.aprovar();

        when(repository.findById(osId)).thenReturn(Optional.of(osExec));
        when(pecaRepository.debitarSaldoReservado(pecaId, 2)).thenReturn(true);
        when(orcamentoRepository.findByUuid(orcamentoUuid)).thenReturn(Optional.of(orcamento));

        service.iniciarExecucao(osId, mecanicoId, null);

        verify(osAuditEventPublisher).publish(any(), eq(OsAuditAction.EXECUTAR), isNull(),
                eq(OsAuditAction.EXECUTAR.getObservacaoDefault()), eq(Map.of("mecanico", mecanicoId.toString())));
    }

    @Test
    @DisplayName("finalizarExecucao deve auditar FINALIZAR")
    void finalizarExecucaoAuditaFINALIZAR() {
        OrdemDeServico osEmExec = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osEmExec.iniciarDiagnostico();
        osEmExec.finalizarDiagnostico();
        osEmExec.aprovarOrcamento(UUID.randomUUID());
        osEmExec.iniciarExecucao(UUID.randomUUID(), null);

        when(repository.findById(osId)).thenReturn(Optional.of(osEmExec));

        service.finalizarExecucao(osId, "Finalizado");

        verify(osAuditEventPublisher).publish(any(), eq(OsAuditAction.FINALIZAR), isNull(),
                eq(OsAuditAction.FINALIZAR.getObservacaoDefault()), eq(Map.of()));
    }

    @Test
    @DisplayName("cancelar deve auditar CANCELAR com motivo como observacao")
    void cancelarAuditaCANCELAR() {
        OrdemDeServico osCancel = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osCancel.iniciarDiagnostico();
        osCancel.finalizarDiagnostico();

        when(repository.findById(osCancel.getId())).thenReturn(Optional.of(osCancel));

        service.cancelar(osCancel.getId(), "Cliente desistiu");

        verify(osAuditEventPublisher).publish(any(), eq(OsAuditAction.CANCELAR), isNull(),
                eq("Cliente desistiu"), eq(Map.of()));
        verify(eventPublisher).publish(any(com.fiap.mekano.domain.event.OSCanceladaEvent.class));
    }

    @Test
    @DisplayName("entregar deve auditar ENTREGAR com recebidoPor como observacao")
    void entregarAuditaENTREGAR() {
        OrdemDeServico osEntregue = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        osEntregue.iniciarDiagnostico();
        osEntregue.finalizarDiagnostico();
        osEntregue.aprovarOrcamento(UUID.randomUUID());
        osEntregue.iniciarExecucao(UUID.randomUUID(), null);
        osEntregue.finalizarExecucao(null);
        osEntregue.gerarCobranca();
        osEntregue.confirmarPagamento("ref-123");

        when(repository.findById(osEntregue.getId())).thenReturn(Optional.of(osEntregue));

        service.entregar(osEntregue.getId(), "João");

        verify(osAuditEventPublisher).publish(any(), eq(OsAuditAction.ENTREGAR), isNull(),
                eq("João"), eq(Map.of()));
    }
}