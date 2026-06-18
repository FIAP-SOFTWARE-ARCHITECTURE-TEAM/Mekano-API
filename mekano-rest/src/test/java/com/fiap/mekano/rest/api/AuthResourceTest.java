package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Testes de integração para POST /auth/login.
 *
 * <p>Cobre o fluxo completo:
 * <ol>
 *     <li>{@link #login_validCredentials_returns200WithToken()} — credenciais válidas
 *         retornam JWT no shape OAuth 2.0.</li>
 *     <li>{@link #login_wrongPassword_returns401()} — senha incorreta colapsa em
 *         "Invalid credentials" (T-08-07: sem distinguir de email inexistente).</li>
 *     <li>{@link #login_unknownEmail_returns401()} — email não cadastrado, mesma
 *         resposta canônica (anti-enumeration).</li>
 *     <li>{@link #login_invalidPayload_returns400()} — Bean Validation pega
 *         email malformado.</li>
 * </ol>
 *
 * <p>Usa {@link JwtTestProfile} para fornecer o par RSA in-memory que assina e
 * valida tokens — necessário porque o {@code AuthResource} chama {@code Jwt.sign()}
 * e requer {@code smallrye.jwt.sign.key.location} apontando para uma chave válida.
 *
 * <p>Order(1) seeda o usuário via POST /users com {@code @TestSecurity}; Order(2+)
 * exercitam {@code /auth/login} sem qualquer header de autenticação.
 */
@QuarkusTest
@TestProfile(JwtTestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceTest {

    private static final String EMAIL = "auth.user@fiap.br";
    private static final String PASSWORD = "abc123";

    @Test
    @Order(1)
    @TestSecurity(user = "testuser", roles = {"user"})
    void seed_user_for_login_tests() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Auth User","email":"%s","password":"%s"}
                        """.formatted(EMAIL, PASSWORD))
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(2)
    void login_validCredentials_returns200WithToken() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"%s"}
                        """.formatted(EMAIL, PASSWORD))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("access_token", notNullValue())
                .body("access_token", startsWith("eyJ"))
                .body("token_type", equalTo("Bearer"))
                .body("expires_in", notNullValue());
    }

    @Test
    @Order(3)
    void login_wrongPassword_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"%s","password":"wrong-password"}
                        """.formatted(EMAIL))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("message", equalTo("Invalid credentials"));
    }

    @Test
    @Order(4)
    void login_unknownEmail_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"nobody@fiap.br","password":"%s"}
                        """.formatted(PASSWORD))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("message", equalTo("Invalid credentials"));
    }

    @Test
    @Order(5)
    void login_invalidPayload_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"not-an-email","password":""}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400);
    }
}
