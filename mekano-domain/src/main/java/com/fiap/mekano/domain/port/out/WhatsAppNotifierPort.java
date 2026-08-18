package com.fiap.mekano.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Porta de saída para notificações WhatsApp.
 *
 * <p>Implementada no módulo infrastructure (EvolutionApiNotifier) — o domínio
 * depende apenas desta interface, sem framework (Clean Architecture D-02).
 *
 * <p>Regras de PII (V8 Data Protection): quem implementa deve mascarar o
 * telefone em logs — nunca logar o número completo.
 */
public interface WhatsAppNotifierPort {

    /**
     * Notifica o cliente que o orçamento está pronto para aprovação.
     *
     * @param telefone     dígitos crus (10-11), sem máscara nem DDI
     * @param nomeCliente  nome para saudação na mensagem
     * @param marca        marca do veículo (identifica o orçamento no WhatsApp)
     * @param modelo       modelo do veículo
     * @param placa        placa do veículo (ex.: ABC1D23)
     * @param valorTotal   valor total do orçamento para exibição na mensagem
     */
    void notificarOrcamento(String telefone, String nomeCliente, String marca, String modelo,
                            String placa, BigDecimal valorTotal);

    /**
     * Notifica o cliente sobre a resposta SIM/NÃO ao orçamento enviado via WhatsApp.
     *
     * @param telefone     dígitos crus (10-11), sem máscara nem DDI
     * @param aprovado     true = orçamento aprovado, false = reprovado
     */
    void notificarRespostaOrcamento(String telefone, boolean aprovado);

    /**
     * Notifica o cliente que o veículo está pronto para retirada.
     *
     * @param telefone  dígitos crus (10-11), sem máscara nem DDI
     * @param nomeCliente nome para saudação na mensagem
     * @param placa     placa formatada do veículo (ex.: ABC1D23) para exibição
     * @param osUuid    UUID da ordem de serviço (link de acompanhamento)
     */
    void notificarRetirada(String telefone, String nomeCliente, String placa, UUID osUuid);
}