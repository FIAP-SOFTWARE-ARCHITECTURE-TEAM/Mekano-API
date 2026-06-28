package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.OrdemDeServico;

import java.util.List;
import java.util.UUID;

/**
 * Input port — contrato dos serviços de gerenciamento de Ordem de Serviço.
 */
public interface OrdemDeServicoServicePort {

    OrdemDeServico create(CreateOrdemDeServicoCommand command);

    OrdemDeServico findById(UUID id);

    List<OrdemDeServico> findAll(int page, int size, String sort);

    long countAll();

    OrdemDeServico iniciarDiagnostico(UUID id);

    OrdemDeServico finalizarDiagnostico(UUID id);

    OrdemDeServico aprovarOrcamento(UUID id);

    OrdemDeServico reprovarOrcamento(UUID id, String motivo);

    OrdemDeServico cancelar(UUID id, String motivo);

    OrdemDeServico finalizar(UUID id);

    OrdemDeServico entregar(UUID id);
}
