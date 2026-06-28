package com.fiap.mekano.rest.api;

import com.fiap.mekano.infrastructure.repository.PecaPanacheRepository;
import com.fiap.mekano.rest.api.dto.AlertaResponse;
import com.fiap.mekano.rest.api.dto.PecaResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/alertas")
@RequestScoped
@RolesAllowed({"admin", "atendente"})
@Tag(name = "Alertas", description = "Alertas de estoque mínimo")
public class AlertaResource {

    @Inject
    PecaPanacheRepository pecaPanacheRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar alertas de estoque", description = "Retorna peças cujo saldo atual está abaixo do estoque mínimo configurado.")
    @APIResponse(responseCode = "200", description = "Lista de alertas",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PecaResponse.class)))
    public Response listAlertas() {
        var entities = pecaPanacheRepository.listAll();
        var alertas = entities.stream()
                .filter(e -> e.estoqueMinimo > 0 && e.saldo < e.estoqueMinimo)
                .map(e -> new AlertaResponse(e.uuid, e.uuid.toString(), e.descricao,
                        (long) e.saldo, (long) e.estoqueMinimo))
                .toList();
        return Response.ok(alertas).build();
    }
}
