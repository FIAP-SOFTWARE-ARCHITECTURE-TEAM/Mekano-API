package com.fiap.mekano.application.service.ordemservico;

import com.fiap.mekano.domain.event.OrdemDeServicoCriadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.port.in.CancelarOSCommand;
import com.fiap.mekano.domain.port.in.CriarOSCommand;
import com.fiap.mekano.domain.port.in.FinalizarExecucaoCommand;
import com.fiap.mekano.domain.port.in.IniciarExecucaoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrdemDeServicoService implements OrdemDeServicoServicePort {

    private final OrdemDeServicoRepositoryPort repository;
    private final EventPublisher eventPublisher;

    public OrdemDeServicoService(OrdemDeServicoRepositoryPort repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OrdemDeServico criar(CriarOSCommand command) {
        OrdemDeServico os = OrdemDeServico.create(
                command.clienteId(), command.veiculoId(), command.descricaoProblema());

        OrdemDeServico saved = repository.save(os);
        eventPublisher.publish(OrdemDeServicoCriadaEvent.of(saved));
        return saved;
    }

    @Override
    @Transactional
    public OrdemDeServico iniciarDiagnostico(UUID osUuid) {
        OrdemDeServico os = buscar(osUuid);
        os.iniciarDiagnostico();
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarDiagnostico(UUID osUuid) {
        OrdemDeServico os = buscar(osUuid);
        os.finalizarDiagnostico();
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico iniciarExecucao(IniciarExecucaoCommand command) {
        OrdemDeServico os = buscar(command.osUuid());
        if (os.getStatus() != StatusOS.EM_EXECUCAO) {
            throw new AppException(422, Messages.get("os.transicao.invalida", os.getStatus(), StatusOS.EM_EXECUCAO));
        }
        os.iniciarExecucao(command.mecanicoUuid(), command.observacao());
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico finalizarExecucao(FinalizarExecucaoCommand command) {
        OrdemDeServico os = buscar(command.osUuid());
        os.finalizarExecucao(command.observacao());
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico entregar(UUID osUuid) {
        OrdemDeServico os = buscar(osUuid);
        os.entregar();
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico cancelar(CancelarOSCommand command) {
        OrdemDeServico os = buscar(command.osUuid());
        os.cancelar(command.motivo());
        return repository.save(os);
    }

    @Override
    public OrdemDeServico buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", id)));
    }

    @Override
    public List<OrdemDeServico> listar(int page, int size, String sort) {
        return repository.findAll(page, size, sort);
    }

    @Override
    public List<OrdemDeServico> listarComFiltros(String status, UUID clienteUuid, int page, int size) {
        if (status != null && !status.isBlank()) {
            try {
                StatusOS.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new AppException(400, Messages.get("os.status.invalido", status));
            }
        }
        return repository.findAllWithFilters(status, clienteUuid, page, size);
    }

    @Override
    public long contar() {
        return repository.countAll();
    }

    @Override
    public Optional<Double> calcularTempoMedioExecucao() {
        return repository.calcularTempoMedioExecucao();
    }

    @Override
    @Transactional
    public void deletar(UUID id) {
        repository.markAsDeleted(id);
    }

    private OrdemDeServico buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", id)));
    }
}
