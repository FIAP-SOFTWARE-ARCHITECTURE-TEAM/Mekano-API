package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.os.OrdemServico;
import com.fiap.mekano.domain.os.OsStatus;
import com.fiap.mekano.infrastructure.entity.OrdemServicoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrdemServicoRepositoryImplTest {

    @Test
    @DisplayName("Deve persistir nova ordem de serviço")
    void devePersistirNovaOrdemDeServico() {
        OrdemServicoPanacheRepository panacheRepository = mock(OrdemServicoPanacheRepository.class);

        OrdemServicoRepositoryImpl repository = new OrdemServicoRepositoryImpl();
        repository.panacheRepository = panacheRepository;

        OrdemServico os = OrdemServico.restaurar(UUID.randomUUID(), OsStatus.RECEBIDA);

        when(panacheRepository.findByUuid(os.getUuid())).thenReturn(Optional.empty());

        OrdemServico result = repository.save(os);

        ArgumentCaptor<OrdemServicoEntity> captor = ArgumentCaptor.forClass(OrdemServicoEntity.class);

        verify(panacheRepository).persist(captor.capture());

        OrdemServicoEntity entity = captor.getValue();

        assertEquals(os.getUuid(), entity.uuid);
        assertEquals("ABERTA", entity.status);
        assertEquals(os.getUuid(), result.getUuid());
        assertEquals(OsStatus.RECEBIDA, result.getStatus());
    }

    @Test
    @DisplayName("Deve atualizar ordem de serviço existente")
    void deveAtualizarOrdemDeServicoExistente() {
        OrdemServicoPanacheRepository panacheRepository = mock(OrdemServicoPanacheRepository.class);

        OrdemServicoRepositoryImpl repository = new OrdemServicoRepositoryImpl();
        repository.panacheRepository = panacheRepository;

        UUID osUuid = UUID.randomUUID();

        OrdemServicoEntity entity = new OrdemServicoEntity();
        entity.uuid = osUuid;
        entity.status = "ABERTA";

        when(panacheRepository.findByUuid(osUuid)).thenReturn(Optional.of(entity));

        OrdemServico result = repository.save(OrdemServico.restaurar(osUuid, OsStatus.RECEBIDA));

        verify(panacheRepository).persist(entity);

        assertEquals(osUuid, entity.uuid);
        assertEquals("ORCADA", entity.status);
        assertEquals(OsStatus.RECEBIDA, result.getStatus());
    }

    @Test
    @DisplayName("Deve buscar OS por UUID e converter para domínio")
    void deveBuscarOsPorUuidEConverterParaDominio() {
        OrdemServicoPanacheRepository panacheRepository = mock(OrdemServicoPanacheRepository.class);

        OrdemServicoRepositoryImpl repository = new OrdemServicoRepositoryImpl();
        repository.panacheRepository = panacheRepository;

        UUID osUuid = UUID.randomUUID();

        OrdemServicoEntity entity = new OrdemServicoEntity();
        entity.uuid = osUuid;
        entity.status = "APROVADA";

        when(panacheRepository.findByUuid(osUuid)).thenReturn(Optional.of(entity));

        Optional result = repository.findByUuid(osUuid);

        assertTrue(result.isPresent());

        OrdemServico os = (OrdemServico) result.get();

        assertEquals(osUuid, os.getUuid());
        assertEquals(OsStatus.RECEBIDA, os.getStatus());
    }
}
