package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.repository.PecaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PecaDebitoAtomicoTest {

    @Mock
    PecaRepositoryPort pecaRepository;

    @Mock
    PecaRepositoryImpl pecaRepositoryImpl;

    @InjectMocks
    PecaService pecaService;

    private UUID pecaId;
    private Peca pecaMock;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        pecaMock = new Peca("Peca A", 100, 10);
    }

    @Test
    void devePermitirApenasUmaThreadDebitarQuandoSaldoInsuficiente() throws InterruptedException {
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(java.util.Optional.of(pecaMock));

        AtomicInteger debitosComSucesso = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                when(pecaRepositoryImpl.debitarSaldo(pecaId, 60)).thenReturn(true);
                boolean sucesso = pecaService.debitarSaldo(pecaId, 60);
                if (sucesso) debitosComSucesso.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                when(pecaRepositoryImpl.debitarSaldo(pecaId, 60)).thenReturn(false);
                boolean sucesso = pecaService.debitarSaldo(pecaId, 60);
                if (sucesso) debitosComSucesso.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();
        latch.await();

        assertThat(debitosComSucesso.get()).isEqualTo(1);
    }
}
