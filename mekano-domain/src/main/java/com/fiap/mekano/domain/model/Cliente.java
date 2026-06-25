package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.valueobject.Cpf;
import com.fiap.mekano.domain.valueobject.Email;
import com.fiap.mekano.domain.valueobject.Telefone;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Cliente {

    private final UUID id;
    private final String nome;
    private final Cpf cpf;
    private final Email email;
    private final Telefone telefone;
    private final String enderecoLogradouro;
    private final String enderecoNumero;
    private final String enderecoBairro;
    private final String enderecoCidade;
    private final String enderecoUf;
    private final String enderecoCep;
    private final LocalDateTime createdAt;

    public static Cliente create(String nome, String cpfValue, String emailValue,
                                  String telefoneValue, String logradouro, String numero,
                                  String bairro, String cidade, String uf, String cep) {
        return Cliente.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .cpf(new Cpf(cpfValue))
                .email(new Email(emailValue))
                .telefone(new Telefone(telefoneValue))
                .enderecoLogradouro(logradouro)
                .enderecoNumero(numero)
                .enderecoBairro(bairro)
                .enderecoCidade(cidade)
                .enderecoUf(uf)
                .enderecoCep(cep)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Cliente reconstitute(UUID id, String nome, String cpfValue, String emailValue,
                                        String telefoneValue, String logradouro, String numero,
                                        String bairro, String cidade, String uf, String cep,
                                        LocalDateTime createdAt) {
        return Cliente.builder()
                .id(id)
                .nome(nome)
                .cpf(new Cpf(cpfValue))
                .email(new Email(emailValue))
                .telefone(new Telefone(telefoneValue))
                .enderecoLogradouro(logradouro)
                .enderecoNumero(numero)
                .enderecoBairro(bairro)
                .enderecoCidade(cidade)
                .enderecoUf(uf)
                .enderecoCep(cep)
                .createdAt(createdAt)
                .build();
    }
}
