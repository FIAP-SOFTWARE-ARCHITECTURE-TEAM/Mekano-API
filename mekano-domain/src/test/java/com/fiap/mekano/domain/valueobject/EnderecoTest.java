package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Endereco Value Object")
class EnderecoTest {

    @Test
    @DisplayName("deve criar Endereco com todos os campos validos")
    void deveCriarEnderecoValido() {
        Endereco endereco = new Endereco("Rua das Flores", "123", "Centro",
                "Sao Paulo", "SP", "01001000");

        assertEquals("Rua das Flores", endereco.getLogradouro());
        assertEquals("123", endereco.getNumero());
        assertEquals("Centro", endereco.getBairro());
        assertEquals("Sao Paulo", endereco.getCidade());
        assertEquals("SP", endereco.getUf());
        assertEquals("01001000", endereco.getCep());
    }

    @Test
    @DisplayName("deve normalizar UF para uppercase")
    void deveNormalizarUfParaUppercase() {
        Endereco endereco = new Endereco("Rua A", "1", "B", "C", "sp", "01001000");
        assertEquals("SP", endereco.getUf());
    }

    @Test
    @DisplayName("deve remover caracteres nao numericos do CEP")
    void deveRemoverNaoNumericosDoCep() {
        Endereco endereco = new Endereco("Rua A", "1", "B", "C", "SP", "01001-000");
        assertEquals("01001000", endereco.getCep());
    }

    @Test
    @DisplayName("deve aceitar numero e bairro nulos")
    void deveAceitarNumeroEBairroNulos() {
        Endereco endereco = new Endereco("Rua A", null, null, "Cidade", "SP", "01001000");
        assertNull(endereco.getNumero());
        assertNull(endereco.getBairro());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lancar AppException(400) para logradouro null ou vazio")
    void deveLancarExcecaoParaLogradouroInvalido(String logradouro) {
        AppException ex = assertThrows(AppException.class,
                () -> new Endereco(logradouro, "1", "B", "C", "SP", "01001000"));
        assertEquals(400, ex.getStatus());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lancar AppException(400) para cidade null ou vazia")
    void deveLancarExcecaoParaCidadeInvalida(String cidade) {
        AppException ex = assertThrows(AppException.class,
                () -> new Endereco("Rua A", "1", "B", cidade, "SP", "01001000"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve lancar AppException(400) para UF com tamanho diferente de 2")
    void deveLancarExcecaoParaUfInvalida() {
        assertThrows(AppException.class,
                () -> new Endereco("Rua A", "1", "B", "C", "SPP", "01001000"));
    }

    @Test
    @DisplayName("deve lancar AppException(400) para UF null")
    void deveLancarExcecaoParaUfNull() {
        assertThrows(AppException.class,
                () -> new Endereco("Rua A", "1", "B", "C", null, "01001000"));
    }

    @Test
    @DisplayName("deve lancar AppException(400) para CEP com tamanho diferente de 8")
    void deveLancarExcecaoParaCepInvalido() {
        assertThrows(AppException.class,
                () -> new Endereco("Rua A", "1", "B", "C", "SP", "0100100"));
    }

    @Test
    @DisplayName("deve lancar AppException(400) para CEP null")
    void deveLancarExcecaoParaCepNull() {
        assertThrows(AppException.class,
                () -> new Endereco("Rua A", "1", "B", "C", "SP", null));
    }

    @Test
    @DisplayName("dois Enderecos com mesmos valores devem ser iguais por equals")
    void doisEnderecosComMesmosValoresDevemSerIguais() {
        Endereco e1 = new Endereco("Rua A", "1", "B", "C", "SP", "01001000");
        Endereco e2 = new Endereco("Rua A", "1", "B", "C", "SP", "01001000");
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
