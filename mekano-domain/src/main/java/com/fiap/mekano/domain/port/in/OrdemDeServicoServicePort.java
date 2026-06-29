package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.OrdemDeServico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemDeServicoServicePort {

    OrdemDeServico criar(CriarOSCommand command);

    OrdemDeServico iniciarDiagnostico(UUID osUuid);

    OrdemDeServico finalizarDiagnostico(UUID osUuid);

    OrdemDeServico iniciarExecucao(IniciarExecucaoCommand command);

    OrdemDeServico finalizarExecucao(FinalizarExecucaoCommand command);

    OrdemDeServico entregar(UUID osUuid);

    OrdemDeServico cancelar(CancelarOSCommand command);

    OrdemDeServico buscarPorId(UUID id);

    List<OrdemDeServico> listar(int page, int size, String sort);

    List<OrdemDeServico> listarComFiltros(String status, UUID clienteUuid, int page, int size);

    long contar();

    Optional<Double> calcularTempoMedioExecucao();

    void deletar(UUID id);
}
