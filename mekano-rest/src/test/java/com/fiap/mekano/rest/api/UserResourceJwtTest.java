package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testes de integração JWT da Phase 8 (UAT-2 + regressões D-05/G10).
 *
 * Emite tokens reais via SmallRyeJwtBuildApi assinados com a chave privada
 * in-memory de {@link JwtTestProfile} (D-10). A chave pública correspondente
 * é injetada via override inline `mp.jwt.verify.publickey` do TestProfile,
 * tomando precedência sobre `mp.jwt.verify.publickey.location`.
 *
 * Cenários (RESEARCH §D-10 e PATTERNS.md):
 * <ul>
 *   <li>{@link #test_validJwt_returns201} — UAT-2 (token válido → 201).</li>
 *   <li>{@link #test_wrongIssuer_returns401} — D-05 / T-08-10 Spoofing.</li>
 *   <li>{@link #test_expiredJwt_returns401} — G10 / T-08-11 (claim {@code exp}).</li>
 * </ul>
 *
 * Cenário role-mismatch ({@code groups != "user"}) deferido a v2 (RESEARCH Q3).
 */
@QuarkusTest
@TestProfile(JwtTestProfile.class)
class UserResourceJwtTest {

    private static final String DEFAULT_ISSUER = "https://mekano.fiap.com.br/auth";

    /**
     * Helper: emite token JWT assinado com a chave privada do {@link JwtTestProfile}.
     * TTL parametrizável para permitir token expirado (G10).
     */
    private String issueToken(String issuer, Set<String> groups, Duration ttl) {
        return Jwt.issuer(issuer)
                .upn("ana.jwt@fiap.br")
                .groups(groups)
                .expiresIn(ttl)
                .sign(JwtTestProfile.privateKey());
    }

    @Test
    void test_validJwt_returns201() {
        String token = issueToken(DEFAULT_ISSUER, Set.of("user"), Duration.ofMinutes(5));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
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
    void test_wrongIssuer_returns401() {
        String token = issueToken("https://attacker.example/auth", Set.of("user"), Duration.ofMinutes(5));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"name":"Ana","email":"ana.uat-iss@fiap.br","password":"abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(401)
                .body("message", notNullValue());
    }

    @Test
    void test_expiredJwt_returns401() {
        String token = issueToken(DEFAULT_ISSUER, Set.of("user"), Duration.ofSeconds(-60));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"name":"Ana","email":"ana.uat-exp@fiap.br","password":"abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(401)
                .body("message", notNullValue());
    }

    /**
     * WR-02 (Code Review 08): JWT de assinatura válida cuja claim {@code groups}
     * não contém {@code "user"} deve disparar {@link jakarta.ws.rs.ForbiddenException}
     * pelo interceptor JAX-RS, traduzido em 403 + body {@link com.fiap.mekano.rest.api.exception.ErrorResponse}
     * pelo {@code ForbiddenExceptionMapper} (D-06: toda resposta de erro é JSON canônico).
     */
    @Test
    void test_wrongRole_returns403() {
        String token = issueToken(DEFAULT_ISSUER, Set.of("guest"), Duration.ofMinutes(5));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("""
                        {"name":"Ana","email":"ana.uat-role@fiap.br","password":"abc123"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(403)
                .body("message", notNullValue());
    }
}
