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

    @Inject
    EnderecoMapper enderecoMapper;

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
        entity.setEnderecoLogradouro(enderecoMapper.logradouro(cliente.getEndereco()));
        entity.setEnderecoNumero(enderecoMapper.numero(cliente.getEndereco()));
        entity.setEnderecoBairro(enderecoMapper.bairro(cliente.getEndereco()));
        entity.setEnderecoCidade(enderecoMapper.cidade(cliente.getEndereco()));
        entity.setEnderecoUf(enderecoMapper.uf(cliente.getEndereco()));
        entity.setEnderecoCep(enderecoMapper.cep(cliente.getEndereco()));
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
                entity.getCreatedAt());
    }

    @Override
    public void updateEntity(Cliente cliente, ClienteEntity entity) {
        if (cliente == null || entity == null) {
            return;
        }
        entity.setNome(cliente.getNome());
        entity.setEmail(cliente.getEmail().getValue());
        entity.setTelefone(cliente.getTelefone() == null ? null : cliente.getTelefone().getValue());
        entity.setEnderecoLogradouro(cliente.getEndereco().getLogradouro());
        entity.setEnderecoNumero(cliente.getEndereco().getNumero());
        entity.setEnderecoBairro(cliente.getEndereco().getBairro());
        entity.setEnderecoCidade(cliente.getEndereco().getCidade());
        entity.setEnderecoUf(cliente.getEndereco().getUf());
        entity.setEnderecoCep(cliente.getEndereco().getCep());
    }
}
