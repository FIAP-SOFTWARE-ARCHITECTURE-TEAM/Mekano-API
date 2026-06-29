package com.fiap.mekano.rest.api.mapper;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.CancelarOSCommand;
import com.fiap.mekano.domain.port.in.CriarOSCommand;
import com.fiap.mekano.domain.port.in.FinalizarExecucaoCommand;
import com.fiap.mekano.domain.port.in.IniciarExecucaoCommand;
import com.fiap.mekano.rest.api.dto.CancelarOSRequest;
import com.fiap.mekano.rest.api.dto.CreateOrdemDeServicoRequest;
import com.fiap.mekano.rest.api.dto.FinalizarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.IniciarExecucaoRequest;
import com.fiap.mekano.rest.api.dto.OrdemDeServicoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface OrdemDeServicoDtoMapper {

    CriarOSCommand toCreateCommand(CreateOrdemDeServicoRequest request);

    IniciarExecucaoCommand toIniciarExecucaoCommand(IniciarExecucaoRequest request);

    @Mapping(target = "observacao", source = "observacao")
    FinalizarExecucaoCommand toFinalizarExecucaoCommand(FinalizarExecucaoRequest request);

    CancelarOSCommand toCancelarCommand(CancelarOSRequest request);

    OrdemDeServicoResponse toResponse(OrdemDeServico os);
}