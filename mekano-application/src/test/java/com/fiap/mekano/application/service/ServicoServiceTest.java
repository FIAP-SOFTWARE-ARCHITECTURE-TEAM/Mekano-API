package com.fiap.mekano.application.service;

import com.fiap.mekano.application.service.ServicoService;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.port.in.CreateServicoCommand;
import com.fiap.mekano.domain.port.in.UpdateServicoCommand;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServicoService")
class ServicoServiceTest {

    @Mock
    ServicoRepositoryPort servicoRepository;

    @InjectMocks
    ServicoService servicoService;

    @Test
    @DisplayName("deve criar serviço com dados válidos")
    void deveCriarServicoComDadosValidos() {
        var command = new CreateServicoCommand("Troca de óleo", "Óleo sintético", new BigDecimal("89.90"));
        when(servicoRepository.existsByNome("Troca de óleo")).thenReturn(false);
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        Servico result = servicoService.create(command);

        assertNotNull(result);
        assertEquals("Troca de óleo", result.getNome());
        assertEquals(new BigDecimal("89.90"), result.getValor());
        verify(servicoRepository, times(1)).save(any(Servico.class));
    }

    @Test
    @DisplayName("deve lançar AppException(409) quando nome duplicado")
    void deveLancarExcecaoQuandoNomeDuplicado() {
        var command = new CreateServicoCommand(" Troca de óleo ", "desc", new BigDecimal("89.90"));
        when(servicoRepository.existsByNome("Troca de óleo")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> servicoService.create(command));
        assertEquals(409, ex.getStatus());
        verify(servicoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar AppException(400) quando valor é zero")
    void deveLancarExcecaoQuandoValorZero() {
        var command = new CreateServicoCommand("Serviço", "desc", BigDecimal.ZERO);
        when(servicoRepository.existsByNome("Serviço")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> servicoService.create(command));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve lançar AppException(400) quando valor é negativo")
    void deveLancarExcecaoQuandoValorNegativo() {
        var command = new CreateServicoCommand("Serviço", "desc", new BigDecimal("-10.00"));
        when(servicoRepository.existsByNome("Serviço")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> servicoService.create(command));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("deve atualizar serviço existente")
    void deveAtualizarServicoExistente() {
        UUID id = UUID.randomUUID();
        Servico existing = Servico.create("Troca de óleo", "desc", new BigDecimal("89.90"));
        var command = new UpdateServicoCommand("Troca de óleo sintético", "premium", new BigDecimal("129.90"));

        when(servicoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(servicoRepository.existsByNomeAndIdNot("Troca de óleo sintético", id)).thenReturn(false);
        when(servicoRepository.save(any(Servico.class))).thenAnswer(inv -> inv.getArgument(0));

        Servico result = servicoService.update(id, command);

        assertEquals("Troca de óleo sintético", result.getNome());
        assertEquals(new BigDecimal("129.90"), result.getValor());
    }

    @Test
    @DisplayName("deve lançar AppException(404) quando serviço não existe para update")
    void deveLancarExcecaoQuandoServicoNaoExisteParaUpdate() {
        UUID id = UUID.randomUUID();
        var command = new UpdateServicoCommand("Troca", "desc", new BigDecimal("50.00"));
        when(servicoRepository.findById(id)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> servicoService.update(id, command));
        assertEquals(404, ex.getStatus());
    }

    @Test
    @DisplayName("deve lançar AppException(409) quando nome duplicado no update")
    void deveLancarExcecaoQuandoNomeDuplicadoNoUpdate() {
        UUID id = UUID.randomUUID();
        Servico existing = Servico.create("Troca de óleo", "desc", new BigDecimal("89.90"));
        var command = new UpdateServicoCommand(" Alinhamento ", "desc", new BigDecimal("100.00"));

        when(servicoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(servicoRepository.existsByNomeAndIdNot("Alinhamento", id)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> servicoService.update(id, command));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("deve buscar serviço por id")
    void deveBuscarServicoPorId() {
        UUID id = UUID.randomUUID();
        Servico servico = Servico.create("Troca", "desc", new BigDecimal("50.00"));
        when(servicoRepository.findById(id)).thenReturn(Optional.of(servico));

        Servico result = servicoService.findById(id);

        assertNotNull(result);
        assertEquals("Troca", result.getNome());
    }

    @Test
    @DisplayName("deve lançar AppException(404) quando serviço não encontrado")
    void deveLancarExcecaoQuandoServicoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.findById(id)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> servicoService.findById(id));
        assertEquals(404, ex.getStatus());
    }

    @Test
    @DisplayName("deve deletar serviço")
    void deveDeletarServico() {
        UUID id = UUID.randomUUID();

        servicoService.delete(id);

        verify(servicoRepository, times(1)).markAsDeleted(id);
    }
}
