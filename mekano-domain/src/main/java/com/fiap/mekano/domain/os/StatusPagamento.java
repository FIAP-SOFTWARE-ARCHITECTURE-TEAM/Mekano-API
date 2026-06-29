package com.fiap.mekano.domain.os;

import java.util.Map;
import java.util.Set;

public enum StatusPagamento {

    NAO_COBRADO,
    AGUARDANDO_PAGAMENTO,
    CONFIRMADO,
    CANCELADO;

    private static final Map<StatusPagamento, Set<StatusPagamento>> TRANSICOES = Map.of(
            NAO_COBRADO, Set.of(AGUARDANDO_PAGAMENTO, CANCELADO),
            AGUARDANDO_PAGAMENTO, Set.of(CONFIRMADO, CANCELADO),
            CONFIRMADO, Set.of(),
            CANCELADO, Set.of()
    );

    public boolean podeTransicionarPara(StatusPagamento destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
