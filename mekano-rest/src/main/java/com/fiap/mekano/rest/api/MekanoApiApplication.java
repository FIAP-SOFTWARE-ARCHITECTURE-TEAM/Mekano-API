package com.fiap.mekano.rest.api;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Configuração JAX-RS + OpenAPI da API Mekano.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Mekano API",
                version = "1.0.0",
                description = "Clean Architecture REST API — FIAP Software Architecture"
        )
)
public class MekanoApiApplication extends Application {
}
