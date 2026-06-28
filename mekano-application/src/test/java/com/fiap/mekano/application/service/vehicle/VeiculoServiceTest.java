package com.fiap.mekano.application.service.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fiap.mekano.domain.event.VeiculoCriadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.in.CreateVeiculoCommand;
import com.fiap.mekano.domain.port.in.UpdateVeiculoCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;

/**
 * Cobertura de teste
 * 
 * ✅ criação com sucesso
 * ✅ cliente inexistente
 * ✅ placa duplicada
 * ✅ atualização com sucesso
 * ✅ atualização de veículo inexistente
 * ✅ atualização para placa já utilizada
 * ✅ busca por ID
 * ✅ listagem paginada
 * ✅ soft delete
 * ✅ publicação de evento
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("VeiculoService")
class VeiculoServiceTest {

    @Mock
    VeiculoRepositoryPort veiculoRepository;

    @Mock
    UserRepositoryPort clienteRepository;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    VeiculoService veiculoService;

    @Test
    @DisplayName("deve criar veículo com dados válidos")
    void deveCriarVeiculoComDadosValidos() {

        UUID clienteUuid = UUID.randomUUID();

        CreateVeiculoCommand command = new CreateVeiculoCommand(
                clienteUuid,
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        User cliente = User.reconstitute(
                clienteUuid,
                "João Silva",
                "joao@fiap.br",
                 true,
                "$2a$10$hash",
                LocalDateTime.now());

        when(clienteRepository.findById(clienteUuid))
                .thenReturn(Optional.of(cliente));

        when(veiculoRepository.existsByPlaca("ABC1234"))
                .thenReturn(false);

        when(veiculoRepository.save(any(Veiculo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Veiculo result = veiculoService.execute(command);

        assertNotNull(result);
        assertEquals("ABC1234", result.getPlaca().getValue());
        assertEquals("Toyota", result.getMarca());
        assertEquals("Corolla", result.getModelo());
        assertEquals(2020, result.getAno());

        verify(veiculoRepository, times(1))
                .save(any(Veiculo.class));

        verify(eventPublisher, times(1))
                .publish(any(VeiculoCriadoEvent.class));
    }

    @Test
    @DisplayName("deve lançar AppException(404) quando cliente não existe")
    void deveLancarExcecaoQuandoClienteNaoExiste() {

        UUID clienteUuid = UUID.randomUUID();

        CreateVeiculoCommand command = new CreateVeiculoCommand(
                clienteUuid,
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        when(clienteRepository.findById(clienteUuid))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> veiculoService.execute(command));

        verify(veiculoRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("deve lançar AppException(409) quando placa já existe")
    void deveLancarExcecaoQuandoPlacaDuplicada() {

        UUID clienteUuid = UUID.randomUUID();

        CreateVeiculoCommand command = new CreateVeiculoCommand(
                clienteUuid,
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        User cliente = User.reconstitute(
                clienteUuid,
                "Cliente",
                "cliente@fiap.br",
                true,
                "$2a$10$hash",
                LocalDateTime.now());

        when(clienteRepository.findById(clienteUuid))
                .thenReturn(Optional.of(cliente));

        when(veiculoRepository.existsByPlaca("ABC1234"))
                .thenReturn(true);

        assertThrows(
                AppException.class,
                () -> veiculoService.execute(command));

        verify(veiculoRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("deve atualizar veículo existente")
    void deveAtualizarVeiculo() {

        UUID veiculoId = UUID.randomUUID();
        UUID clienteUuid = UUID.randomUUID();

        Veiculo existente = Veiculo.reconstitute(
                veiculoId,
                clienteUuid,
                "ABC1234",
                "Toyota",
                "Corolla",
                2020,
                LocalDateTime.now());

        UpdateVeiculoCommand command = new UpdateVeiculoCommand(
                "ABC1D23",
                "Toyota",
                "Yaris",
                2022);

        when(veiculoRepository.findById(veiculoId))
                .thenReturn(Optional.of(existente));

        when(veiculoRepository.existsByPlaca("ABC1D23"))
                .thenReturn(false);

        when(veiculoRepository.save(any(Veiculo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Veiculo atualizado = veiculoService.update(
                veiculoId,
                command);

        assertEquals(
                "ABC1D23",
                atualizado.getPlaca().getValue());

        assertEquals(
                "Yaris",
                atualizado.getModelo());

        assertEquals(
                2022,
                atualizado.getAno());

        verify(veiculoRepository)
                .save(any(Veiculo.class));
    }

    @Test
    @DisplayName("deve lançar AppException(404) ao atualizar veículo inexistente")
    void deveLancarExcecaoAoAtualizarVeiculoInexistente() {

        UUID veiculoId = UUID.randomUUID();

        UpdateVeiculoCommand command = new UpdateVeiculoCommand(
                "ABC1D23",
                "Toyota",
                "Yaris",
                2022);

        when(veiculoRepository.findById(veiculoId))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> veiculoService.update(
                        veiculoId,
                        command));
    }

    @Test
    @DisplayName("deve lançar AppException(409) ao atualizar para placa já existente")
    void deveLancarExcecaoAoAtualizarParaPlacaDuplicada() {

        UUID veiculoId = UUID.randomUUID();

        Veiculo existente = Veiculo.reconstitute(
                veiculoId,
                UUID.randomUUID(),
                "ABC1234",
                "Toyota",
                "Corolla",
                2020,
                LocalDateTime.now());

        UpdateVeiculoCommand command = new UpdateVeiculoCommand(
                "XYZ9999",
                "Toyota",
                "Yaris",
                2022);

        when(veiculoRepository.findById(veiculoId))
                .thenReturn(Optional.of(existente));

        when(veiculoRepository.existsByPlaca("XYZ9999"))
                .thenReturn(true);

        assertThrows(
                AppException.class,
                () -> veiculoService.update(
                        veiculoId,
                        command));

        verify(veiculoRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("deve buscar veículo por id")
    void deveBuscarVeiculoPorId() {

        UUID veiculoId = UUID.randomUUID();

        Veiculo veiculo = Veiculo.create(
                UUID.randomUUID(),
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        when(veiculoRepository.findById(veiculoId))
                .thenReturn(Optional.of(veiculo));

        Veiculo resultado = veiculoService.findById(veiculoId);

        assertNotNull(resultado);
        assertEquals(
                "ABC1234",
                resultado.getPlaca().getValue());
    }

    @Test
    @DisplayName("deve listar veículos")
    void deveListarVeiculos() {

        when(veiculoRepository.findAll(0, 10, "placa,asc"))
                .thenReturn(List.of());

        List<Veiculo> resultado = veiculoService.findAll(
                0,
                10,
                "placa,asc");

        assertNotNull(resultado);

        verify(veiculoRepository)
                .findAll(
                        0,
                        10,
                        "placa,asc");
    }

    @Test
    @DisplayName("deve realizar soft delete")
    void deveRealizarSoftDelete() {

        UUID veiculoId = UUID.randomUUID();

        veiculoService.delete(veiculoId);

        verify(veiculoRepository)
                .markAsDeleted(veiculoId);
    }
}
