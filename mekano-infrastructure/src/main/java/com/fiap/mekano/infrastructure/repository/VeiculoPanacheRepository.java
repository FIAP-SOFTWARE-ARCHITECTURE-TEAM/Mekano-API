package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.VeiculoEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI Panache que expõe as operações de banco para {@link VeiculoEntity}.
 *
 * <p>
 * Separado de {@link VeiculoRepositoryImpl} para evitar conflito de assinatura:
 * a PK interna é {@code Long} (auto-increment).
 * Todos os métodos Panache (persist, flush, find, count, etc.)
 * são herdados de {@code PanacheRepositoryBase} via bytecode enhancement do
 * Quarkus.
 */

@ApplicationScoped
public class VeiculoPanacheRepository
        implements PanacheRepositoryBase<VeiculoEntity, Long> {
}
