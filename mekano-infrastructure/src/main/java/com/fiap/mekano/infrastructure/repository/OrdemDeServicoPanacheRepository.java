package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrdemDeServicoPanacheRepository implements PanacheRepository<OrdemDeServicoEntity, Long> {

    public OrdemDeServicoEntity buscarPorUuid(UUID uuid) {
        return find("uuid = ?1 and isActive = true", uuid).firstResult();
    }

    public List<OrdemDeServicoEntity> buscarPorStatusPagamento(String statusPagamento) {
        return find("statusPagamento = ?1 and isActive = true", statusPagamento).list();
    }

    public List<OrdemDeServicoEntity> buscarPorStatusEntrega(String statusEntrega) {
        return find("statusEntrega = ?1 and isActive = true", statusEntrega).list();
    }
}
