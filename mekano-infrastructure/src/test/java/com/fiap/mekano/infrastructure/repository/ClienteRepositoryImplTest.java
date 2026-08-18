package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Cliente;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("ClienteRepositoryImpl")
class ClienteRepositoryImplTest {

    @Inject
    ClienteRepositoryImpl repository;

    private Cliente cliente(String nome, String telefone, String cpf) {
        return Cliente.reconstitute(UUID.randomUUID(), nome, cpf,
                nome.toLowerCase().replace(" ", "") + "@test.com", telefone,
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000", LocalDateTime.now());
    }

    @Test
    @TestTransaction
    void findByTelefone_exato_deveRetornarCliente() {
        repository.save(cliente("Cliente A", "91984847811", "12345678909"));

        var result = repository.findByTelefone("91984847811");

        assertThat(result).isPresent();
        assertThat(result.get().getTelefone().getValue()).isEqualTo("91984847811");
    }

    @Test
    @TestTransaction
    void findByTelefone_fallbackSufixoComDDD_deveRetornarUnico() {
        repository.save(cliente("Cliente A", "91984847811", "12345678909"));

        // 13 dígitos (DDI 55 + DDD 91 + número): sem match exato → fallback
        // por sufixo com DDD (últimos 10 dígitos = 91984847811) → único (WR-04)
        var result = repository.findByTelefone("5591984847811");

        assertThat(result).isPresent();
        assertThat(result.get().getTelefone().getValue()).isEqualTo("91984847811");
    }

    @Test
    @TestTransaction
    void findByTelefone_fallbackAmbiguo_deveRetornarVazio() {
        repository.save(cliente("Cliente A", "91984847811", "12345678909"));
        repository.save(cliente("Cliente B", "21984847811", "11144477735"));

        // Mesmo número local (984847811) em DDDs diferentes (91 e 21):
        // sufixo de 10 dígitos é idêntico → resultado ambíguo → vazio (WR-04)
        var result = repository.findByTelefone("11984847811");

        assertThat(result).isEmpty();
    }
}