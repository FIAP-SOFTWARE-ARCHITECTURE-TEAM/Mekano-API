package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrdemDeServicoRepositoryImpl {

    private final OrdemDeServicoPanacheRepository panacheRepository;

    public OrdemDeServicoRepositoryImpl(OrdemDeServicoPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Transactional
    public OrdemDeServicoEntity salvar(OrdemDeServicoEntity os) {
        if (os.getId() == null) {
            panacheRepository.persist(os);
        }
        return os;
    }

    public Optional<OrdemDeServicoEntity> buscarPorUuid(UUID uuid) {
        return Optional.ofNullable(panacheRepository.buscarPorUuid(uuid));
    }

    public List<OrdemDeServicoEntity> buscarPorStatusPagamento(String statusPagamento) {
        return panacheRepository.buscarPorStatusPagamento(statusPagamento);
    }

    public List<OrdemDeServicoEntity> buscarPorStatusEntrega(String statusEntrega) {
        return panacheRepository.buscarPorStatusEntrega(statusEntrega);
    }

    public long contarPorStatusPagamento(String statusPagamento) {
        return panacheRepository.count("statusPagamento = ?1 and isActive = true", statusPagamento);
    }

    public long contarPorStatusEntrega(String statusEntrega) {
        return panacheRepository.count("statusEntrega = ?1 and isActive = true", statusEntrega);
    }
}
