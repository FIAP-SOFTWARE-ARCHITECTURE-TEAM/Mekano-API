package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.in.UpdatePecaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PecaService {

    private final PecaRepositoryPort pecaRepository;
    private final EventPublisher eventPublisher;
    private final OrcamentoRepositoryPort orcamentoRepository;

    public PecaService(PecaRepositoryPort pecaRepository, EventPublisher eventPublisher,
                       OrcamentoRepositoryPort orcamentoRepository) {
        this.pecaRepository = pecaRepository;
        this.eventPublisher = eventPublisher;
        this.orcamentoRepository = orcamentoRepository;
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

    @Transactional
    public Peca updatePeca(UUID id, UpdatePecaCommand command) {
        Peca atual = buscarPorId(id);
        Peca atualizada = Peca.reconstitute(
                id, command.codigo(), command.descricao(), command.valorUnitario(),
                atual.getSaldoAtual(), command.estoqueMinimo(), atual.getCreatedAt(), atual.getSaldoReservado(),
                atual.getIsActive());
        return pecaRepository.salvar(atualizada);
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
            Long novoDisponivel = novoSaldo - peca.getSaldoReservado();
            if (peca.getEstoqueMinimo() != null && novoDisponivel < peca.getEstoqueMinimo()) {
                eventPublisher.publish(new EstoqueMinimoAtingidoEvent(
                        pecaId, novoDisponivel.intValue(), peca.getEstoqueMinimo().intValue()));
            }
        }
        return sucesso;
    }

    @Transactional
    public void creditarSaldo(UUID pecaId, Integer quantidade) {
        pecaRepository.creditarSaldo(pecaId, quantidade);
    }

    @Transactional
    public boolean reservarSaldo(UUID pecaId, Integer quantidade) {
        return pecaRepository.reservarSaldo(pecaId, quantidade);
    }

    @Transactional
    public boolean debitarSaldoReservado(UUID pecaId, Integer quantidade) {
        return pecaRepository.debitarSaldoReservado(pecaId, quantidade);
    }

    @Transactional
    public boolean liberarReserva(UUID pecaId, Integer quantidade) {
        return pecaRepository.liberarReserva(pecaId, quantidade);
    }

    public List<Peca> findAll(int page, int size, Boolean isActive) {
        return pecaRepository.findAll(page, size, isActive);
    }

    public long countAll(Boolean isActive) {
        return pecaRepository.countAll(isActive);
    }

    public List<Peca> listarAbaixoEstoqueMinimo() {
        return pecaRepository.listarAbaixoEstoqueMinimo();
    }

    @Transactional
    public void excluir(UUID pecaId) {
        if (orcamentoRepository.existsByPecaIdVinculadaAOrdemComStatus(
                pecaId, List.of("AGUARDANDO_APROVACAO", "EM_EXECUCAO"))) {
            throw new AppException(409, Messages.get("os.peca.vinculada.os.ativa"));
        }
        pecaRepository.remover(pecaId);
    }

    @Transactional
    public void reativar(UUID pecaId) {
        pecaRepository.reativar(pecaId);
    }
}
