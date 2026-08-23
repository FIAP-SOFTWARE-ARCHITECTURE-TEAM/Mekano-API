package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link PecaRepositoryImpl}.
 *
 * Usa QuarkusTest + DevServices PostgreSQL + Flyway migrations.
 * Cada teste roda dentro de uma transação isolada com rollback automático.
 */
@QuarkusTest
@DisplayName("PecaRepositoryImpl")
class PecaRepositoryImplTest {

    @Inject
    PecaRepositoryImpl repository;

    @Inject
    PecaPanacheRepository panacheRepository;

    private UUID pecaId;

    @BeforeEach
    void setUp() {
        Peca peca = Peca.create("PEA-INTEG-001", "Óleo Motor 5W30", new BigDecimal("45.50"), 5L);
        Peca saved = repository.salvar(peca);
        pecaId = saved.getId();
    }

    @Test
    @TestTransaction
    @DisplayName("salvar deve persistir saldoReservado como 0")
    void salvarDevePersistirSaldoReservadoZero() {
        Optional<Peca> found = repository.buscarPorId(pecaId);

        assertThat(found).isPresent();
        assertThat(found.get().getSaldoReservado()).isZero();
        assertThat(found.get().getSaldoAtual()).isZero();
    }

    @Test
    @TestTransaction
    @DisplayName("salvar em peça inativa deve atualizar campos preservando isActive=false")
    void salvarDevePreservarIsActiveEmPecaInativa() {
        repository.remover(pecaId);

        Optional<Peca> inativa = repository.buscarPorId(pecaId);
        assertThat(inativa).isPresent();
        assertThat(inativa.get().getIsActive()).isFalse();

        Peca atualizada = Peca.reconstitute(
                pecaId, "PEA-INTEG-001", "Óleo Motor 5W40", new BigDecimal("55.90"),
                0L, 5L, inativa.get().getCreatedAt(), 0L);

        Peca resultado = repository.salvar(atualizada);

        assertThat(resultado.getDescricao()).isEqualTo("Óleo Motor 5W40");
        assertThat(resultado.getValorUnitario()).isEqualTo(new BigDecimal("55.90"));
        assertThat(resultado.getIsActive()).isFalse();

        PecaEntity persistido = panacheRepository.find("uuid", pecaId).firstResult();
        assertThat(persistido).isNotNull();
        assertThat(persistido.descricao).isEqualTo("Óleo Motor 5W40");
        assertThat(persistido.getIsActive()).isFalse();
        assertThat(persistido.getDeletedAt()).isNotNull();
    }

    @Test
    @TestTransaction
    @DisplayName("reservarSaldo com saldo suficiente deve retornar true e incrementar saldo_reservado")
    void reservarSaldoComSaldoSuficiente() {
        // creditar saldo primeiro
        repository.creditarSaldo(pecaId, 50);

        boolean reservado = repository.reservarSaldo(pecaId, 10);

        assertThat(reservado).isTrue();
        Optional<Peca> found = repository.buscarPorId(pecaId);
        assertThat(found).isPresent();
        assertThat(found.get().getSaldoReservado()).isEqualTo(10L);
        assertThat(found.get().getSaldoAtual()).isEqualTo(50L);
    }

    @Test
    @TestTransaction
    @DisplayName("reservarSaldo acima do disponível deve retornar false sem alterar")
    void reservarSaldoAcimaDoDisponivel() {
        repository.creditarSaldo(pecaId, 20);

        boolean reservado = repository.reservarSaldo(pecaId, 999);

        assertThat(reservado).isFalse();
        Optional<Peca> found = repository.buscarPorId(pecaId);
        assertThat(found).isPresent();
        assertThat(found.get().getSaldoReservado()).isZero();
        assertThat(found.get().getSaldoAtual()).isEqualTo(20L);
    }

    @Test
    @TestTransaction
    @DisplayName("debitarSaldoReservado deve decrementar saldo e saldo_reservado")
    void debitarSaldoReservadoDeveDebitar() {
        repository.creditarSaldo(pecaId, 50);
        repository.reservarSaldo(pecaId, 10);

        boolean debitado = repository.debitarSaldoReservado(pecaId, 10);

        assertThat(debitado).isTrue();
        Optional<Peca> found = repository.buscarPorId(pecaId);
        assertThat(found).isPresent();
        assertThat(found.get().getSaldoAtual()).isEqualTo(40L);
        assertThat(found.get().getSaldoReservado()).isZero();
    }

    @Test
    @TestTransaction
    @DisplayName("debitarSaldoReservado sem reserva deve retornar false")
    void debitarSaldoReservadoSemReserva() {
        repository.creditarSaldo(pecaId, 50);

        boolean debitado = repository.debitarSaldoReservado(pecaId, 10);

        assertThat(debitado).isFalse();
    }

    @Test
    @TestTransaction
    @DisplayName("liberarReserva deve decrementar saldo_reservado sem alterar saldo")
    void liberarReservaDeveLiberar() {
        repository.creditarSaldo(pecaId, 50);
        repository.reservarSaldo(pecaId, 10);

        boolean liberado = repository.liberarReserva(pecaId, 10);

        assertThat(liberado).isTrue();
        Optional<Peca> found = repository.buscarPorId(pecaId);
        assertThat(found).isPresent();
        assertThat(found.get().getSaldoAtual()).isEqualTo(50L);
        assertThat(found.get().getSaldoReservado()).isZero();
    }

    @Test
    @TestTransaction
    @DisplayName("liberarReserva sem reserva deve retornar false")
    void liberarReservaSemReserva() {
        repository.creditarSaldo(pecaId, 50);

        boolean liberado = repository.liberarReserva(pecaId, 10);

        assertThat(liberado).isFalse();
    }

    @Test
    @TestTransaction
    @DisplayName("listarAbaixoEstoqueMinimo deve considerar saldo disponível")
    void listarAbaixoEstoqueMinimoConsideraDisponivel() {
        // saldo=10, minimo=5, reservado=8 → disponivel=2 < 5 → listado
        repository.creditarSaldo(pecaId, 10);
        repository.reservarSaldo(pecaId, 8);

        List<Peca> abaixo = repository.listarAbaixoEstoqueMinimo();

        assertThat(abaixo).extracting(Peca::getId).contains(pecaId);
    }

    @Test
    @TestTransaction
    @DisplayName("listarAbaixoEstoqueMinimo não lista peças com disponível suficiente")
    void listarAbaixoEstoqueMinimoIgnoraDisponivelSuficiente() {
        // saldo=10, minimo=5, reservado=2 → disponivel=8 >= 5 → não listado
        repository.creditarSaldo(pecaId, 10);
        repository.reservarSaldo(pecaId, 2);

        List<Peca> abaixo = repository.listarAbaixoEstoqueMinimo();

        assertThat(abaixo).extracting(Peca::getId).doesNotContain(pecaId);
    }

    @Test
    @TestTransaction
    @DisplayName("findAll deve filtrar por isActive")
    void findAllDeveFiltrarPorIsActive() {
        Peca ativa = Peca.create("PEA-INTEG-002", "Filtro de Óleo", new BigDecimal("25.00"), 3L);
        Peca salvaInativa = repository.salvar(ativa);
        repository.remover(salvaInativa.getId());

        assertThat(repository.findAll(0, 100, null))
                .extracting(Peca::getId)
                .contains(pecaId, salvaInativa.getId());
        assertThat(repository.findAll(0, 100, true))
                .extracting(Peca::getId)
                .contains(pecaId)
                .doesNotContain(salvaInativa.getId());
        assertThat(repository.findAll(0, 100, false))
                .extracting(Peca::getId)
                .contains(salvaInativa.getId())
                .doesNotContain(pecaId);
        assertThat(repository.countAll(null)).isGreaterThanOrEqualTo(2L);
        assertThat(repository.countAll(true)).isGreaterThanOrEqualTo(1L);
        assertThat(repository.countAll(false)).isGreaterThanOrEqualTo(1L);
    }
}