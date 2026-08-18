package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import com.fiap.mekano.infrastructure.mapper.ClienteEntityMapper;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ClienteRepositoryImpl implements ClienteRepositoryPort {

    private final ClientePanacheRepository panacheRepository;
    private final ClienteEntityMapper clienteEntityMapper;

    public ClienteRepositoryImpl(ClientePanacheRepository panacheRepository, ClienteEntityMapper clienteEntityMapper) {
        this.panacheRepository = panacheRepository;
        this.clienteEntityMapper = clienteEntityMapper;
    }

    @Override
    public Cliente save(Cliente cliente) {
        ClienteEntity entity = panacheRepository.find("uuid = ?1", cliente.getId()).firstResult();
        if (entity == null) {
            entity = new ClienteEntity();
        }

        entity.setUuid(cliente.getId());
        entity.setNome(cliente.getNome());
        entity.setCpf(cliente.getCpf().getValue());
        entity.setEmail(cliente.getEmail().getValue());
        entity.setTelefone(cliente.getTelefone() == null ? null : cliente.getTelefone().getValue());
        entity.setEnderecoLogradouro(cliente.getEndereco().getLogradouro());
        entity.setEnderecoNumero(cliente.getEndereco().getNumero());
        entity.setEnderecoBairro(cliente.getEndereco().getBairro());
        entity.setEnderecoCidade(cliente.getEndereco().getCidade());
        entity.setEnderecoUf(cliente.getEndereco().getUf());
        entity.setEnderecoCep(cliente.getEndereco().getCep());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(cliente.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return clienteEntityMapper.toDomain(entity);
    }

    @Override
    public Optional<Cliente> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(clienteEntityMapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return Optional.empty();
        }

        String digits = cpf.replaceAll("\\D", "");
        return panacheRepository.find("cpf = ?1 and isActive = ?2", digits, true)
                .firstResultOptional()
                .map(clienteEntityMapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return Optional.empty();
        }

        String digits = telefone.replaceAll("\\D", "");
        Optional<ClienteEntity> exact = panacheRepository
                .find("telefone = ?1 and isActive = ?2",
                        Sort.by("createdAt").descending(), digits, true)
                .firstResultOptional();
        if (exact.isPresent()) {
            return exact.map(clienteEntityMapper::toDomain);
        }

        // WR-04: fallback por sufixo incluindo o DDD (últimos 10 dígitos) com
        // ORDER BY createdAt (determinístico) e retorno APENAS quando há uma
        // única correspondência — múltiplos clientes com o mesmo número local
        // em DDDs diferentes tornam o resultado ambíguo (retorna vazio).
        if (digits.length() >= 10) {
            String suffix = digits.substring(digits.length() - 10);
            List<ClienteEntity> matches = panacheRepository
                    .find("telefone like ?1 and isActive = ?2",
                            Sort.by("createdAt").descending(), "%" + suffix, true)
                    .list();
            if (matches.size() == 1) {
                return Optional.of(matches.get(0)).map(clienteEntityMapper::toDomain);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean existsByCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");
        return panacheRepository.count("cpf = ?1 and isActive = ?2", digits, true) > 0;
    }

    @Override
    public List<Cliente> findAll(int page, int size, String sort) {
        return panacheRepository.find("isActive = ?1", parseSort(sort), true)
                .page(Page.of(Math.max(page, 0), normalizeSize(size)))
                .list()
                .stream()
                .map(clienteEntityMapper::toDomain)
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
        String sortValue = sort == null || sort.isBlank() ? "nome,asc" : sort;
        String[] sortParts = sortValue.split(",", 2);

        String sortField = switch (sortParts[0].strip()) {
            case "nome", "email", "createdAt" -> sortParts[0].strip();
            default -> "nome";
        };

        boolean ascending = sortParts.length < 2 || !"desc".equalsIgnoreCase(sortParts[1].strip());
        return ascending ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
    }
}
