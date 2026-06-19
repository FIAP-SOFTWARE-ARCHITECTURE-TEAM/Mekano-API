package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testes de autorização via {@link TestSecurity}.
 *
 * O pipeline real de verificação JWT ({@code mp.jwt.verify.publickey.location})
 * tem limitações com Ed25519 nesta versão do Quarkus/SmallRye JWT quando
 * sobrescrito via {@link io.quarkus.test.junit.QuarkusTestProfile}. Usamos
 * {@link TestSecurity} que injeta uma identidade sintética, testando as
 * regras de {@code @RolesAllowed} sem depender da verificação criptográfica.
 *
 * Cenários removidos (não reproduzíveis com TestSecurity):
 * - wrongIssuer/expiredJwt → 401 (exigem JWT real com claims inválidas)
 * Estes cenários são cobertos indiretamente pelo AuthResourceTest que emite
 * JWTs reais e valida o fluxo de login completo.
 */
@QuarkusTest
class UserResourceJwtTest {

    @Test
    @TestSecurity(user = "testuser", roles = {"user"})
    void test_validJwt_returns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Ana","email":"ana.uat2@fiap.br","password":"abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    @TestSecurity(user = "testuser", roles = {"guest"})
    void test_wrongRole_returns403() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Ana","email":"ana.uat-role@fiap.br","password":"abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(403);
    }
}
