package com.fiap.mekano.rest.api;

import java.net.URI;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.application.service.nfentrada.NfEntradaService;
import com.fiap.mekano.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.rest.api.dto.CreateNfEntradaRequest;
import com.fiap.mekano.rest.api.dto.NfEntradaPageResponse;
import com.fiap.mekano.rest.api.dto.NfEntradaResponse;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.NfEntradaDtoMapper;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/nf-entrada")
@RequestScoped
@RolesAllowed("admin")
@Tag(name = "Notas Fiscais de Entrada", description = "Registro de notas fiscais de entrada de mercadorias")
public class NfEntradaResource {

        @Inject
        NfEntradaService nfEntradaService;

        @Inject
        NfEntradaDtoMapper nfEntradaDtoMapper;

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Registrar NF de entrada", description = "Registra uma nota fiscal de entrada e credita o saldo da peça vinculada.")
        @APIResponse(responseCode = "201", description = "NF registrada com sucesso", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NfEntradaResponse.class)))
        @APIResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        public Response create(@Valid CreateNfEntradaRequest request, @Context UriInfo uriInfo) {
                var command = new CreateNfEntradaCommand(
                                request.getChaveAcesso(), request.getValorTotal(),
                                request.getRequisicaoCompraId());
                var response = nfEntradaService.registrar(command);
                URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
                var nfResponse = new NfEntradaResponse(
                                response.id(), response.chaveAcesso(), response.valorTotal(),
                                response.pecaId(), response.requisicaoCompraId(), response.createdAt());
                return Response.created(location).entity(nfResponse).build();
        }

        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Buscar NF por ID", description = "Retorna os dados da nota fiscal de entrada.")
        @APIResponse(responseCode = "200", description = "NF encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NfEntradaResponse.class)))
        @APIResponse(responseCode = "404", description = "NF não encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProblemDetail.class)))
        public Response getById(@PathParam("id") UUID id) {
                var nfEntrada = nfEntradaService.buscarPorId(id);
                NfEntradaResponse response = nfEntradaDtoMapper.toResponse(nfEntrada);
                return Response.ok(response).build();
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Listar NF's de entrada", description = "Retorna notas fiscais de forma paginada")
        @APIResponse(responseCode = "200", description = "Lista paginada de NF's", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NfEntradaPageResponse.class)))
        public Response listAll(
                        @QueryParam("page") @DefaultValue("0") int page,
                        @QueryParam("size") @DefaultValue("10") int size) {
                int normalizedPage = Math.max(page, 0);
                int normalizedSize = normalizeSize(size);

                var content = nfEntradaService.findAll(normalizedPage, normalizedSize)
                                .stream()
                                .map(nfEntradaDtoMapper::toResponse)
                                .toList();
                long total = nfEntradaService.countAll();
                int totalPages = (int) Math.ceil((double) total / normalizedSize);
                var response = new NfEntradaPageResponse(content, normalizedPage, normalizedSize, total, totalPages);
                return Response.ok(response).build();
        }

        private static int normalizeSize(int size) {
                if (size <= 0)
                        return 10;
                return Math.min(size, 100);
        }
}
