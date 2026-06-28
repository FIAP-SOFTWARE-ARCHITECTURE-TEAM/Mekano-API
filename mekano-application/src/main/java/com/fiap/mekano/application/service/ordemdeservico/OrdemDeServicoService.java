package com.fiap.mekano.application.service.ordemdeservico;

import com.fiap.mekano.domain.event.OrdemDeServicoCriadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.CreateOrdemDeServicoCommand;
import com.fiap.mekano.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use case de OrdemDeServico — orquestra transições chamando métodos explícitos
 * da entidade. Nunca setStatus() (D-26).
 */
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
    public OrdemDeServico finalizarDiagnostico(UUID id) {
        OrdemDeServico os = findById(id);
        os.finalizarDiagnostico();
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico aprovarOrcamento(UUID id) {
        OrdemDeServico os = findById(id);
        os.aprovarOrcamento();
        return repository.save(os);
    }

    @Override
    @Transactional
    public OrdemDeServico reprovarOrcamento(UUID id, String motivo) {
        OrdemDeServico os = findById(id);
        os.reprovarOrcamento(motivo);
        return repository.save(os);
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
