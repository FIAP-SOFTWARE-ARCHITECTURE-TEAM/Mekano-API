package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ==== PlacaVeiculoTest ====
 * 
 * cria placa antiga válida (ABC1234)
 * cria placa Mercosul válida (ABC1D23)
 * normaliza para uppercase (abc1234 -> ABC1234)
 * remove hífen (ABC-1234 -> ABC1234)
 * duas placas equivalentes são iguais por equals()
 * lança AppException(400) para null e vazio
 * lança AppException(400) para espaços
 * lança AppException(400) para formatos inválidos
 */

@DisplayName("PlacaVeiculo Value Object")
class PlacaVeiculoTest {

    @Test
    @DisplayName("deve criar placa antiga válida")
    void deveCriarPlacaAntigaValida() {
        PlacaVeiculo placa = new PlacaVeiculo("ABC1234");
        assertEquals("ABC1234", placa.getValue());
    }

    @Test
    @DisplayName("deve criar placa Mercosul válida")
    void deveCriarPlacaMercosulValida() {
        PlacaVeiculo placa = new PlacaVeiculo("ABC1D23");
        assertEquals("ABC1D23", placa.getValue());
    }

    @Test
    @DisplayName("deve normalizar placa para uppercase")
    void deveNormalizarParaUppercase() {
        PlacaVeiculo placa = new PlacaVeiculo("abc1234");
        assertEquals("ABC1234", placa.getValue());
    }

    @Test
    @DisplayName("deve remover hífen da placa")
    void deveRemoverHifen() {
        PlacaVeiculo placa = new PlacaVeiculo("ABC-1234");
        assertEquals("ABC1234", placa.getValue());
    }

    @Test
    @DisplayName("placas equivalentes devem ser iguais")
    void placasEquivalentesDevemSerIguais() {
        PlacaVeiculo placa1 = new PlacaVeiculo("ABC1234");
        PlacaVeiculo placa2 = new PlacaVeiculo("abc-1234");
        assertEquals(placa1, placa2);
        assertEquals(placa1.hashCode(), placa2.hashCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lançar AppException(400) para null e vazio")
    void deveLancarExcecaoParaNullEVazio(String valor) {
        assertThrows(AppException.class, () -> new PlacaVeiculo(valor));
    }

    @Test
    @DisplayName("deve lançar AppException(400) para espaços")
    void deveLancarExcecaoParaEspacos() {
        assertThrows(AppException.class, () -> new PlacaVeiculo(" "));
    }

    @ParameterizedTest
    @ValueSource(strings = { "INVALID", "AB12345", "ABCD123", "ABC12D3", "1234567", "AAA111", "AAAA1111" })
    @DisplayName("deve lançar AppException(400) para formatos inválidos")
    void deveLancarExcecaoParaFormatoInvalido(String placaInvalida) {
        assertThrows(AppException.class, () -> new PlacaVeiculo(placaInvalida));
    }

}
