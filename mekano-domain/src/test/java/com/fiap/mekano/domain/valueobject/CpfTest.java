package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cpf Value Object")
class CpfTest {

    @Test
    @DisplayName("deve criar CPF com valor valido")
    void deveCriarCpfValido() {
        Cpf cpf = new Cpf("52998224725");
        assertEquals("52998224725", cpf.getValue());
    }

    @Test
    @DisplayName("deve aceitar CPF com formatacao e extrair somente digitos")
    void deveAceitarCpfFormatado() {
        Cpf cpf = new Cpf("529.982.247-25");
        assertEquals("52998224725", cpf.getValue());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lancar AppException(400) para null e vazio")
    void deveLancarExcecaoParaNullEVazio(String valor) {
        AppException ex = assertThrows(AppException.class, () -> new Cpf(valor));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve lancar AppException(400) para string com apenas espacos")
    void deveLancarExcecaoParaEspacos() {
        assertThrows(AppException.class, () -> new Cpf("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11111111111",
            "22222222222",
            "00000000000",
            "99999999999"
    })
    @DisplayName("deve rejeitar CPF com todos os digitos iguais")
    void deveRejeitarDigitosRepetidos(String cpfRepetido) {
        AppException ex = assertThrows(AppException.class, () -> new Cpf(cpfRepetido));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve rejeitar CPF com menos de 11 digitos")
    void deveRejeitarMenosDe11Digitos() {
        assertThrows(AppException.class, () -> new Cpf("1234567890"));
    }

    @Test
    @DisplayName("deve rejeitar CPF com mais de 11 digitos")
    void deveRejeitarMaisDe11Digitos() {
        assertThrows(AppException.class, () -> new Cpf("123456789012"));
    }

    @Test
    @DisplayName("deve rejeitar CPF com digito verificador invalido")
    void deveRejeitarDigitoVerificadorInvalido() {
        assertThrows(AppException.class, () -> new Cpf("52998224726"));
    }

    @Test
    @DisplayName("dois CPFs com mesmo valor devem ser iguais por equals")
    void doisCpfsComMesmoValorDevemSerIguais() {
        Cpf cpf1 = new Cpf("52998224725");
        Cpf cpf2 = new Cpf("52998224725");
        assertEquals(cpf1, cpf2);
        assertEquals(cpf1.hashCode(), cpf2.hashCode());
    }

    @Test
    @DisplayName("dois CPFs com valores diferentes nao devem ser iguais")
    void doisCpfsDiferentesNaoDevemSerIguais() {
        Cpf cpf1 = new Cpf("52998224725");
        Cpf cpf2 = new Cpf("39053344705");
        assertNotEquals(cpf1, cpf2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52998224725",
            "39053344705",
            "13494019193",
            "25324576190"
    })
    @DisplayName("deve aceitar CPFs validos conhecidos")
    void deveAceitarCpfsValidosConhecidos(String cpfValido) {
        assertDoesNotThrow(() -> new Cpf(cpfValido));
    }
}
