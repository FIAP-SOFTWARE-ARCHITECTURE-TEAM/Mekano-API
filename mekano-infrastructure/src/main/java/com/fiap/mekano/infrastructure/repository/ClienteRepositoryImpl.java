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
    public Cliente create(Cliente cliente) {
        ClienteEntity entity = new ClienteEntity();
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
        entity.setCreatedAt(cliente.getCreatedAt());
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        panacheRepository.persist(entity);

        return clienteEntityMapper.toDomain(entity);
    }

    @Override
    public Cliente update(Cliente cliente) {
        ClienteEntity entity = panacheRepository.find("uuid = ?1 and isActive = ?2", cliente.getId(), true)
                .firstResult();
        if (entity == null) {
            throw new IllegalArgumentException("Cliente não encontrado para atualização: " + cliente.getId());
        }

        clienteEntityMapper.updateEntity(cliente, entity);

        return clienteEntityMapper.toDomain(entity);
    }

    @Override
    public Optional<Cliente> findById(UUID id) {
        return panacheRepository.find("uuid = ?1", id)
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
    public boolean existsByCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");
        return panacheRepository.count("cpf = ?1 and isActive = ?2", digits, true) > 0;
    }

    @Override
    public List<Cliente> findAll(int page, int size, String sort) {
        return panacheRepository.findAll(parseSort(sort))
                .page(Page.of(Math.max(page, 0), normalizeSize(size)))
                .list()
                .stream()
                .map(clienteEntityMapper::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count();
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
