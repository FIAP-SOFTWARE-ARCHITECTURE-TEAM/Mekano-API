package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.OrdemDeServico;
import java.time.LocalDateTime;

public record OrdemDeServicoCriadaEvent(OrdemDeServico ordemDeServico, LocalDateTime occurredAt) {
    public static OrdemDeServicoCriadaEvent of(OrdemDeServico os) {
        return new OrdemDeServicoCriadaEvent(os, LocalDateTime.now());
    }
}
