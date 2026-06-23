package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Telefone Value Object")
class TelefoneTest {

    @Test
    @DisplayName("deve criar Telefone com celular valido (11 digitos)")
    void deveCriarTelefoneCelularValido() {
        Telefone tel = new Telefone("11999887766");
        assertEquals("11999887766", tel.getValue());
    }

    @Test
    @DisplayName("deve criar Telefone com fixo valido (10 digitos)")
    void deveCriarTelefoneFixoValido() {
        Telefone tel = new Telefone("1133445566");
        assertEquals("1133445566", tel.getValue());
    }

    @Test
    @DisplayName("deve remover caracteres nao numericos")
    void deveRemoverCaracteresNaoNumericos() {
        Telefone tel = new Telefone("(11) 99988-7766");
        assertEquals("11999887766", tel.getValue());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lancar AppException(400) para null e vazio")
    void deveLancarExcecaoParaNullEVazio(String valor) {
        AppException ex = assertThrows(AppException.class, () -> new Telefone(valor));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve rejeitar telefone com menos de 10 digitos")
    void deveRejeitarMenosDe10Digitos() {
        assertThrows(AppException.class, () -> new Telefone("123456789"));
    }

    @Test
    @DisplayName("deve rejeitar telefone com mais de 11 digitos")
    void deveRejeitarMaisDe11Digitos() {
        assertThrows(AppException.class, () -> new Telefone("123456789012"));
    }

    @Test
    @DisplayName("dois Telefones com mesmo valor devem ser iguais por equals")
    void doisTelefonesComMesmoValorDevemSerIguais() {
        Telefone tel1 = new Telefone("11999887766");
        Telefone tel2 = new Telefone("11999887766");
        assertEquals(tel1, tel2);
        assertEquals(tel1.hashCode(), tel2.hashCode());
    }

    @Test
    @DisplayName("dois Telefones com valores diferentes nao devem ser iguais")
    void doisTelefonesDiferentesNaoDevemSerIguais() {
        Telefone tel1 = new Telefone("11999887766");
        Telefone tel2 = new Telefone("21988776655");
        assertNotEquals(tel1, tel2);
    }
}
