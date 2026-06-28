package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ServicoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI Panache que expõe as operações de banco para {@link ServicoEntity}.
 *
 * <p>Separado de {@link ServicoRepositoryImpl} para evitar conflito de assinatura:
 * PK interna é {@code Long} (auto-increment).
 */
@ApplicationScoped
public class ServicoPanacheRepository implements PanacheRepositoryBase<ServicoEntity, Long> {
}
