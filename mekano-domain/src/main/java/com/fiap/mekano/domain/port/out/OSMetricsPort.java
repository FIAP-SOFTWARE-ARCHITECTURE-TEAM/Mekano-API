package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.StatusOS;

import java.time.Duration;

/**
 * Output port para metricas de negocio de Ordens de Servico.
 * Implementado na camada de infrastructure (Adapter) usando Micrometer.
 */
public interface OSMetricsPort {

    /**
     * Registra a criacao de uma nova OS (counter os.criadas.total).
     */
    void registrarCriacaoOS();

    /**
     * Registra uma transicao de status bem-sucedida (counter os.status.alterado).
     *
     * @param de   status de origem (null para criacao)
     * @param para status de destino
     */
    void registrarTransicaoStatus(StatusOS de, StatusOS para);

    /**
     * Registra uma tentativa de transicao invalida (counter os.transicao.falha).
     *
     * @param statusAtual status atual da OS
     * @param tentado     status que se tentou transicionar
     */
    void registrarFalhaTransicao(StatusOS statusAtual, StatusOS tentado);

    /**
     * Registra a duracao de uma fase da OS (timer os.fase.duracao).
     *
     * @param fase    nome da fase (RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO,
     *                AGUARDANDO_EXECUCAO, EM_EXECUCAO, FINALIZADA, ENTREGUE, TOTAL)
     * @param duracao duracao medida
     */
    void registrarTempoFase(String fase, Duration duracao);

    /**
     * Incrementa gauge de OS por status atual (counter os.status.atual com tag status).
     *
     * @param status status atual da OS
     */
    void registrarOSPorStatus(String status);
}
