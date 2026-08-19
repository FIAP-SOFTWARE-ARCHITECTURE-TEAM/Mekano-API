package com.fiap.mekano.application.service.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.in.CreateClienteCommand;
import com.fiap.mekano.domain.port.in.UpdateClienteCommand;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock
    ClienteRepositoryPort clienteRepository;

    @Mock
    EventPublisher eventPublisher;

    @Mock
    OrdemDeServicoRepositoryPort osRepository;

    @InjectMocks
    ClienteService clienteService;

    @Captor
    ArgumentCaptor<Cliente> clienteCaptor;

    private static final UUID CLIENTE_UUID = UUID.randomUUID();
    private static final String CPF_VALIDO = "52998224725";
    private static final LocalDateTime CREATED_AT = LocalDateTime.now().minusDays(30);

    private Cliente mockClienteExistente() {
        return Cliente.reconstitute(
                CLIENTE_UUID,
                "João Silva",
                CPF_VALIDO,
                "joao@fiap.br",
                "11999999999",
                "Rua A",
                "100",
                "Centro",
                "São Paulo",
                "SP",
                "01001000",
                CREATED_AT
        );
    }

    @Test
    @DisplayName("updateCliente deve aplicar campos do comando preservando CPF/id/createdAt")
    void updateCliente_aplicaCamposDoComando() {
        // Arrange
        Cliente existente = mockClienteExistente();
        when(clienteRepository.findById(CLIENTE_UUID))
                .thenReturn(Optional.of(existente));

        when(clienteRepository.update(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClienteCommand command = new UpdateClienteCommand(
                "João Silva Atualizado",
                "joao.novo@fiap.br",
                "11988888888",
                "Rua B",
                "200",
                "Vila Nova",
                "Campinas",
                "SP",
                "02002000"
        );

        // Act
        Cliente resultado = clienteService.updateCliente(CLIENTE_UUID, command);

        // Assert
        assertNotNull(resultado);
        verify(clienteRepository, times(1)).update(clienteCaptor.capture());

        Cliente capturado = clienteCaptor.getValue();
        assertEquals(CLIENTE_UUID, capturado.getId());
        assertEquals("João Silva Atualizado", capturado.getNome());
        assertEquals("joao.novo@fiap.br", capturado.getEmail().getValue());
        assertEquals(CPF_VALIDO, capturado.getCpf().getValue());
        assertEquals(CREATED_AT, capturado.getCreatedAt());
        assertEquals("Rua B", capturado.getEndereco().getLogradouro());
        assertEquals("200", capturado.getEndereco().getNumero());
        assertEquals("Vila Nova", capturado.getEndereco().getBairro());
        assertEquals("Campinas", capturado.getEndereco().getCidade());
        assertEquals("SP", capturado.getEndereco().getUf());
        assertEquals("02002000", capturado.getEndereco().getCep());
    }

    @Test
    @DisplayName("updateCliente deve lançar AppException(404) quando cliente não existe")
    void updateCliente_naoEncontrado_lanca404() {
        // Arrange
        when(clienteRepository.findById(CLIENTE_UUID))
                .thenReturn(Optional.empty());

        UpdateClienteCommand command = new UpdateClienteCommand(
                "Qualquer Nome",
                "qualquer@email.com",
                null,
                "Rua X",
                "1",
                "Bairro",
                "Cidade",
                "UF",
                "00000000"
        );

        // Act & Assert
        AppException ex = assertThrows(
                AppException.class,
                () -> clienteService.updateCliente(CLIENTE_UUID, command)
        );

        assertEquals(404, ex.getStatus());
        verify(clienteRepository, never()).update(any());
    }

    @Test
    @DisplayName("execute deve lançar AppException(409) quando CPF já existe")
    void execute_comCpfExistente_deveLancar409() {
        CreateClienteCommand command = new CreateClienteCommand(
                "João Silva",
                CPF_VALIDO,
                "joao@fiap.br",
                "11999999999",
                "Rua A",
                "100",
                "Centro",
                "São Paulo",
                "SP",
                "01001000"
        );

        when(clienteRepository.existsByCpf(CPF_VALIDO))
                .thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> clienteService.execute(command)
        );

        assertEquals(409, ex.getStatus());
        verify(clienteRepository, never()).create(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("deleteCliente deve delegar ao repository")
    void deleteCliente_deveDelegarAoRepository() {
        clienteService.deleteCliente(CLIENTE_UUID);

        verify(clienteRepository, times(1)).markAsDeleted(CLIENTE_UUID);
    }

    @Test
    @DisplayName("deleteCliente deve lançar AppException(409) quando há OS ativa")
    void deleteCliente_comOsAtiva_deveLancar409() {
        when(osRepository.existsByClienteUuidAndStatusIn(CLIENTE_UUID, java.util.List.of("EM_EXECUCAO", "AGUARDANDO_APROVACAO")))
                .thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> clienteService.deleteCliente(CLIENTE_UUID)
        );

        assertEquals(409, ex.getStatus());
        verify(clienteRepository, never()).markAsDeleted(any());
    }

    @Test
    @DisplayName("reactivate deve delegar ao repository")
    void reactivate_deveDelegarAoRepository() {
        clienteService.reactivate(CLIENTE_UUID);

        verify(clienteRepository, times(1)).reactivate(CLIENTE_UUID);
    }
}