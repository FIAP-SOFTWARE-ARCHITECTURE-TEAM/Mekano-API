package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.Endereco;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("EnderecoMapper")
class EnderecoMapperTest {

    @Inject
    EnderecoMapper mapper;

    private static Endereco criarEndereco() {
        return new Endereco("Rua A", "100", "Centro", "São Paulo", "SP", "01001000");
    }

    @Test
    @DisplayName("logradouro deve retornar valor do endereco")
    void logradouroDeveRetornarValor() {
        assertThat(mapper.logradouro(criarEndereco())).isEqualTo("Rua A");
    }

    @Test
    @DisplayName("numero deve retornar valor do endereco")
    void numeroDeveRetornarValor() {
        assertThat(mapper.numero(criarEndereco())).isEqualTo("100");
    }

    @Test
    @DisplayName("bairro deve retornar valor do endereco")
    void bairroDeveRetornarValor() {
        assertThat(mapper.bairro(criarEndereco())).isEqualTo("Centro");
    }

    @Test
    @DisplayName("cidade deve retornar valor do endereco")
    void cidadeDeveRetornarValor() {
        assertThat(mapper.cidade(criarEndereco())).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("uf deve retornar valor do endereco")
    void ufDeveRetornarValor() {
        assertThat(mapper.uf(criarEndereco())).isEqualTo("SP");
    }

    @Test
    @DisplayName("cep deve retornar valor do endereco")
    void cepDeveRetornarValor() {
        assertThat(mapper.cep(criarEndereco())).isEqualTo("01001000");
    }

    @Test
    @DisplayName("logradouro com endereco null deve retornar null")
    void logradouroComNullDeveRetornarNull() {
        assertThat(mapper.logradouro(null)).isNull();
    }

    @Test
    @DisplayName("numero com endereco null deve retornar null")
    void numeroComNullDeveRetornarNull() {
        assertThat(mapper.numero(null)).isNull();
    }

    @Test
    @DisplayName("bairro com endereco null deve retornar null")
    void bairroComNullDeveRetornarNull() {
        assertThat(mapper.bairro(null)).isNull();
    }

    @Test
    @DisplayName("cidade com endereco null deve retornar null")
    void cidadeComNullDeveRetornarNull() {
        assertThat(mapper.cidade(null)).isNull();
    }

    @Test
    @DisplayName("uf com endereco null deve retornar null")
    void ufComNullDeveRetornarNull() {
        assertThat(mapper.uf(null)).isNull();
    }

    @Test
    @DisplayName("cep com endereco null deve retornar null")
    void cepComNullDeveRetornarNull() {
        assertThat(mapper.cep(null)).isNull();
    }
}
