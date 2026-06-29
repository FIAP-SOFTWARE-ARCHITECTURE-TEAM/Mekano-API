package com.fiap.mekano.domain.os;

import java.util.Map;
import java.util.Set;

public enum OsStatus {
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
    private static final Map<OsStatus, Set<OsStatus>> TRANSICOES = Map.of(
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
    public boolean podeTransicionarPara(OsStatus destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}