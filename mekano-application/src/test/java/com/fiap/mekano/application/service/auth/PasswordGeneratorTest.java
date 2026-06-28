package com.fiap.mekano.application.service.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    @Test
    void generate_deveGerarSenhaComNoMinimo12CaracteresEComplexidade() {
        String password = PasswordGenerator.generate();

        assertNotNull(password);
        assertTrue(password.length() >= 12);
        assertTrue(password.matches(".*[A-Z].*"), "Deve conter letra maiúscula");
        assertTrue(password.matches(".*[a-z].*"), "Deve conter letra minúscula");
        assertTrue(password.matches(".*\\d.*"), "Deve conter dígito");
        assertTrue(password.matches(".*[!@#$%&*()\\-_=+\\[\\]{}].*"), "Deve conter símbolo");
    }

    @Test
    void generate_deveGerarSenhasDiferentes() {
        String password1 = PasswordGenerator.generate();
        String password2 = PasswordGenerator.generate();

        assertNotEquals(password1, password2);
    }

    @Test
    void generate_quandoTamanhoMenorQue12_deveLancarErro() {
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generate(8));
    }
}
