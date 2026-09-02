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
 * - Telefone é opcional (nullable) — clientes podem ser cadastrados sem
 * telefone.
 * - Endereço é validado/normalizado pelo VO {@link Endereco} (UF uppercase, CEP
 * só dígitos).
 * - Imutável após criação: campos final, sem setters.
 *
 * Mapeamento JPA (ClienteEntity) é responsabilidade do módulo infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Cliente {

    private final UUID id;
    private String nome;
    private final Cpf cpf;
    private Email email;
    private Telefone telefone;
    private Endereco endereco;
    private final LocalDateTime createdAt;
    private final Boolean isActive;

    public static Cliente create(
            String nome,
            String cpfValue,
            String emailValue,
            String telefoneValue,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String uf,
            String cep) {

        return Cliente.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .cpf(new Cpf(cpfValue))
                .email(new Email(emailValue))
                .telefone(toTelefone(telefoneValue))
                .endereco(new Endereco(
                        logradouro,
                        numero,
                        bairro,
                        cidade,
                        uf,
                        cep))
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    public static Cliente reconstitute(
            UUID id,
            String nome,
            String cpfValue,
            String emailValue,
            String telefoneValue,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String uf,
            String cep,
            LocalDateTime createdAt) {

        return reconstitute(id, nome, cpfValue, emailValue, telefoneValue,
                logradouro, numero, bairro, cidade, uf, cep, createdAt, true);
    }

    public static Cliente reconstitute(
            UUID id,
            String nome,
            String cpfValue,
            String emailValue,
            String telefoneValue,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String uf,
            String cep,
            LocalDateTime createdAt,
            Boolean isActive) {

        return Cliente.builder()
                .id(id)
                .nome(nome)
                .cpf(new Cpf(cpfValue))
                .email(new Email(emailValue))
                .telefone(toTelefone(telefoneValue))
                .endereco(new Endereco(
                        logradouro,
                        numero,
                        bairro,
                        cidade,
                        uf,
                        cep))
                .createdAt(createdAt)
                .isActive(isActive)
                .build();
    }

    public void atualizar(
            String nome,
            String emailValue,
            String telefoneValue,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String uf,
            String cep) {

        this.nome = nome;
        this.email = new Email(emailValue);
        this.telefone = toTelefone(telefoneValue);
        this.endereco = new Endereco(
                logradouro,
                numero,
                bairro,
                cidade,
                uf,
                cep);
    }

    private static Telefone toTelefone(String value) {
        return value != null && !value.isBlank()
                ? new Telefone(value)
                : null;
    }
}
