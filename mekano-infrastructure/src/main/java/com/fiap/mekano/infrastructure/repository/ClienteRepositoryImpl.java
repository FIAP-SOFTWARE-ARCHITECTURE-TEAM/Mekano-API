package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import com.fiap.mekano.infrastructure.mapper.ClienteEntityMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
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
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public Cliente update(Cliente cliente) {
        ClienteEntity entity = panacheRepository.find("uuid = ?1", cliente.getId())
                .firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", cliente.getId())));

        clienteEntityMapper.updateEntity(cliente, entity);

        return clienteEntityMapper.toDomain(entity);
    }

    @Override
    @Retry(maxRetries = 3)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = CacheNames.CLIENTES)
    public Optional<Cliente> findById(UUID id) {
        return panacheRepository.find("uuid = ?1", id)
                .firstResultOptional()
                .map(clienteEntityMapper::toDomain);
    }

    @Override
    @Retry(maxRetries = 3)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
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
    @Retry(maxRetries = 3)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
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

        // WR-04: fallback por sufixo incluindo o DDD (ÃƒÂºltimos 10 dÃƒÂ­gitos) com
        // ORDER BY createdAt (determinÃƒÂ­stico) e retorno APENAS quando hÃƒÂ¡ uma
        // ÃƒÂºnica correspondÃƒÂªncia Ã¢â‚¬â€ mÃƒÂºltiplos clientes com o mesmo nÃƒÂºmero local
        // em DDDs diferentes tornam o resultado ambÃƒÂ­guo (retorna vazio).
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
    @Retry(maxRetries = 3)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public boolean existsByCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");
        return panacheRepository.count("cpf = ?1 and isActive = ?2", digits, true) > 0;
    }

    @Override
    @Retry(maxRetries = 3)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public List<Cliente> findAll(int page, int size, String sort, Boolean isActive) {
        Sort panacheSort = parseSort(sort);
        PanacheQuery<ClienteEntity> query = isActive == null
                ? panacheRepository.findAll(panacheSort)
                : panacheRepository.find("isActive = ?1", panacheSort, isActive);
        return query.page(Page.of(Math.max(page, 0), normalizeSize(size)))
                .list()
                .stream()
                .map(clienteEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Retry(maxRetries = 3)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public long countAll(Boolean isActive) {
        return isActive == null
                ? panacheRepository.count()
                : panacheRepository.count("isActive = ?1", isActive);
    }

    @Override
    @Transactional
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public void markAsDeleted(UUID id) {        ClienteEntity entity = panacheRepository.find("uuid = ?1", id)
                .firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));
        entity.setIsActive(false);
        entity.setDeletedAt(LocalDateTime.now());
    }

    @Override
    @Transactional
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public void reactivate(UUID id) {
        ClienteEntity entity = panacheRepository.find("uuid = ?1", id)
                .firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));
        entity.setIsActive(true);
        entity.setDeletedAt(null);
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
