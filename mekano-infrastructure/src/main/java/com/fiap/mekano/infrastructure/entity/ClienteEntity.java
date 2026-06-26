package com.fiap.mekano.infrastructure.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class ClienteEntity extends BaseEntity {

    @Column(unique = true, nullable = false)
    UUID uuid;

    @Column(nullable = false)
    String nome;

    @Column(nullable = false, length = 11)
    String cpf;

    @Column(nullable = false)
    String email;

    @Column(length = 11)
    String telefone;

    @Column(name = "endereco_logradouro")
    String enderecoLogradouro;

    @Column(name = "endereco_numero", length = 20)
    String enderecoNumero;

    @Column(name = "endereco_bairro")
    String enderecoBairro;

    @Column(name = "endereco_cidade")
    String enderecoCidade;

    @Column(name = "endereco_uf", length = 2)
    String enderecoUf;

    @Column(name = "endereco_cep", length = 8)
    String enderecoCep;
}
