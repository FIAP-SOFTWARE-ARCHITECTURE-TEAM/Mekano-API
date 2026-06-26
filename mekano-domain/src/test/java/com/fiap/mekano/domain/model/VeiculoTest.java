package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.valueobject.PlacaVeiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ==== VeiculoTest ====
 * Criar veículo com todos os campos preenchidos:
 * - placa é do tipo PlacaVeiculo,
 * - create() gera UUID automaticamente,
 * - create() gera createdAt;
 * Duas chamadas geram UUIDs distintos;
 * Reconstitute() preserva:
 * - UUID original,
 * - createdAt,
 * - clienteUuid;
 */

@DisplayName("Veiculo — entidade de domínio")
class VeiculoTest {

    private static final UUID CLIENTE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PLACA_VALIDA = "ABC1234";
    private static final String MARCA_VALIDA = "Toyota";
    private static final String MODELO_VALIDO = "Corolla";
    private static final Integer ANO_VALIDO = 2024;

    @Test
    @DisplayName("deve criar Veiculo com todos os campos populados")
    void deveCriarVeiculoComCamposPopulados() {
        Veiculo veiculo = Veiculo.create(CLIENTE_UUID, PLACA_VALIDA, MARCA_VALIDA, MODELO_VALIDO, ANO_VALIDO);
        assertNotNull(veiculo);
        assertNotNull(veiculo.getId());
        assertNotNull(veiculo.getCreatedAt());
        assertEquals(CLIENTE_UUID, veiculo.getClienteUuid());
        assertEquals(MARCA_VALIDA, veiculo.getMarca());
        assertEquals(MODELO_VALIDO, veiculo.getModelo());
        assertEquals(ANO_VALIDO, veiculo.getAno());
        assertNotNull(veiculo.getPlaca());
    }

    @Test
    @DisplayName("campo placa deve ser do tipo PlacaVeiculo VO")
    void campoPlacaDeveSerVO() {
        Veiculo veiculo = Veiculo.create(CLIENTE_UUID, PLACA_VALIDA, MARCA_VALIDA, MODELO_VALIDO, ANO_VALIDO);
        assertInstanceOf(PlacaVeiculo.class, veiculo.getPlaca());
        assertEquals(PLACA_VALIDA, veiculo.getPlaca().getValue());
    }

    @Test
    @DisplayName("duas chamadas a create devem gerar UUIDs distintos")
    void duasChamadasDevemGerarIdsDistintos() {
        Veiculo veiculo1 = Veiculo.create(CLIENTE_UUID, PLACA_VALIDA, MARCA_VALIDA, MODELO_VALIDO, ANO_VALIDO);
        Veiculo veiculo2 = Veiculo.create(CLIENTE_UUID, "DEF1234", MARCA_VALIDA, MODELO_VALIDO, ANO_VALIDO);
        assertNotEquals(veiculo1.getId(), veiculo2.getId());
    }

    @Test
    @DisplayName("reconstitute deve preservar UUID e createdAt")
    void reconstituteDevePreservarValoresOriginais() {
        UUID veiculoUuid = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 26, 10, 30);
        Veiculo veiculo = Veiculo.reconstitute(veiculoUuid, CLIENTE_UUID, PLACA_VALIDA, MARCA_VALIDA, MODELO_VALIDO,
                ANO_VALIDO, createdAt);
        assertEquals(veiculoUuid, veiculo.getId());
        assertEquals(CLIENTE_UUID, veiculo.getClienteUuid());
        assertEquals(createdAt, veiculo.getCreatedAt());
    }
}
