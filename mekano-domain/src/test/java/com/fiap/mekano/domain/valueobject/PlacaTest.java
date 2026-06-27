package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Placa Value Object")
class PlacaTest {

    @Test
    @DisplayName("deve aceitar placa antiga (3 letras + 4 números)")
    void deveAceitarPlacaAntiga() {
        Placa placa = new Placa("ABC1234");

        assertThat(placa.getValue()).isEqualTo("ABC1234");
        assertThat(placa.isMercosul()).isFalse();
        assertThat(placa.formatted()).isEqualTo("ABC-1234");
    }

    @Test
    @DisplayName("deve aceitar placa Mercosul (2 letras + 4 números + 2 letras)")
    void deveAceitarPlacaMercosul() {
        Placa placa = new Placa("AB1234CD");

        assertThat(placa.getValue()).isEqualTo("AB1234CD");
        assertThat(placa.isMercosul()).isTrue();
        assertThat(placa.formatted()).isEqualTo("AB-1234-CD");
    }

    @Test
    @DisplayName("deve aceitar placa com formatação e conversão automática")
    void deveAceitarPlacaComFormatacao() {
        Placa placa = new Placa("abc-1234");

        assertThat(placa.getValue()).isEqualTo("ABC1234");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "   ", "AB12", "ABC12345"})
    @DisplayName("deve rejeitar placa inválida")
    void deveRejeitarPlacaInvalida(String valor) {
        String placa = "null".equals(valor) ? null : valor;

        assertThatThrownBy(() -> new Placa(placa))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve verificar igualdade por valor")
    void deveVerificarIgualdadePorValor() {
        Placa placa1 = new Placa("ABC1234");
        Placa placa2 = new Placa("abc-1234");

        assertThat(placa1).isEqualTo(placa2);
    }
}
