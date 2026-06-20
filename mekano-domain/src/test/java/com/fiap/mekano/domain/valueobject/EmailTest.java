package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Email Value Object")
class EmailTest {

    @Test
    @DisplayName("deve criar Email com valor válido")
    void deveCriarEmailValido() {
        Email email = new Email("user@example.com");
        assertEquals("user@example.com", email.getValue());
    }

    @Test
    @DisplayName("deve normalizar email para lowercase")
    void deveNormalizarParaLowercase() {
        Email email = new Email("USER@FIAP.BR");
        assertEquals("user@fiap.br", email.getValue());
    }

    @Test
    @DisplayName("email normalizado deve ser igual a email já em lowercase")
    void emailNormalizadoDeveSerIgual() {
        Email emailMaiusculo = new Email("USER@FIAP.BR");
        Email emailMinusculo = new Email("user@fiap.br");
        assertEquals(emailMaiusculo, emailMinusculo);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lançar AppException(400) para null e vazio")
    void deveLancarExcecaoParaNullEVazio(String valor) {
        assertThrows(AppException.class, () -> new Email(valor));
    }

    @Test
    @DisplayName("deve lançar AppException(400) para string com apenas espaços")
    void deveLancarExcecaoParaEspacos() {
        assertThrows(AppException.class, () -> new Email("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "semArroba",
        "@dominio.com",
        "usuario@",
        "usuario@dominio",
        "usuario @dominio.com",
        "usuario@dominio.c"
    })
    @DisplayName("deve lançar AppException(400) para formatos inválidos")
    void deveLancarExcecaoParaFormatoInvalido(String emailInvalido) {
        assertThrows(AppException.class, () -> new Email(emailInvalido));
    }

    @Test
    @DisplayName("dois Email com mesmo valor devem ser iguais por equals")
    void doisEmailsComMesmoValorDevemSerIguais() {
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("user@example.com");
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    @DisplayName("dois Email com valores diferentes não devem ser iguais")
    void doisEmailsDiferentesNaoDevemSerIguais() {
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("outro@example.com");
        assertNotEquals(email1, email2);
    }

    @Test
    @DisplayName("deve aceitar TLD de 2 caracteres")
    void deveAceitarTldDoisCaracteres() {
        assertDoesNotThrow(() -> new Email("a@b.co"));
    }
}
