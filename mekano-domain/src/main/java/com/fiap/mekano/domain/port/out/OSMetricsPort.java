package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.StatusOS;

import java.time.Duration;

/**
 * Output port para métricas de negócio de Ordens de Serviço.
 * Implementado na camada de infrastructure (Adapter) usando Micrometer.
 */
public interface OSMetricsPort {

    /**
     * Registra a criação de uma nova OS (counter os.criadas.total).
     */
    void registrarCriacaoOS();

    /**
     * Registra uma transição de status bem-sucedida (counter os.status.alterado).
     *
     * @param de   status de origem (null para criação)
     * @param para status de destino
     */
    void registrarTransicaoStatus(StatusOS de, StatusOS para);

    /**
     * Registra uma tentativa de transição inválida (counter os.transicao.falha).
     *
     * @param statusAtual status atual da OS
     * @param tentado     status que se tentou transicionar
     */
    void registrarFalhaTransicao(StatusOS statusAtual, StatusOS tentado);

    /**
     * Registra a duração de uma fase da OS (timer os.duracao.fase).
     *
     * @param fase    nome da fase (DIAGNÓSTICO, EXECUCAO, TOTAL)
     * @param duracao duração medida
     */
    void registrarTempoFase(String fase, Duration duracao);
}
