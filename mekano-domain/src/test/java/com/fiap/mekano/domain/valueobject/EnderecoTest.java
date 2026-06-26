package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Endereco Value Object")
class EnderecoTest {

    @Test
    @DisplayName("deve criar endereço válido")
    void deveCriarEnderecoValido() {
        Endereco endereco = new Endereco("Rua das Flores", "100", "Centro", "São Paulo", "SP", "01234567");

        assertThat(endereco.getLogradouro()).isEqualTo("Rua das Flores");
        assertThat(endereco.getNumero()).isEqualTo("100");
        assertThat(endereco.getBairro()).isEqualTo("Centro");
        assertThat(endereco.getCidade()).isEqualTo("São Paulo");
        assertThat(endereco.getUf()).isEqualTo("SP");
        assertThat(endereco.getCep()).isEqualTo("01234567");
    }

    @Test
    @DisplayName("deve normalizar UF para uppercase")
    void deveNormalizarUfParaUppercase() {
        Endereco endereco = new Endereco("Rua A", "1", "Bairro", "Cidade", "sp", "12345678");

        assertThat(endereco.getUf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("deve remover formatação do CEP")
    void deveRemoverFormatacaoDoCep() {
        Endereco endereco = new Endereco("Rua A", "1", "Bairro", "Cidade", "SP", "01234-567");

        assertThat(endereco.getCep()).isEqualTo("01234567");
    }

    @Test
    @DisplayName("deve rejeitar logradouro null ou blank")
    void deveRejeitarLogradouroInvalido() {
        assertThatThrownBy(() -> new Endereco(null, "1", "Bairro", "Cidade", "SP", "12345678"))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> new Endereco("   ", "1", "Bairro", "Cidade", "SP", "12345678"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar UF inválido")
    void deveRejeitarUfInvalido() {
        assertThatThrownBy(() -> new Endereco("Rua", "1", "Bairro", "Cidade", "S", "12345678"))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> new Endereco("Rua", "1", "Bairro", "Cidade", "SP1", "12345678"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar CEP inválido")
    void deveRejeitarCepInvalido() {
        assertThatThrownBy(() -> new Endereco("Rua", "1", "Bairro", "Cidade", "SP", "1234"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve formatar CEP corretamente")
    void deveFormatarCepCorretamente() {
        Endereco endereco = new Endereco("Rua", "1", "Bairro", "Cidade", "SP", "01234567");

        assertThat(endereco.cepFormatted()).isEqualTo("01234-567");
    }
}
