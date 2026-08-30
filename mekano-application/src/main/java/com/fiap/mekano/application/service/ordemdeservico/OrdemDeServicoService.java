package com.fiap.mekano.application.service.ordemdeservico;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.application.service.os.OsAuditEventPublisher;
import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.event.OSCanceladaEvent;
import com.fiap.mekano.domain.event.OSFinalizadaEvent;
import com.fiap.mekano.domain.event.OrdemDeServicoCriadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.ItemOs;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.ItemOsRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Use case de OrdemDeServico — orquestra transições chamando métodos explícitos
 * da entidade. Nunca setStatus() (D-26).
 */
@ApplicationScoped
public class OrdemDeServicoService implements OrdemDeServicoServicePort {

private final OrdemDeServicoRepositoryPort repository;
    private final EventPublisher eventPublisher;
    private final PecaRepositoryPort pecaRepository;
    private final ServicoRepositoryPort servicoRepository;
    private final OrcamentoRepositoryPort orcamentoRepository;
    private final OsAuditEventPublisher osAuditEventPublisher;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;
    private final ItemOsRepositoryPort itemOsRepository;

    public OrdemDeServicoService(OrdemDeServicoRepositoryPort repository, EventPublisher eventPublisher,
                                  PecaRepositoryPort pecaRepository, ServicoRepositoryPort servicoRepository,
                                  OrcamentoRepositoryPort orcamentoRepository,
                                  OsAuditEventPublisher osAuditEventPublisher,
                                  ClienteRepositoryPort clienteRepository,
                                  VeiculoRepositoryPort veiculoRepository,
                                  ItemOsRepositoryPort itemOsRepository) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.pecaRepository = pecaRepository;
        this.servicoRepository = servicoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.osAuditEventPublisher = osAuditEventPublisher;
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.itemOsRepository = itemOsRepository;
    }

    @Override
    @Transactional
    public OrdemDeServico create(CreateOrdemDeServicoCommand command) {
        // OS-07: validar existência e atividade de clienteId e veiculoId antes de criar
        var cliente = clienteRepository.findById(command.clienteId())
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", command.clienteId())));
        if (!Boolean.TRUE.equals(cliente.getIsActive())) {
            throw new AppException(422, Messages.get("cliente.inactive", command.clienteId()));
        }
        var veiculo = veiculoRepository.findById(command.veiculoId())
                .orElseThrow(() -> new AppException(404, Messages.get("veiculo.not.found", command.veiculoId())));
        if (!Boolean.TRUE.equals(veiculo.getIsActive())) {
            throw new AppException(422, Messages.get("veiculo.inactive", command.veiculoId()));
        }

        OrdemDeServico os = OrdemDeServico.create(command.clienteId(), command.veiculoId(),
                command.descricaoProblema());
        OrdemDeServico saved = repository.save(os);

        // Persistir itens na junction table
        if (command.itens() != null) {
            for (var itemCmd : command.itens()) {
                ItemOs itemOs = ItemOs.create(saved.getId(), itemCmd.referenciaUuid(),
                        itemCmd.tipo(), command.descricaoProblema(), itemCmd.quantidade());
                validateAndPersistItem(itemOs);
                itemOsRepository.save(itemOs);
            }
        }

        eventPublisher.publish(OrdemDeServicoCriadaEvent.of(saved));
        osAuditEventPublisher.publish(saved.getId(), OsAuditAction.CRIAR, null,
                OsAuditAction.CRIAR.getObservacaoDefault(), Map.of());
        return saved;
    }

    @Override
    public OrdemDeServico findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", id)));
    }

    @Override
    @Transactional
    public OrdemDeServico update(UUID id, CreateOrdemDeServicoCommand command) {
        OrdemDeServico os = findById(id);
        var cliente = clienteRepository.findById(command.clienteId())
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", command.clienteId())));
        if (!Boolean.TRUE.equals(cliente.getIsActive())) {
            throw new AppException(422, Messages.get("cliente.inactive", command.clienteId()));
        }
        var veiculo = veiculoRepository.findById(command.veiculoId())
                .orElseThrow(() -> new AppException(404, Messages.get("veiculo.not.found", command.veiculoId())));
        if (!Boolean.TRUE.equals(veiculo.getIsActive())) {
            throw new AppException(422, Messages.get("veiculo.inactive", command.veiculoId()));
        }

        os.atualizar(command.clienteId(), command.veiculoId(), command.descricaoProblema());
        OrdemDeServico saved = repository.save(os);

        // Substituir itens na junction table
        if (command.itens() != null) {
            itemOsRepository.deleteByOsUuid(saved.getId());
            for (var itemCmd : command.itens()) {
                ItemOs itemOs = ItemOs.create(saved.getId(), itemCmd.referenciaUuid(),
                        itemCmd.tipo(), command.descricaoProblema(), itemCmd.quantidade());
                validateAndPersistItem(itemOs);
                itemOsRepository.save(itemOs);
            }
        }

        return saved;
    }

    @Override
    public List<OrdemDeServico> findAll(int page, int size, String sort) {
        return repository.findAll(page, size, sort);
    }

    @Override
    public long countAll() {
        return repository.countAll();
    }

    @Override
    @Transactional
    public OrdemDeServico iniciarDiagnostico(UUID id) {
        OrdemDeServico os = findById(id);
        os.iniciarDiagnostico();
        OrdemDeServico saved = repository.save(os);
        osAuditEventPublisher.publish(saved.getId(), OsAuditAction.DIAGNOSTICAR, null,
                OsAuditAction.DIAGNOSTICAR.getObservacaoDefault(), Map.of());
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarDiagnostico(FinalizarDiagnosticoCommand command) {
        OrdemDeServico os = findById(command.osId());

        // Adicionar novos itens do mecânico à junction table
        if (command.itens() != null) {
            for (var item : command.itens()) {
                ItemOs itemOs = ItemOs.create(os.getId(), item.referenciaUuid(),
                        item.tipo(), command.descricao(), item.quantidade());
                validateAndPersistItem(itemOs);
                itemOsRepository.save(itemOs);
            }
        }

        // Ler TODOS os itens da junction table para criar o orçamento
        List<ItemOs> todosItens = itemOsRepository.findByOsUuid(os.getId());
        List<ItemOrcamento> itensOrcamento = new ArrayList<>();

        for (ItemOs itemOs : todosItens) {
            if (itemOs.isPeca()) {
                Peca peca = pecaRepository.buscarPorId(itemOs.getReferenciaUuid())
                        .orElseThrow(() -> new AppException(404, "Peça não encontrada: " + itemOs.getReferenciaUuid()));
                if (!Boolean.TRUE.equals(peca.getIsActive())) {
                    throw new AppException(422, Messages.get("peca.inactive", itemOs.getReferenciaUuid()));
                }
                itensOrcamento.add(new ItemOrcamento(peca.getDescricao(), itemOs.getQuantidade(),
                        peca.getValorUnitario(), peca.getId()));
            } else if (itemOs.isServico()) {
                Servico servico = servicoRepository.findById(itemOs.getReferenciaUuid())
                        .orElseThrow(() -> new AppException(404, "Serviço não encontrado: " + itemOs.getReferenciaUuid()));
                if (!Boolean.TRUE.equals(servico.getIsActive())) {
                    throw new AppException(422, Messages.get("servico.inactive", itemOs.getReferenciaUuid()));
                }
                itensOrcamento.add(new ItemOrcamento(servico.getNome(), itemOs.getQuantidade(),
                        servico.getValor()));
            }
        }

        os.finalizarDiagnostico();
        repository.save(os);
        eventPublisher.publish(DiagnosticoFinalizadoEvent.of(os.getId(), command.descricao(), itensOrcamento));
        osAuditEventPublisher.publish(os.getId(), OsAuditAction.ORCAR, null,
                OsAuditAction.ORCAR.getObservacaoDefault(), Map.of("itens", itensOrcamento.size()));
        return os;
    }

    private void validateAndPersistItem(ItemOs itemOs) {
        if (itemOs.isPeca()) {
            var peca = pecaRepository.findById(itemOs.getReferenciaUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.peca.not.found", itemOs.getReferenciaUuid())));
            if (!Boolean.TRUE.equals(peca.getIsActive())) {
                throw new AppException(422, Messages.get("peca.inactive", itemOs.getReferenciaUuid()));
            }
        } else if (itemOs.isServico()) {
            var servico = servicoRepository.findById(itemOs.getReferenciaUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.servico.not.found", itemOs.getReferenciaUuid())));
            if (!Boolean.TRUE.equals(servico.getIsActive())) {
                throw new AppException(422, Messages.get("servico.inactive", itemOs.getReferenciaUuid()));
            }
        }
    }

    @Override
    @Transactional
    public OrdemDeServico cancelar(UUID id, String motivo) {
        OrdemDeServico os = findById(id);
        // D-08: cancelamento libera reserva (não credita saldo — peças nunca saíram do físico)
        if (os.getOrcamentoUuid() != null) {
            orcamentoRepository.findByUuid(os.getOrcamentoUuid()).ifPresent(orcamento -> {
                for (ItemOrcamento item : orcamento.getItens()) {
                    if (item.getPecaId() != null) {
                        pecaRepository.liberarReserva(item.getPecaId(), item.getQuantidade().intValue());
                    }
                }
            });
        }
        os.cancelar(motivo);
        OrdemDeServico saved = repository.save(os);
        osAuditEventPublisher.publish(saved.getId(), OsAuditAction.CANCELAR, null, motivo, Map.of());
        eventPublisher.publish(OSCanceladaEvent.of(saved.getId(), motivo));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico entregar(UUID id, String recebidoPor) {
        OrdemDeServico os = findById(id);
        var event = os.entregar(recebidoPor);
        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(event);
        osAuditEventPublisher.publish(saved.getId(), OsAuditAction.ENTREGAR, null, recebidoPor, Map.of());
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico iniciarExecucao(UUID id, UUID mecanicoUuid, String observacao) {
        OrdemDeServico os = findById(id);
        if (os.getStatus() != com.fiap.mekano.domain.model.StatusOS.AGUARDANDO_EXECUCAO) {
            throw new AppException(400, Messages.get("os.execucao.status.invalido.iniciar", os.getStatus()));
        }

        // D-04: debitar reserva dos itens de peça do orçamento antes de iniciar execução
        if (os.getOrcamentoUuid() != null) {
            orcamentoRepository.findByUuid(os.getOrcamentoUuid()).ifPresent(orcamento -> {
                for (ItemOrcamento item : orcamento.getItens()) {
                    if (item.getPecaId() != null) {
                        boolean ok = pecaRepository.debitarSaldoReservado(
                                item.getPecaId(), item.getQuantidade().intValue());
                        if (!ok) {
                            Peca peca = pecaRepository.findById(item.getPecaId())
                                    .orElseThrow(() -> new AppException(404,
                                            Messages.get("peca.not.found", item.getPecaId())));
                            throw new AppException(409, Messages.get("peca.saldo.insuficiente",
                                    item.getDescricao(), peca.getSaldoAtual(), item.getQuantidade()));
                        }
                    }
                }
            });
        }

        os.iniciarExecucao(mecanicoUuid, observacao);
        OrdemDeServico saved = repository.save(os);
        osAuditEventPublisher.publish(saved.getId(), OsAuditAction.EXECUTAR, null,
                OsAuditAction.EXECUTAR.getObservacaoDefault(),
                Map.of("mecanico", mecanicoUuid.toString()));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarExecucao(UUID id, String observacao) {
        OrdemDeServico os = findById(id);
        if (os.getStatus() != com.fiap.mekano.domain.model.StatusOS.EM_EXECUCAO) {
            throw new AppException(400, Messages.get("os.execucao.status.invalido.finalizar", os.getStatus()));
        }
        os.finalizarExecucao(observacao);
        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OSFinalizadaEvent.of(saved.getId()));
        osAuditEventPublisher.publish(saved.getId(), OsAuditAction.FINALIZAR, null,
                OsAuditAction.FINALIZAR.getObservacaoDefault(), Map.of());
        return saved;
    }

    @Override
    public List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
                                                    LocalDateTime dataInicio, LocalDateTime dataFim,
                                                    int page, int size) {
        return repository.findAllWithFilters(status, clienteUuid, veiculoUuid, dataInicio, dataFim, page, size);
    }

    @Override
    public Optional<OrdemDeServico> findByIdWithItems(UUID id) {
        return repository.findByIdWithItems(id);
    }

    @Override
    public Optional<UUID> findOrcamentoUuidByOsId(UUID osId) {
        return repository.findOrcamentoUuidByOsId(osId);
    }

    @Override
    public Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return repository.calcularTempoMedioExecucao(dataInicio, dataFim);
    }

    @Override
    public Map<UUID, Double> calcularTempoMedioPorMecanico(LocalDateTime dataInicio, LocalDateTime dataFim) {
        return repository.calcularTempoMedioPorMecanico(dataInicio, dataFim);
    }

    @Override
    public boolean clientePossuiOsAtiva(UUID clienteUuid) {
        return repository.existsByClienteUuidAndStatusIn(clienteUuid, List.of("EM_EXECUCAO", "AGUARDANDO_APROVACAO"));
    }

    @Override
    public List<String> buscarItensOrcados(UUID osId) {
        Optional<UUID> orcamentoUuid = repository.findOrcamentoUuidByOsId(osId);
        if (orcamentoUuid.isEmpty()) {
            return Collections.emptyList();
        }
        return orcamentoRepository.findByUuid(orcamentoUuid.get())
                .map(orcamento -> orcamento.getItens().stream()
                        .map(item -> {
                            String tipo = item.getPecaId() != null ? "Peça" : "Serviço";
                            return String.format("%s: %s x%d (R$ %.2f)",
                                    tipo, item.getDescricao(), item.getQuantidade(), item.calcularSubtotal());
                        })
                        .toList())
                .orElse(Collections.emptyList());
    }
}
