package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.os.OsAuditQueryService;
import com.fiap.mekano.rest.api.dto.OsAuditLogResponse;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;
import java.util.UUID;

@Path("/os/{uuid}/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OsAuditResource {

    @Inject
    OsAuditQueryService service;

    @GET
    @RolesAllowed({"admin", "atendente", "mecanico", "financeiro"})
    @Operation(
            summary = "Consultar trilha de auditoria da OS",
            description = "Retorna o histórico de transições da ordem de serviço, ordenado do evento mais recente para o mais antigo."
    )
    @APIResponse(
            responseCode = "200",
            description = "Histórico de auditoria retornado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(responseCode = "401", description = "Usuário não autenticado")
    @APIResponse(responseCode = "403", description = "Usuário sem permissão")
    public List<OsAuditLogResponse> findAudit(@PathParam("uuid") UUID osUuid) {
        return service.findAudit(osUuid)
                .stream()
                .map(item -> new OsAuditLogResponse(
                        item.uuid(),
                        item.osUuid(),
                        item.acao().name(),
                        item.usuarioEmail(),
                        item.observacao(),
                        item.metadataJson(),
                        item.createdAt()
                ))
                .toList();
    }
}
