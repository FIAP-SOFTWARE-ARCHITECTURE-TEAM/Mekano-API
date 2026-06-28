package com.fiap.mekano.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Enum com a máquina de estados da Ordem de Serviço.
 *
 * <p>7 estados, sem APROVADA (INC-01 — aprovação vai direto para EM_EXECUCAO).
 * A matriz {@link #TRANSICOES} é a fonte única da verdade para transições válidas.
 * ENTREGUE e CANCELADA são estados terminais (nenhuma saída).
 *
 * <p>Transições são validadas via {@link #podeTransicionarPara(StatusOS)}.
 * Nunca usar setStatus() — métodos explícitos na entidade OrdemDeServico (D-26).
 */
public enum StatusOS {

    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    /**
     * Matriz de transições — Map<origem, Set<destinos_validos>>.
     * Fonte única da verdade para a máquina de estados.
     */
    private static final Map<StatusOS, Set<StatusOS>> TRANSICOES = Map.of(
            RECEBIDA, Set.of(EM_DIAGNOSTICO, CANCELADA),
            EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, Set.of(FINALIZADA, CANCELADA),
            FINALIZADA, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADA, Set.of()
    );

    /**
     * Verifica se a transição deste estado para o destino é válida.
     *
     * @param destino estado alvo
     * @return true se a transição é permitida
     */
    public boolean podeTransicionarPara(StatusOS destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
