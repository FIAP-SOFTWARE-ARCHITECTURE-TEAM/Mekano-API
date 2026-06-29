package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PecaService {

    private final PecaRepositoryPort pecaRepository;
    private final EventPublisher eventPublisher;

    public PecaService(PecaRepositoryPort pecaRepository, EventPublisher eventPublisher) {
        this.pecaRepository = pecaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreatePecaResponse criar(CreatePecaCommand command) {
        var peca = Peca.create(
                command.codigo(),
                command.descricao(),
                command.valorUnitario(),
                command.estoqueMinimo()
        );
        var saved = pecaRepository.salvar(peca);
        return new CreatePecaResponse(
                saved.getId(), saved.getCodigo(), saved.getDescricao(),
                saved.getValorUnitario(),
                saved.getSaldoAtual(), saved.getEstoqueMinimo(), saved.getCreatedAt()
        );
    }

    public Peca buscarPorId(UUID id) {
        return pecaRepository.buscarPorId(id)
                .orElseThrow(() -> new AppException(404, Messages.get("peca.not.found", id)));
    }

    @Transactional
    public boolean debitarSaldo(UUID pecaId, Integer quantidade) {
        var peca = buscarPorId(pecaId);
        boolean sucesso = pecaRepository.debitarSaldo(pecaId, quantidade);
        if (sucesso) {
            Long novoSaldo = peca.getSaldoAtual() - quantidade;
            if (peca.getEstoqueMinimo() != null && novoSaldo <= peca.getEstoqueMinimo()) {
                eventPublisher.publish(new EstoqueMinimoAtingidoEvent(
                        pecaId, novoSaldo.intValue(), peca.getEstoqueMinimo().intValue()));
            }
        }
        return sucesso;
    }

    @Transactional
    public void creditarSaldo(UUID pecaId, Integer quantidade) {
        pecaRepository.creditarSaldo(pecaId, quantidade);
    }

    public List<Peca> findAll(int page, int size) {
        return pecaRepository.findAll(page, size);
    }

    public long countAll() {
        return pecaRepository.countAll();
    }

    public List<Peca> listarAbaixoEstoqueMinimo() {
        return pecaRepository.listarAbaixoEstoqueMinimo();
    }
}
