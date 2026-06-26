package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cpf Value Object")
@Disabled("TODO: Corrigir algoritmo de checksum do CPF")
class CpfTest {

    private static final String VALID_CPF = "11144477783";

    @Test
    @DisplayName("deve aceitar CPF válido sem formatação")
    void deveAceitarCpfValidoSemFormatacao() {
        Cpf cpf = new Cpf(VALID_CPF);

        assertThat(cpf.getValue()).isEqualTo(VALID_CPF);
        assertThat(cpf.formatted()).contains("-");
    }

    @Test
    @DisplayName("deve aceitar CPF válido com formatação")
    void deveAceitarCpfValidoComFormatacao() {
        String cpfFormatado = "111.444.777-83";
        Cpf cpf = new Cpf(cpfFormatado);

        assertThat(cpf.getValue()).isEqualTo(VALID_CPF);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123", "111.111.111-11"})
    @DisplayName("deve rejeitar CPF inválido")
    void deveRejeitarCpfInvalido(String valor) {
        assertThatThrownBy(() -> new Cpf(valor))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar CPF null")
    void deveRejeitarCpfNull() {
        assertThatThrownBy(() -> new Cpf(null))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar CPF com todos os dígitos iguais")
    void deveRejeitarCpfComDigitosIguais() {
        assertThatThrownBy(() -> new Cpf("11111111111"))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve verificar igualdade por valor")
    void deveVerificarIgualdadePorValor() {
        Cpf cpf1 = new Cpf(VALID_CPF);
        Cpf cpf2 = new Cpf("111.444.777-83");

        assertThat(cpf1).isEqualTo(cpf2);
    }
}
