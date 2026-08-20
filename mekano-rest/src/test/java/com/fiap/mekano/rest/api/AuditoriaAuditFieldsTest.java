package com.fiap.mekano.rest.api;

import com.fiap.mekano.infrastructure.entity.PecaEntity;
import com.fiap.mekano.infrastructure.repository.PecaPanacheRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Verifica o preenchimento automático de {@code created_by}/{@code updated_by}
 * a partir do usuário autenticado (JWT) nos CRUDs.
 *
 * <p>Usa o recurso de {@code @TestSecurity}: o atributo {@code user} é retornado como
 * {@code JsonWebToken.getName()} (subject), que neste projeto é o UUID do usuário.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditoriaAuditFieldsTest {

    private static final String BASE_PATH = "/api/v1/pecas";
    private static final String CRIADOR = "11111111-1111-1111-1111-111111111111";
    private static final String ATUALIZADOR = "22222222-2222-2222-2222-222222222222";
    private static UUID pecaUuid;

    @Inject
    PecaPanacheRepository panacheRepository;

    @Test
    @Order(1)
    @TestTransaction
    @TestSecurity(user = CRIADOR, roles = {"admin"})
    void cadastro_devePreencherCreatedByComUsuarioAutenticado() {
        String codigo = "AUD-" + UUID.randomUUID().toString().substring(0, 8);

        pecaUuid = UUID.fromString(given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "codigo": "%s",
                          "descricao": "Peça de auditoria",
                          "valorUnitario": 12.50,
                          "estoqueMinimo": 5
                        }
                        """.formatted(codigo))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("codigo", equalTo(codigo))
                .extract()
                .path("id"));

        PecaEntity entity = panacheRepository.find("uuid = ?1", pecaUuid).firstResult();
        assertThat(entity).isNotNull();
        assertThat(entity.getCreatedBy()).isEqualTo(UUID.fromString(CRIADOR));
        assertThat(entity.getUpdatedBy()).isNull();
    }

    @Test
    @Order(2)
    @TestTransaction
    @TestSecurity(user = ATUALIZADOR, roles = {"admin"})
    void alteracao_deveAtualizarUpdatedBy_ePreservarCreatedBy() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "codigo": "AUD-X",
                          "descricao": "Peça de auditoria atualizada",
                          "valorUnitario": 15.00,
                          "estoqueMinimo": 3
                        }
                        """)
                .when()
                .put(BASE_PATH + "/" + pecaUuid)
                .then()
                .statusCode(200);

        PecaEntity entity = panacheRepository.find("uuid = ?1", pecaUuid).firstResult();
        assertThat(entity).isNotNull();
        assertThat(entity.getCreatedBy()).isEqualTo(UUID.fromString(CRIADOR));
        assertThat(entity.getUpdatedBy()).isEqualTo(UUID.fromString(ATUALIZADOR));
    }
}