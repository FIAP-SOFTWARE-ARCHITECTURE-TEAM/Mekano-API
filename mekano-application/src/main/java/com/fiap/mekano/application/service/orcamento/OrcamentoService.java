package com.fiap.mekano.application.service.orcamento;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrcamentoService implements OrcamentoServicePort {

    private final OrcamentoRepositoryPort orcamentoRepository;
    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;

    public OrcamentoService(OrcamentoRepositoryPort orcamentoRepository,
                            OrdemDeServicoRepositoryPort ordemDeServicoRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    @Transactional
    public Orcamento aprovar(AprovarOrcamentoCommand command) {
        Orcamento orcamento = orcamentoRepository.findByUuid(command.orcamentoUuid())
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", command.orcamentoUuid())));

        orcamento.aprovar();

        if (orcamento.getOrdemServicoUuid() != null) {
            OrdemDeServico os = ordemDeServicoRepository.findById(orcamento.getOrdemServicoUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", orcamento.getOrdemServicoUuid())));
            os.aprovarOrcamento(orcamento.getId());
            ordemDeServicoRepository.save(os);
        }

        return orcamentoRepository.save(orcamento);
    }

    @Override
    @Transactional
    public Orcamento reprovar(ReprovarOrcamentoCommand command) {
        Orcamento orcamento = orcamentoRepository.findByUuid(command.orcamentoUuid())
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", command.orcamentoUuid())));

        orcamento.reprovar();

        if (orcamento.getOrdemServicoUuid() != null) {
            OrdemDeServico os = ordemDeServicoRepository.findById(orcamento.getOrdemServicoUuid())
                    .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", orcamento.getOrdemServicoUuid())));
            os.reprovarOrcamento(command.motivo());
            ordemDeServicoRepository.save(os);
        }

        return orcamentoRepository.save(orcamento);
    }

    @Override
    public Orcamento buscarPorId(UUID orcamentoUuid) {
        return orcamentoRepository.findByUuid(orcamentoUuid)
                .orElseThrow(() -> new AppException(404, Messages.get("orcamento.not.found", orcamentoUuid)));
    }

    @Override
    public Orcamento buscarPorOrdemServico(UUID osUuid) {
        return orcamentoRepository.findByOrdemServicoUuid(osUuid)
                .orElseThrow(() -> new AppException(404, "Nenhum orçamento encontrado para a OS: " + osUuid));
    }
}
