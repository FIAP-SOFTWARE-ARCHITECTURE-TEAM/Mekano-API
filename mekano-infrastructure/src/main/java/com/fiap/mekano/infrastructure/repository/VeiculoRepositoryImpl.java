package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.infrastructure.entity.VeiculoEntity;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VeiculoRepositoryImpl implements VeiculoRepositoryPort {

    private final VeiculoPanacheRepository panacheRepository;

    public VeiculoRepositoryImpl(VeiculoPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public Veiculo save(Veiculo veiculo) {
        VeiculoEntity entity = panacheRepository.find("uuid = ?1", veiculo.getId()).firstResult();
        if (entity == null) {
            entity = new VeiculoEntity();
        }

        entity.setUuid(veiculo.getId());
        entity.setClienteUuid(veiculo.getClienteUuid());
        entity.setPlaca(veiculo.getPlaca().getValue());
        entity.setMarca(veiculo.getMarca());
        entity.setModelo(veiculo.getModelo());
        entity.setAno(veiculo.getAno());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(veiculo.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    public Optional<Veiculo> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(VeiculoRepositoryImpl::toDomain);
    }

    @Override
    public Optional<Veiculo> findByPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            return Optional.empty();
        }

        String normalizedPlaca = placa.strip().toUpperCase().replace("-", "");
        return panacheRepository.find("placa = ?1 and isActive = ?2", normalizedPlaca, true)
                .firstResultOptional()
                .map(VeiculoRepositoryImpl::toDomain);
    }

    @Override
    public boolean existsByPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            return false;
        }

        String normalizedPlaca = placa.strip().toUpperCase().replace("-", "");
        return panacheRepository.count("placa = ?1 and isActive = ?2", normalizedPlaca, true) > 0;
    }

    @Override
    public List<Veiculo> findAll(int page, int size, String sort) {
        return panacheRepository.find("isActive = ?1", parseSort(sort), true)
                .page(Page.of(Math.max(page, 0), normalizeSize(size)))
                .list()
                .stream()
                .map(VeiculoRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }

    @Override
    public void markAsDeleted(UUID id) {
        panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .ifPresent(entity -> {
                    entity.setIsActive(false);
                    entity.setDeletedAt(LocalDateTime.now());
                });
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private static Sort parseSort(String sort) {
        String sortValue = sort == null || sort.isBlank() ? "placa,asc" : sort;
        String[] sortParts = sortValue.split(",", 2);

        String sortField = switch (sortParts[0].strip()) {
            case "placa", "marca", "modelo", "ano", "createdAt" -> sortParts[0].strip();
            default -> "placa";
        };

        boolean ascending = sortParts.length < 2 || !"desc".equalsIgnoreCase(sortParts[1].strip());
        return ascending ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
    }

    private static Veiculo toDomain(VeiculoEntity entity) {
        return Veiculo.reconstitute(
                entity.getUuid(),
                entity.getClienteUuid(),
                entity.getPlaca(),
                entity.getMarca(),
                entity.getModelo(),
                entity.getAno(),
                entity.getCreatedAt()
        );
    }
}
