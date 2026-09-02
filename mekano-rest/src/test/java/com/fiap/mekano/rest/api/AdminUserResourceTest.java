package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.in.AdminCreatedUser;
import com.fiap.mekano.domain.port.in.AdminUserServicePort;
import com.fiap.mekano.domain.port.in.AdminUserSummary;
import com.fiap.mekano.domain.port.in.CreateAdminUserCommand;
import com.fiap.mekano.shared.exception.AppException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestSecurity(user = "admin-test", roles = {"admin"})
class AdminUserResourceTest {

    @InjectMock
    AdminUserServicePort adminUserService;

    @Test
    void post_comAdmin_deveRetornar201ComSenhaGerada() {
        UUID userUuid = UUID.randomUUID();

        when(adminUserService.criarUsuario(any(CreateAdminUserCommand.class)))
                .thenReturn(new AdminCreatedUser(
                        userUuid,
                        "Admin Novo",
                        "admin.novo@mekano.com",
                        Role.admin,
                        "Aa1!senhaGerada"
                ));

        given()
                .contentType("application/json")
                .body("""
                      {
                        "name": "Admin Novo",
                        "email": "admin.novo@mekano.com",
                        "role": "admin"
                      }
                      """)
        .when()
                .post("/api/v1/admin/usuarios")
        .then()
                .statusCode(201)
                .body("id", equalTo(userUuid.toString()))
                .body("name", equalTo("Admin Novo"))
                .body("email", equalTo("admin.novo@mekano.com"))
                .body("role", equalTo("admin"))
                .body("senhaGerada", equalTo("Aa1!senhaGerada"));

        verify(adminUserService).criarUsuario(any(CreateAdminUserCommand.class));
    }

    @Test
    void post_comRoleCliente_deveRetornar201() {
        UUID userUuid = UUID.randomUUID();

        when(adminUserService.criarUsuario(any(CreateAdminUserCommand.class)))
                .thenReturn(new AdminCreatedUser(
                        userUuid,
                        "Cliente Novo",
                        "cliente.novo@mekano.com",
                        Role.cliente,
                        "Aa1!senhaGerada"
                ));

        given()
                .contentType("application/json")
                .body("""
                      {
                        "name": "Cliente Novo",
                        "email": "cliente.novo@mekano.com",
                        "role": "cliente"
                      }
                      """)
        .when()
                .post("/api/v1/admin/usuarios")
        .then()
                .statusCode(201)
                .body("role", equalTo("cliente"))
                .body("senhaGerada", equalTo("Aa1!senhaGerada"));
    }

    @Test
    void get_comAdmin_deveRetornarListaPaginada() {
        UUID userUuid = UUID.randomUUID();

        when(adminUserService.listar(0, 20, null))
                .thenReturn(List.of(new AdminUserSummary(
                        userUuid,
                        "Admin Novo",
                        "admin.novo@mekano.com",
                        Role.admin,
                        true
                )));

        given()
        .when()
                .get("/api/v1/admin/usuarios")
        .then()
                .statusCode(200)
                .body("[0].id", equalTo(userUuid.toString()))
                .body("[0].name", equalTo("Admin Novo"))
                .body("[0].email", equalTo("admin.novo@mekano.com"))
                .body("[0].role", equalTo("admin"))
                .body("[0].active", equalTo(true));
    }

    @Test
    void get_filtroIsActiveFalse_deveRepassarFiltro() {
        UUID userUuid = UUID.randomUUID();

        when(adminUserService.listar(0, 20, false))
                .thenReturn(List.of(new AdminUserSummary(
                        userUuid,
                        "Admin Inativo",
                        "admin.inativo@mekano.com",
                        Role.admin,
                        false
                )));

        given()
        .when()
                .get("/api/v1/admin/usuarios?isActive=false")
        .then()
                .statusCode(200)
                .body("[0].id", equalTo(userUuid.toString()))
                .body("[0].active", equalTo(false));

        verify(adminUserService).listar(0, 20, false);
    }

    @Test
    void delete_comAdmin_deveRetornar204() {
        UUID userUuid = UUID.randomUUID();

        given()
        .when()
                .delete("/api/v1/admin/usuarios/{uuid}", userUuid)
        .then()
                .statusCode(204);

        verify(adminUserService).deletar(userUuid);
    }

    @Test
    void post_emailDuplicado_retorna409() {
        when(adminUserService.criarUsuario(any(CreateAdminUserCommand.class)))
                .thenThrow(new AppException(409, "Usuário já existe com o email: duplicado@mekano.com"));

        given()
                .contentType("application/json")
                .body("""
                      {
                        "name": "Duplicado",
                        "email": "duplicado@mekano.com",
                        "role": "admin"
                      }
                      """)
        .when()
                .post("/api/v1/admin/usuarios")
        .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"))
                .body("status", equalTo(409));
    }
}
