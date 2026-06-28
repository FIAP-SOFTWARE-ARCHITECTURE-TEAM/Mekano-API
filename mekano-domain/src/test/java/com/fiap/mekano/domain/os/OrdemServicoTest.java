package com.fiap.mekano.domain.os;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrdemServicoTest {

    @Test
    @DisplayName("Deve criar nova OS com status ABERTA")
    void deveCriarNovaOsAberta() {
        OrdemServico os = OrdemServico.criarNova();

        assertNotNull(os.getUuid());
        assertEquals(OsStatus.ABERTA, os.getStatus());
    }

    @Test
    @DisplayName("Deve restaurar OS preservando UUID e status")
    void deveRestaurarOs() {
        UUID uuid = UUID.randomUUID();

        OrdemServico os = OrdemServico.restaurar(uuid, OsStatus.APROVADA);

        assertEquals(uuid, os.getUuid());
        assertEquals(OsStatus.APROVADA, os.getStatus());
    }

    @Test
    @DisplayName("Deve executar fluxo completo até FINALIZADA")
    void deveExecutarFluxoCompleto() {
        OrdemServico os = OrdemServico.criarNova();

        os.diagnosticar();
        os.orcar();
        os.aprovar();
        os.executar();
        os.finalizar();

        assertEquals(OsStatus.FINALIZADA, os.getStatus());
    }

    @Test
    @DisplayName("Não deve orçar OS antes de diagnosticar")
    void naoDeveOrcarAntesDeDiagnosticar() {
        OrdemServico os = OrdemServico.criarNova();

        IllegalStateException exception = assertThrows(IllegalStateException.class, os::orcar);

        assertTrue(exception.getMessage().contains("Transição inválida"));
        assertEquals(OsStatus.ABERTA, os.getStatus());
    }

    @Test
    @DisplayName("Não deve aprovar OS antes de orçar")
    void naoDeveAprovarAntesDeOrcar() {
        OrdemServico os = OrdemServico.restaurar(UUID.randomUUID(), OsStatus.DIAGNOSTICADA);

        IllegalStateException exception = assertThrows(IllegalStateException.class, os::aprovar);

        assertTrue(exception.getMessage().contains("Transição inválida"));
        assertEquals(OsStatus.DIAGNOSTICADA, os.getStatus());
    }

    @Test
    @DisplayName("Deve cancelar OS não finalizada")
    void deveCancelarOsNaoFinalizada() {
        OrdemServico os = OrdemServico.restaurar(UUID.randomUUID(), OsStatus.APROVADA);

        os.cancelar();

        assertEquals(OsStatus.CANCELADA, os.getStatus());
    }

    @Test
    @DisplayName("Não deve cancelar OS finalizada")
    void naoDeveCancelarOsFinalizada() {
        OrdemServico os = OrdemServico.restaurar(UUID.randomUUID(), OsStatus.FINALIZADA);

        IllegalStateException exception = assertThrows(IllegalStateException.class, os::cancelar);

        assertTrue(exception.getMessage().contains("finalizada"));
        assertEquals(OsStatus.FINALIZADA, os.getStatus());
    }

    @Test
    @DisplayName("Não deve cancelar OS já cancelada")
    void naoDeveCancelarOsJaCancelada() {
        OrdemServico os = OrdemServico.restaurar(UUID.randomUUID(), OsStatus.CANCELADA);

        IllegalStateException exception = assertThrows(IllegalStateException.class, os::cancelar);

        assertTrue(exception.getMessage().contains("cancelada"));
        assertEquals(OsStatus.CANCELADA, os.getStatus());
    }
}
