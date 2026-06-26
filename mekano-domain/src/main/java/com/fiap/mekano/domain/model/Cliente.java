package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.valueobject.Cpf;
import com.fiap.mekano.domain.valueobject.Email;
import com.fiap.mekano.domain.valueobject.Endereco;
import com.fiap.mekano.domain.valueobject.Telefone;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio Cliente — POJO puro sem anotações JPA.
 *
 * Regras:
 * - Criação APENAS via factory method {@link #create} ou {@link #reconstitute}.
 * - O builder é privado para forçar o uso dos factory methods.
 * - Telefone é opcional (nullable) — clientes podem ser cadastrados sem telefone.
 * - Endereço é validado/normalizado pelo VO {@link Endereco} (UF uppercase, CEP só dígitos).
 * - Imutável após criação: campos final, sem setters.
 *
 * Mapeamento JPA (ClienteEntity) é responsabilidade do módulo infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Cliente {

    private final UUID id;
    private final String nome;
    private final Cpf cpf;
    private final Email email;
    private final Telefone telefone;
    private final Endereco endereco;
    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de um novo cliente.
     * Telefone é opcional: se null ou blank, será armazenado como null.
     * Endereço é validado e normalizado pelo VO Endereco.
     */
    public static Cliente create(String nome, String cpfValue, String emailValue,
                                  String telefoneValue, String logradouro, String numero,
                                  String bairro, String cidade, String uf, String cep) {
        return Cliente.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .cpf(new Cpf(cpfValue))
                .email(new Email(emailValue))
                .telefone(toTelefone(telefoneValue))
                .endereco(new Endereco(logradouro, numero, bairro, cidade, uf, cep))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp — preserva exatamente os valores do banco.
     * VOs são revalidados na reconstrução.
     */
    public static Cliente reconstitute(UUID id, String nome, String cpfValue, String emailValue,
                                        String telefoneValue, String logradouro, String numero,
                                        String bairro, String cidade, String uf, String cep,
                                        LocalDateTime createdAt) {
        return Cliente.builder()
                .id(id)
                .nome(nome)
                .cpf(new Cpf(cpfValue))
                .email(new Email(emailValue))
                .telefone(toTelefone(telefoneValue))
                .endereco(new Endereco(logradouro, numero, bairro, cidade, uf, cep))
                .createdAt(createdAt)
                .build();
    }

    private static Telefone toTelefone(String value) {
        return value != null && !value.isBlank() ? new Telefone(value) : null;
    }
}
