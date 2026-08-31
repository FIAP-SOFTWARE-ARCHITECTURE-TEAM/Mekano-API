package com.fiap.mekano.infrastructure.repository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("ProcessedEventRepositoryImpl")
class ProcessedEventRepositoryImplTest {

    @Inject
    ProcessedEventRepositoryImpl repository;

    @Test
    @TestTransaction
    @DisplayName("existsFor deve retornar false quando evento não processado")
    void existsForDeveRetornarFalseQuandoNaoProcessado() {
        boolean exists = repository.existsFor("PAGAMENTO_CONFIRMADO", UUID.randomUUID());

        assertThat(exists).isFalse();
    }

    @Test
    @TestTransaction
    @DisplayName("existsFor deve retornar true quando evento já processado")
    void existsForDeveRetornarTrueQuandoJaProcessado() {
        UUID aggregateUuid = UUID.randomUUID();
        repository.save("PAGAMENTO_CONFIRMADO", aggregateUuid);

        boolean exists = repository.existsFor("PAGAMENTO_CONFIRMADO", aggregateUuid);

        assertThat(exists).isTrue();
    }

    @Test
    @TestTransaction
    @DisplayName("save deve persistir novo evento processado")
    void saveDevePersistirNovoEvento() {
        UUID aggregateUuid = UUID.randomUUID();

        repository.save("OS_CANCELADA", aggregateUuid);

        boolean exists = repository.existsFor("OS_CANCELADA", aggregateUuid);
        assertThat(exists).isTrue();
    }

    @Test
    @TestTransaction
    @DisplayName("save não deve duplicar evento já existente")
    void saveNaoDeveDuplicarEventoExistente() {
        UUID aggregateUuid = UUID.randomUUID();
        repository.save("ENTREGA_CONFIRMADA", aggregateUuid);

        repository.save("ENTREGA_CONFIRMADA", aggregateUuid);

        boolean exists = repository.existsFor("ENTREGA_CONFIRMADA", aggregateUuid);
        assertThat(exists).isTrue();
    }

    @Test
    @TestTransaction
    @DisplayName("existsFor com tipos diferentes deve retornar false")
    void existsForComTiposDiferentesDeveRetornarFalse() {
        UUID aggregateUuid = UUID.randomUUID();
        repository.save("PAGAMENTO_CONFIRMADO", aggregateUuid);

        boolean exists = repository.existsFor("OS_CANCELADA", aggregateUuid);

        assertThat(exists).isFalse();
    }
}
