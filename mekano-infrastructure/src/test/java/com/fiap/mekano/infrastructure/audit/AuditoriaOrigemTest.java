package com.fiap.mekano.infrastructure.audit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaOrigemTest {

    @Test
    void resolver_deveRetornarUUID_quandoPrincipalEhUuidValido() {
        UUID uuid = UUID.randomUUID();
        assertThat(AuditoriaOrigem.resolver(uuid.toString())).isEqualTo(uuid);
    }

    @Test
    void resolver_deveRetornarPUBLICO_quandoPrincipalEhNulo() {
        assertThat(AuditoriaOrigem.resolver(null)).isEqualTo(AuditoriaOrigem.PUBLICO.getCodigo());
    }

    @Test
    void resolver_deveRetornarPUBLICO_quandoPrincipalEhVazio() {
        assertThat(AuditoriaOrigem.resolver("")).isEqualTo(AuditoriaOrigem.PUBLICO.getCodigo());
        assertThat(AuditoriaOrigem.resolver("   ")).isEqualTo(AuditoriaOrigem.PUBLICO.getCodigo());
    }

    @Test
    void resolver_deveRetornarPUBLICO_quandoPrincipalNaoEhUuid() {
        assertThat(AuditoriaOrigem.resolver("usuario-comum")).isEqualTo(AuditoriaOrigem.PUBLICO.getCodigo());
    }

    @Test
    void codigos_devemSerUuidValidosEDistintos() {
        assertThat(AuditoriaOrigem.PUBLICO.getCodigo()).isNotEqualTo(AuditoriaOrigem.SISTEMA.getCodigo());
        assertThat(AuditoriaOrigem.PUBLICO.getCodigo().toString()).matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        assertThat(AuditoriaOrigem.SISTEMA.getCodigo().toString()).matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }
}