package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.OrdemDeServico;

import java.time.LocalDateTime;

/**
 * Evento de domínio disparado quando uma nova Ordem de Serviço é criada.
 */
public record OrdemDeServicoCriadaEvent(
        OrdemDeServico ordemDeServico,
        LocalDateTime occurredAt
) {
    public static OrdemDeServicoCriadaEvent of(OrdemDeServico os) {
        return new OrdemDeServicoCriadaEvent(os, LocalDateTime.now());
    }
}
