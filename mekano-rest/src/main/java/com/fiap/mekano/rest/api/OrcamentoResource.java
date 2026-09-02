package com.fiap.mekano.rest.api;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.rest.api.dto.OrcamentoResponse;
import com.fiap.mekano.rest.api.dto.ReprovarMotivoRequest;
import com.fiap.mekano.rest.api.exception.ProblemDetail;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orcamentos")
@RequestScoped
@RolesAllowed({"admin", "atendente", "mecanico", "cliente"})
@Tag(name = "Orçamentos", description = "Consulta, aprovação e reprovação de orçamentos")
public class OrcamentoResource {

    @Inject
    OrcamentoServicePort orcamentoService;

    @GET
    @Path("/os/{osUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar orçamento por OS", description = "Busca o orçamento associado ao UUID de uma ordem de serviço")
    @APIResponse(responseCode = "200", description = "Orçamento encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrcamentoResponse.class)))
    @APIResponse(responseCode = "404", description = "Orçamento não encontrado para a OS",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response buscarPorOSPath(@PathParam("osUuid") UUID osUuid) {
        Orcamento orcamento = orcamentoService.buscarPorOrdemServico(osUuid);
        return Response.ok(OrcamentoResponse.from(orcamento)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar orçamento por OS (query param)", description = "Busca o orçamento associado ao UUID de uma ordem de serviço via query parameter")
    @APIResponse(responseCode = "200", description = "Orçamento encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrcamentoResponse.class)))
    @APIResponse(responseCode = "404", description = "Orçamento não encontrado para a OS",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response buscarPorOSQuery(@QueryParam("osUuid") UUID osUuid) {
        Orcamento orcamento = orcamentoService.buscarPorOrdemServico(osUuid);
        return Response.ok(OrcamentoResponse.from(orcamento)).build();
    }

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar orçamento por UUID", description = "Busca um orçamento pelo seu próprio UUID")
    @APIResponse(responseCode = "200", description = "Orçamento encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrcamentoResponse.class)))
    @APIResponse(responseCode = "404", description = "Orçamento não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response buscarPorId(@PathParam("uuid") UUID uuid) {
        var orcamento = orcamentoService.buscarPorId(uuid);
        return Response.ok(OrcamentoResponse.from(orcamento)).build();
    }

    @POST
    @Path("/{uuid}/aprovar")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Aprovar orçamento", description = "Cliente aprova o orçamento — OS transiciona para EM_EXECUCAO")
    @APIResponse(responseCode = "200", description = "Orçamento aprovado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrcamentoResponse.class)))
    @APIResponse(responseCode = "422", description = "Orçamento não está pendente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response aprovar(@PathParam("uuid") UUID uuid) {
        var orcamento = orcamentoService.aprovar(new AprovarOrcamentoCommand(uuid));
        return Response.ok(OrcamentoResponse.from(orcamento)).build();
    }

    @POST
    @Path("/{uuid}/reprovar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Reprovar orçamento", description = "Cliente reprova o orçamento — OS transiciona para CANCELADA")
    @APIResponse(responseCode = "200", description = "Orçamento reprovado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrcamentoResponse.class)))
    @APIResponse(responseCode = "422", description = "Orçamento não está pendente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response reprovar(@PathParam("uuid") UUID uuid, @Valid ReprovarMotivoRequest request) {
        var orcamento = orcamentoService.reprovar(new ReprovarOrcamentoCommand(uuid, request.motivo()));
        return Response.ok(OrcamentoResponse.from(orcamento)).build();
    }
}