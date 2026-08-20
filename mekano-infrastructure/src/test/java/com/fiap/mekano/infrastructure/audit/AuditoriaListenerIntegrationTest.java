package com.fiap.mekano.infrastructure.audit;

import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.infrastructure.entity.ServicoEntity;
import com.fiap.mekano.infrastructure.repository.ServicoPanacheRepository;
import com.fiap.mekano.infrastructure.repository.ServicoRepositoryImpl;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica o preenchimento automático de {@code createdBy}/{@code updatedBy} pelo
 * {@link AuditoriaListener} durante um request sem usuário autenticado
 * (request context ativo, principal ausente) → {@link AuditoriaOrigem#PUBLICO}.
 */
@QuarkusTest
class AuditoriaListenerIntegrationTest {

    @Inject
    ServicoRepositoryImpl repository;

    @Inject
    ServicoPanacheRepository panacheRepository;

    @Test
    @TestTransaction
    void persistirSemUsuarioAutenticado_devePreencherCreatedByComPUBLICO() {
        Servico servico = Servico.create("Serviço Auditoria", "verificação de auditoria", new BigDecimal("50.00"));

        Servico salvo = repository.save(servico);

        ServicoEntity entity = panacheRepository.find("uuid = ?1", salvo.getId()).firstResult();
        assertThat(entity).isNotNull();
        assertThat(entity.getCreatedBy()).isEqualTo(AuditoriaOrigem.PUBLICO.getCodigo());
        assertThat(entity.getUpdatedBy()).isNull();
    }
}