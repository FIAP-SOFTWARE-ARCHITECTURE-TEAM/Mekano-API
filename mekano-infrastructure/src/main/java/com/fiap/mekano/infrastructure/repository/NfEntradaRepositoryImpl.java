package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.infrastructure.entity.NfEntradaEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaRepositoryImpl implements NfEntradaRepositoryPort {

    private final NfEntradaPanacheRepository panacheRepository;

    public NfEntradaRepositoryImpl(NfEntradaPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public NfEntrada salvar(NfEntrada nfEntrada) {
        NfEntradaEntity entity = panacheRepository.find("uuid = ?1", nfEntrada.getId()).firstResult();
        if (entity == null) {
            entity = new NfEntradaEntity();
        }

        entity.uuid = nfEntrada.getId();
        entity.pecaId = nfEntrada.getId();
        entity.requisicaoCompraId = nfEntrada.getId();
        entity.quantidade = 1;
        entity.dataRecebimento = nfEntrada.getCreatedAt();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(nfEntrada.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    public Optional<NfEntrada> buscarPorId(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(NfEntradaRepositoryImpl::toDomain);
    }

    private static NfEntrada toDomain(NfEntradaEntity entity) {
        LocalDateTime now = entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt();
        String cnpj = "00000000000000";
        String chave = "00000000000000000000000000000000000000000000";

        return NfEntrada.reconstitute(
                entity.uuid,
                String.valueOf(entity.getId() == null ? 0L : entity.getId()),
                "1",
                cnpj,
                "Fornecedor",
                now,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                chave,
                now
        );
    }
}
