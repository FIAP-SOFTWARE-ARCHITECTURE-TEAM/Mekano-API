package com.fiap.mekano.rest.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.in.CreateVeiculoCommand;
import com.fiap.mekano.domain.port.in.UpdateVeiculoCommand;
import com.fiap.mekano.domain.port.in.VeiculoServicePort;
import com.fiap.mekano.rest.api.dto.CreateVeiculoRequest;
import com.fiap.mekano.rest.api.dto.UpdateVeiculoRequest;
import com.fiap.mekano.rest.api.dto.VeiculoPageResponse;
import com.fiap.mekano.rest.api.dto.VeiculoResponse;
import com.fiap.mekano.rest.api.mapper.VeiculoDtoMapper;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/veiculos")
@RequestScoped
@RolesAllowed({ "admin", "atendente" })
@Tag(name = "Veículos", description = "Gerenciamento de veículos")
public class VeiculoResource {

        @Inject
        private VeiculoServicePort veiculoService;

        @Inject
        private VeiculoDtoMapper veiculoDtoMapper;

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Cadastrar veículo", description = "Cadastra um veículo vinculado a um cliente existente.")
        public Response create(
                        @Valid CreateVeiculoRequest request,
                        @Context UriInfo uriInfo) {

                CreateVeiculoCommand command = veiculoDtoMapper.toCommand(request);

                Veiculo veiculo = veiculoService.execute(command);

                VeiculoResponse response = veiculoDtoMapper.toResponse(veiculo);

                URI location = uriInfo
                                .getAbsolutePathBuilder()
                                .path(response.id().toString())
                                .build();

                return Response
                                .created(location)
                                .entity(response)
                                .build();
        }

        @PUT
        @Path("/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Atualizar veículo", description = "Atualiza os dados de um veículo existente.")
        public Response update(
                        @PathParam("id") UUID id,
                        @Valid UpdateVeiculoRequest request) {

                UpdateVeiculoCommand command = veiculoDtoMapper.toCommand(request);

                Veiculo veiculo = veiculoService.update(id, command);

                return Response.ok(
                                veiculoDtoMapper.toResponse(veiculo)).build();
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Listar veículos", description = "Lista todos os veículos cadastrados, com paginação e ordenação.")
        public Response listAll(
                        @QueryParam("page") @DefaultValue("0") int page,

                        @QueryParam("size") @DefaultValue("10") int size,

                        @QueryParam("sort") @DefaultValue("placa,asc") String sort) {

                List<VeiculoResponse> content = veiculoService.findAll(page, size, sort)
                                .stream()
                                .map(veiculoDtoMapper::toResponse)
                                .toList();

                long total = veiculoService.countAll();

                int totalPages = (int) Math.ceil((double) total / size);

                VeiculoPageResponse response = new VeiculoPageResponse(
                                content,
                                page,
                                size,
                                total,
                                totalPages);

                return Response.ok(response).build();
        }

        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @Operation(summary = "Buscar veículo por ID", description = "Retorna os dados de um veículo específico com base no ID informado.")
        public Response findById(
                        @PathParam("id") UUID id) {

                Veiculo veiculo = veiculoService.findById(id);

                return Response.ok(
                                veiculoDtoMapper.toResponse(veiculo)).build();
        }

        @DELETE
        @Path("/{id}")
        @Operation(summary = "Remover veículo", description = "Remove logicamente um veículo existente com base no ID informado.")
        public Response delete(
                        @PathParam("id") UUID id) {

                veiculoService.delete(id);

                return Response.noContent().build();
        }

        @PUT
        @Path("/{id}/ativar")
        @Operation(summary = "Reativar veículo", description = "Reativa um veículo inativo. Se o veículo já estiver ativo, nenhuma alteração é feita.")
        public Response reativar(
                        @PathParam("id") UUID id) {

                veiculoService.reactivate(id);

                return Response.noContent().build();
        }
}
