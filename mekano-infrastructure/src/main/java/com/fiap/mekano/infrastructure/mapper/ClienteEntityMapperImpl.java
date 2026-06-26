package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClienteEntityMapperImpl implements ClienteEntityMapper {

    @Inject
    CpfMapper cpfMapper;

    @Inject
    EmailMapper emailMapper;

    @Inject
    TelefoneMapper telefoneMapper;

    @Override
    public ClienteEntity toEntity(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        ClienteEntity entity = new ClienteEntity();
        entity.setUuid(cliente.getId());
        entity.setNome(cliente.getNome());
        entity.setCpf(cpfMapper.cpfToString(cliente.getCpf()));
        entity.setEmail(emailMapper.emailToString(cliente.getEmail()));
        entity.setTelefone(telefoneMapper.telefoneToString(cliente.getTelefone()));
        entity.setEnderecoLogradouro(cliente.getEnderecoLogradouro());
        entity.setEnderecoNumero(cliente.getEnderecoNumero());
        entity.setEnderecoBairro(cliente.getEnderecoBairro());
        entity.setEnderecoCidade(cliente.getEnderecoCidade());
        entity.setEnderecoUf(cliente.getEnderecoUf());
        entity.setEnderecoCep(cliente.getEnderecoCep());
        entity.setCreatedAt(cliente.getCreatedAt());
        return entity;
    }

    @Override
    public Cliente toDomain(ClienteEntity entity) {
        if (entity == null) {
            return null;
        }
        return Cliente.reconstitute(
                entity.getUuid(),
                entity.getNome(),
                entity.getCpf(),
                entity.getEmail(),
                entity.getTelefone(),
                entity.getEnderecoLogradouro(),
                entity.getEnderecoNumero(),
                entity.getEnderecoBairro(),
                entity.getEnderecoCidade(),
                entity.getEnderecoUf(),
                entity.getEnderecoCep(),
                entity.getCreatedAt()
        );
    }
}
