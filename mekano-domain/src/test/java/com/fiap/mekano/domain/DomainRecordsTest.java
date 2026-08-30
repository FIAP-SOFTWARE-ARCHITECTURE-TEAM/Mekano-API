package com.fiap.mekano.domain;

import com.fiap.mekano.domain.event.CobrancaEmitidaEvent;
import com.fiap.mekano.domain.event.ClienteCriadoEvent;
import com.fiap.mekano.domain.event.OSEntregueEvent;
import com.fiap.mekano.domain.event.UserCreatedEvent;
import com.fiap.mekano.domain.model.*;
import com.fiap.mekano.domain.port.in.*;
import com.fiap.mekano.domain.valueobject.Endereco;
import com.fiap.mekano.domain.valueobject.Cpf;
import com.fiap.mekano.domain.valueobject.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Records — construtores e factory methods")
class DomainRecordsTest {

    @Test
    @DisplayName("OSEntregueEvent.of deve criar record com dataEntrega")
    void osEntregueEventOf() {
        UUID osUuid = UUID.randomUUID();
        OSEntregueEvent event = OSEntregueEvent.of(osUuid, "Recebido por João");

        assertThat(event.osUuid()).isEqualTo(osUuid);
        assertThat(event.observacao()).isEqualTo("Recebido por João");
        assertThat(event.dataEntrega()).isNotNull();
    }

    @Test
    @DisplayName("OSEntregueEvent constructor direto")
    void osEntregueEventConstructor() {
        UUID osUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        OSEntregueEvent event = new OSEntregueEvent(osUuid, now, "obs");

        assertThat(event.osUuid()).isEqualTo(osUuid);
        assertThat(event.dataEntrega()).isEqualTo(now);
        assertThat(event.observacao()).isEqualTo("obs");
    }

    @Test
    @DisplayName("CobrancaEmitidaEvent.of deve criar record com cobrancaId random")
    void cobrancaEmitidaEventOf() {
        UUID osUuid = UUID.randomUUID();
        BigDecimal valor = new BigDecimal("1500.00");
        CobrancaEmitidaEvent event = CobrancaEmitidaEvent.of(osUuid, valor);

        assertThat(event.osUuid()).isEqualTo(osUuid);
        assertThat(event.valor()).isEqualByComparingTo(valor);
        assertThat(event.cobrancaId()).isNotNull();
        assertThat(event.dataEmissao()).isNotNull();
    }

    @Test
    @DisplayName("CobrancaEmitidaEvent constructor direto")
    void cobrancaEmitidaEventConstructor() {
        UUID osUuid = UUID.randomUUID();
        UUID cobrancaId = UUID.randomUUID();
        BigDecimal valor = new BigDecimal("500.00");
        LocalDateTime now = LocalDateTime.now();
        CobrancaEmitidaEvent event = new CobrancaEmitidaEvent(osUuid, cobrancaId, valor, now);

        assertThat(event.osUuid()).isEqualTo(osUuid);
        assertThat(event.cobrancaId()).isEqualTo(cobrancaId);
        assertThat(event.valor()).isEqualByComparingTo(valor);
        assertThat(event.dataEmissao()).isEqualTo(now);
    }

    @Test
    @DisplayName("ClienteCriadoEvent.of deve criar record com occurredAt")
    void clienteCriadoEventOf() {
        Cliente cliente = Cliente.create("Maria", "52998224725", "maria@test.com",
                "11999999999", "Rua A", "100", "Centro", "São Paulo", "SP", "01001000");
        ClienteCriadoEvent event = ClienteCriadoEvent.of(cliente);

        assertThat(event.cliente()).isEqualTo(cliente);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("ClienteCriadoEvent constructor direto")
    void clienteCriadoEventConstructor() {
        Cliente cliente = Cliente.create("Maria", "52998224725", "maria@test.com",
                "11999999999", "Rua A", "100", "Centro", "São Paulo", "SP", "01001000");
        LocalDateTime now = LocalDateTime.now();
        ClienteCriadoEvent event = new ClienteCriadoEvent(cliente, now);

        assertThat(event.cliente()).isEqualTo(cliente);
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UserCreatedEvent.of deve criar record com occurredAt")
    void userCreatedEventOf() {
        User user = User.create("João", "joao@test.com", true, "senha123");
        UserCreatedEvent event = UserCreatedEvent.of(user);

        assertThat(event.user()).isEqualTo(user);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("UserCreatedEvent constructor direto")
    void userCreatedEventConstructor() {
        User user = User.create("João", "joao@test.com", true, "senha123");
        LocalDateTime now = LocalDateTime.now();
        UserCreatedEvent event = new UserCreatedEvent(user, now);

        assertThat(event.user()).isEqualTo(user);
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("CriarOSCommand record")
    void criarOSCommand() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        CriarOSCommand cmd = new CriarOSCommand(clienteId, veiculoId, "Freio com problema");

        assertThat(cmd.clienteId()).isEqualTo(clienteId);
        assertThat(cmd.veiculoId()).isEqualTo(veiculoId);
        assertThat(cmd.descricaoProblema()).isEqualTo("Freio com problema");
    }

    @Test
    @DisplayName("IniciarExecucaoCommand record")
    void iniciarExecucaoCommand() {
        UUID osUuid = UUID.randomUUID();
        UUID mecanicoUuid = UUID.randomUUID();
        IniciarExecucaoCommand cmd = new IniciarExecucaoCommand(osUuid, mecanicoUuid, "Iniciando");

        assertThat(cmd.osUuid()).isEqualTo(osUuid);
        assertThat(cmd.mecanicoUuid()).isEqualTo(mecanicoUuid);
        assertThat(cmd.observacao()).isEqualTo("Iniciando");
    }

    @Test
    @DisplayName("FinalizarExecucaoCommand record")
    void finalizarExecucaoCommand() {
        UUID osUuid = UUID.randomUUID();
        FinalizarExecucaoCommand cmd = new FinalizarExecucaoCommand(osUuid, "Concluído");

        assertThat(cmd.osUuid()).isEqualTo(osUuid);
        assertThat(cmd.observacao()).isEqualTo("Concluído");
    }

    @Test
    @DisplayName("CancelarOSCommand record")
    void cancelarOSCommand() {
        UUID osUuid = UUID.randomUUID();
        CancelarOSCommand cmd = new CancelarOSCommand(osUuid, "Cliente desistiu");

        assertThat(cmd.osUuid()).isEqualTo(osUuid);
        assertThat(cmd.motivo()).isEqualTo("Cliente desistiu");
    }

    @Test
    @DisplayName("OSSummary record")
    void osSummary() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID mecanicoUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        OSSummary summary = new OSSummary(id, clienteId, veiculoId, "Problema X",
                StatusOS.EM_EXECUCAO, mecanicoUuid, now);

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.clienteId()).isEqualTo(clienteId);
        assertThat(summary.veiculoId()).isEqualTo(veiculoId);
        assertThat(summary.descricaoProblema()).isEqualTo("Problema X");
        assertThat(summary.status()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(summary.mecanicoUuid()).isEqualTo(mecanicoUuid);
        assertThat(summary.createdAt()).isEqualTo(now);
    }
}
