package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.Endereco;
import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.Named;

@ApplicationScoped
public class EnderecoMapper {

    @Named("enderecoLogradouro")
    public String logradouro(Endereco endereco) {
        return endereco == null ? null : endereco.getLogradouro();
    }

    @Named("enderecoNumero")
    public String numero(Endereco endereco) {
        return endereco == null ? null : endereco.getNumero();
    }

    @Named("enderecoBairro")
    public String bairro(Endereco endereco) {
        return endereco == null ? null : endereco.getBairro();
    }

    @Named("enderecoCidade")
    public String cidade(Endereco endereco) {
        return endereco == null ? null : endereco.getCidade();
    }

    @Named("enderecoUf")
    public String uf(Endereco endereco) {
        return endereco == null ? null : endereco.getUf();
    }

    @Named("enderecoCep")
    public String cep(Endereco endereco) {
        return endereco == null ? null : endereco.getCep();
    }
}
