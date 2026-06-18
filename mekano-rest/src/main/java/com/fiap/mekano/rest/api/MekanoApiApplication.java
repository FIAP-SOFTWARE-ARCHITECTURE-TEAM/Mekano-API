package com.fiap.mekano.rest.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Configuração JAX-RS + OpenAPI da API Mekano.
 *
 * <p><b>Por que existe esta classe</b>: o Quarkus já registra automaticamente
 * todos os {@code @Path}, então uma {@link Application} normalmente é
 * dispensável. Aqui ela serve como âncora para anotações OpenAPI globais
 * que o {@code quarkus-smallrye-openapi} reflete no documento gerado em
 * {@code /q/openapi} e no Swagger UI em {@code /q/swagger-ui}.
 *
 * <p><b>{@code @SecurityScheme}</b> declara um esquema HTTP Bearer/JWT
 * chamado {@code "bearer-jwt"}. É isso que faz o Swagger UI exibir o botão
 * <b>Authorize 🔓</b> na navbar — clicando, o usuário cola o JWT obtido
 * via {@code POST /auth/login} e o UI passa a anexar
 * {@code Authorization: Bearer <token>} a todas as chamadas subsequentes.
 *
 * <p><b>{@code @SecurityRequirement} global</b> aplica o requirement a
 * todos os endpoints por padrão. Endpoints públicos (ex.: {@code AuthResource})
 * sobrescrevem com {@code @SecurityRequirement(name = "")} para indicar
 * que não exigem token.
 */
@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(
                title = "Mekano API",
                version = "1.0.0",
                description = "Clean Architecture REST API — FIAP Software Architecture"
        ),
        security = @SecurityRequirement(name = "bearer-jwt")
)
@SecurityScheme(
        securitySchemeName = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Cole aqui o JWT retornado por POST /auth/login. " +
                "O Swagger UI envia automaticamente como header 'Authorization: Bearer <token>'."
)
public class MekanoApiApplication extends Application {
}
