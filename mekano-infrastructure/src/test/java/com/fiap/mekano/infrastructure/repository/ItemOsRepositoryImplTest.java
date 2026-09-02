package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.ItemOs;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("ItemOsRepositoryImpl")
class ItemOsRepositoryImplTest {

    @Inject
    ItemOsRepositoryImpl repository;

    @Test
    @TestTransaction
    @DisplayName("save deve persistir novo item de OS")
    void saveDevePersistirNovoItem() {
        UUID osUuid = UUID.randomUUID();
        UUID refUuid = UUID.randomUUID();
        ItemOs item = ItemOs.create(osUuid, refUuid, "PECA", "Óleo 5W30", 2L);

        ItemOs saved = repository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOsUuid()).isEqualTo(osUuid);
        assertThat(saved.getReferenciaUuid()).isEqualTo(refUuid);
        assertThat(saved.getTipo()).isEqualTo("PECA");
        assertThat(saved.getQuantidade()).isEqualTo(2L);
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @TestTransaction
    @DisplayName("save deve atualizar item existente")
    void saveDeveAtualizarItemExistente() {
        UUID osUuid = UUID.randomUUID();
        UUID refUuid = UUID.randomUUID();
        ItemOs item = ItemOs.create(osUuid, refUuid, "PECA", "Óleo 5W30", 2L);
        ItemOs saved = repository.save(item);

        ItemOs atualizado = ItemOs.reconstitute(
                saved.getId(), osUuid, refUuid, "PECA", "Troca de óleo", 1L,
                saved.getCreatedAt(), true);

        ItemOs result = repository.save(atualizado);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getTipo()).isEqualTo("PECA");
        assertThat(result.getDescricao()).isEqualTo("Troca de óleo");
    }

    @Test
    @TestTransaction
    @DisplayName("findByOsUuid deve retornar itens da OS")
    void findByOsUuidDeveRetornarItensDaOs() {
        UUID osUuid = UUID.randomUUID();
        UUID refUuid1 = UUID.randomUUID();
        UUID refUuid2 = UUID.randomUUID();
        repository.save(ItemOs.create(osUuid, refUuid1, "PECA", "Filtro", 1L));
        repository.save(ItemOs.create(osUuid, refUuid2, "SERVICO", "Troca", 1L));

        List<ItemOs> result = repository.findByOsUuid(osUuid);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ItemOs::getOsUuid).containsOnly(osUuid);
    }

    @Test
    @TestTransaction
    @DisplayName("findByOsUuid deve retornar lista vazia quando não há itens")
    void findByOsUuidDeveRetornarListaVaziaQuandoNaoHaItens() {
        List<ItemOs> result = repository.findByOsUuid(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("deleteByOsUuid deve fazer soft delete dos itens")
    void deleteByOsUuidDeveFazerSoftDelete() {
        UUID osUuid = UUID.randomUUID();
        repository.save(ItemOs.create(osUuid, UUID.randomUUID(), "PECA", "Filtro", 1L));
        repository.save(ItemOs.create(osUuid, UUID.randomUUID(), "SERVICO", "Troca", 1L));

        repository.deleteByOsUuid(osUuid);

        List<ItemOs> ativos = repository.findByOsUuid(osUuid);
        assertThat(ativos).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("deleteByOsUuid não deve deletar itens de outra OS")
    void deleteByOsUuidNaoDeveDeletarItensDeOutraOs() {
        UUID osUuid1 = UUID.randomUUID();
        UUID osUuid2 = UUID.randomUUID();
        repository.save(ItemOs.create(osUuid1, UUID.randomUUID(), "PECA", "Filtro", 1L));
        repository.save(ItemOs.create(osUuid2, UUID.randomUUID(), "SERVICO", "Troca", 1L));

        repository.deleteByOsUuid(osUuid1);

        List<ItemOs> itensOs2 = repository.findByOsUuid(osUuid2);
        assertThat(itensOs2).hasSize(1);
    }
}
