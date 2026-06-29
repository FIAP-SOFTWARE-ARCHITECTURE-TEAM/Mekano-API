package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.GerarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.rest.api.dto.CreateOrcamentoRequest;
import com.fiap.mekano.rest.api.dto.OrcamentoResponse;
import com.fiap.mekano.rest.api.dto.ReprovarMotivoRequest;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/orcamentos")
@RequestScoped
@RolesAllowed({"admin", "atendente", "mecanico"})
@Tag(name = "Orçamentos", description = "Geração, aprovação e reprovação de orçamentos")
public class OrcamentoResource {

    @Inject
    OrcamentoServicePort orcamentoService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gerar orçamento", description = "Gera um orçamento a partir dos itens de serviço diagnosticados")
    @APIResponse(responseCode = "201", description = "Orçamento gerado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrcamentoResponse.class)))
    @APIResponse(responseCode = "422", description = "OS não está em EM_DIAGNOSTICO",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public Response gerar(@jakarta.validation.Valid CreateOrcamentoRequest request, @Context UriInfo uriInfo) {
        List<ItemOrcamento> itens = request.getItens().stream()
                .<ItemOrcamento>map(i -> new ItemOrcamento(i.getDescricao(), i.getQuantidade(), i.getValorUnitario()))
                .toList();

        var command = new GerarOrcamentoCommand(request.getOrdemServicoUuid(), request.getDescricao(), itens);
        var orcamento = orcamentoService.gerarOrcamento(command);
        var response = OrcamentoResponse.from(orcamento);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar orçamento por UUID")
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
    @RolesAllowed("cliente")
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
    @RolesAllowed("cliente")
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