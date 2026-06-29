package com.fiap.mekano.application.service.ordemdeservico;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.event.OrdemDeServicoCriadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.FinalizarDiagnosticoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;

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

    public OrdemDeServicoService(OrdemDeServicoRepositoryPort repository, EventPublisher eventPublisher,
                                 PecaRepositoryPort pecaRepository, ServicoRepositoryPort servicoRepository,
                                 OrcamentoRepositoryPort orcamentoRepository) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.pecaRepository = pecaRepository;
        this.servicoRepository = servicoRepository;
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    @Transactional
    public OrdemDeServico create(CreateOrdemDeServicoCommand command) {
        OrdemDeServico os = OrdemDeServico.create(command.clienteId(), command.veiculoId(), command.descricaoProblema());
        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OrdemDeServicoCriadaEvent.of(saved));
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
        os.atualizar(command.clienteId(), command.veiculoId(), command.descricaoProblema());
        return repository.save(os);
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
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarDiagnostico(FinalizarDiagnosticoCommand command) {
        OrdemDeServico os = findById(command.osId());
        List<ItemOrcamento> itens = new ArrayList<>();

        for (var item : command.itens()) {
            switch (item.tipo().toUpperCase()) {
                case "PECA" -> {
                    Peca peca = pecaRepository.buscarPorId(item.referenciaUuid())
                            .orElseThrow(() -> new AppException(404, "Peça não encontrada: " + item.referenciaUuid()));
                    itens.add(new ItemOrcamento(peca.getDescricao(), item.quantidade(), peca.getValorUnitario()));
                }
                case "SERVICO" -> {
                    Servico servico = servicoRepository.findById(item.referenciaUuid())
                            .orElseThrow(() -> new AppException(404, "Serviço não encontrado: " + item.referenciaUuid()));
                    itens.add(new ItemOrcamento(servico.getNome(), item.quantidade(), servico.getValor()));
                }
                default -> throw new AppException(400, "Tipo de item inválido: " + item.tipo());
            }
        }

        os.finalizarDiagnostico();
        Orcamento orcamento = Orcamento.create(command.descricao(), itens, os.getId());
        os.associarOrcamento(orcamento.getId());
        repository.save(os);
        orcamentoRepository.save(orcamento);
        eventPublisher.publish(DiagnosticoFinalizadoEvent.of(os.getId(), command.descricao(), itens));
        return os;
    }

    @Override
    @Transactional
    public OrdemDeServico cancelar(UUID id, String motivo) {
        OrdemDeServico os = findById(id);
        os.cancelar(motivo);
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico finalizar(UUID id) {
        OrdemDeServico os = findById(id);
        os.finalizar();
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico entregar(UUID id) {
        OrdemDeServico os = findById(id);
        os.entregar();
        return repository.save(os);
    }
}
