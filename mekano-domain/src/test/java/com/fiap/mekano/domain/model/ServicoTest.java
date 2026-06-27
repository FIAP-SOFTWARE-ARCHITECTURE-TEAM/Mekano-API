package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Servico (domain model)")
class ServicoTest {

    @Test
    @DisplayName("deve criar serviço com dados válidos")
    void deveCriarServicoComDadosValidos() {
        Servico servico = Servico.create("Troca de óleo", "Óleo sintético 5W30", new BigDecimal("89.90"));

        assertNotNull(servico.getId());
        assertEquals("Troca de óleo", servico.getNome());
        assertEquals("Óleo sintético 5W30", servico.getDescricao());
        assertEquals(new BigDecimal("89.90"), servico.getValor());
        assertNotNull(servico.getCreatedAt());
    }

    @Test
    @DisplayName("deve criar serviço sem descrição")
    void deveCriarServicoSemDescricao() {
        Servico servico = Servico.create("Alinhamento", null, new BigDecimal("120.00"));

        assertNull(servico.getDescricao());
    }

    @Test
    @DisplayName("deve lançar exceção quando nome é nulo")
    void deveLancarExcecaoQuandoNomeNulo() {
        AppException ex = assertThrows(AppException.class,
                () -> Servico.create(null, "desc", new BigDecimal("10.00")));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve lançar exceção quando nome é vazio")
    void deveLancarExcecaoQuandoNomeVazio() {
        AppException ex = assertThrows(AppException.class,
                () -> Servico.create("  ", "desc", new BigDecimal("10.00")));
        assertEquals(400, ex.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    @DisplayName("deve lançar exceção quando valor <= 0")
    void deveLancarExcecaoQuandoValorInvalido(String valorStr) {
        AppException ex = assertThrows(AppException.class,
                () -> Servico.create("Serviço", "desc", new BigDecimal(valorStr)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve lançar exceção quando valor é nulo")
    void deveLancarExcecaoQuandoValorNulo() {
        AppException ex = assertThrows(AppException.class,
                () -> Servico.create("Serviço", "desc", null));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve reconstituir serviço preservando valores originais")
    void deveReconstituirServico() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        Servico servico = Servico.reconstitute(id, "Balanceamento", "4 rodas",
                new BigDecimal("60.00"), createdAt);

        assertEquals(id, servico.getId());
        assertEquals("Balanceamento", servico.getNome());
        assertEquals("4 rodas", servico.getDescricao());
        assertEquals(new BigDecimal("60.00"), servico.getValor());
        assertEquals(createdAt, servico.getCreatedAt());
    }

    @Test
    @DisplayName("deve atualizar serviço com dados válidos")
    void deveAtualizarServico() {
        Servico servico = Servico.create("Troca de óleo", "desc", new BigDecimal("89.90"));

        servico.atualizar("Troca de óleo sintético", "Óleo 5W30 premium", new BigDecimal("129.90"));

        assertEquals("Troca de óleo sintético", servico.getNome());
        assertEquals("Óleo 5W30 premium", servico.getDescricao());
        assertEquals(new BigDecimal("129.90"), servico.getValor());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar com valor <= 0")
    void deveLancarExcecaoAoAtualizarComValorInvalido() {
        Servico servico = Servico.create("Troca de óleo", "desc", new BigDecimal("89.90"));

        AppException ex = assertThrows(AppException.class,
                () -> servico.atualizar("Troca", "desc", BigDecimal.ZERO));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar com nome vazio")
    void deveLancarExcecaoAoAtualizarComNomeVazio() {
        Servico servico = Servico.create("Troca de óleo", "desc", new BigDecimal("89.90"));

        AppException ex = assertThrows(AppException.class,
                () -> servico.atualizar("", "desc", new BigDecimal("50.00")));
        assertEquals(400, ex.getStatus());
    }
}
