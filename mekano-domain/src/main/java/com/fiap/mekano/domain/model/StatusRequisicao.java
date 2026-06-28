package com.fiap.mekano.domain.model;

/**
 * Estados possíveis de uma RequisicaoCompra no ciclo de vida do pedido.
 *
 * Transições esperadas:
 * - ABERTA → ENVIADA → RECEBIDA
 * - COMPRADA → RECEBIDA (pois já foi processada, salta ENVIADA)
 * - Qualquer estado → CANCELADA (em caso de cancelamento)
 */
public enum StatusRequisicao {
    ABERTA("Aberta", "Aguardando aprovação de compra"),
    ENVIADA("Enviada", "Pedido enviado ao fornecedor"),
    COMPRADA("Comprada", "Compra já aprovada/processada"),
    RECEBIDA("Recebida", "Mercadoria recebida no estoque"),
    CANCELADA("Cancelada", "Requisição cancelada");

    private final String descricao;
    private final String mensagem;

    StatusRequisicao(String descricao, String mensagem) {
        this.descricao = descricao;
        this.mensagem = mensagem;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getMensagem() {
        return mensagem;
    }
}
