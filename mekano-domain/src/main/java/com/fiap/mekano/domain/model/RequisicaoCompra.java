package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Entidade de domínio RequisicaoCompra — representa um pedido de compra de
 * insumos.
 *
 * Regras:
 * - Criação APENAS via factory methods {@link #criarParaOrcamento} ou
 * {@link #criarParaMinimo}.
 * - O builder é privado para forçar o uso dos factory methods.
 * - Requisição criada via criarParaOrcamento já nasce com status COMPRADA
 * (aprovação automática)
 * - Requisição criada via criarParaMinimo nasce com status ABERTA (aguardando
 * aprovação)
 * - Status pode transitar: ABERTA → ENVIADA → RECEBIDA; ou COMPRADA → RECEBIDA
 * - Imutável após criação: campos final, sem setters.
 * - Cada requisição pode conter múltiplos itens (peças/insumos).
 *
 * Mapeamento JPA (RequisicaoCompraEntity) é responsabilidade do módulo
 * infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class RequisicaoCompra {

    private final UUID id;
    private final List<ItemRequisicaoCompra> itens;
    private final StatusRequisicao status;
    private final MotivoRequisicao motivo;
    private final LocalDateTime createdAt;

    /**
     * Factory method para criar requisição vinculada a um orçamento.
     *
     * Requisições vinculadas a orçamentos já nascem com status COMPRADA,
     * pois presumem-se aprovadas como parte do orçamento do cliente.
     *
     * @param itens   lista de itens (peças/insumos) a comprar
     * @param motivo  razão da compra
     */
    public static RequisicaoCompra criarParaOrcamento(List<ItemRequisicaoCompra> itens, MotivoRequisicao motivo) {
        validateItens(itens);
        validateMotivo(motivo);

        return RequisicaoCompra.builder()
                .id(UUID.randomUUID())
                .itens(new ArrayList<>(itens))
                .status(StatusRequisicao.COMPRA_APROVADA)
                .motivo(motivo)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para criar requisição por reposição de estoque mínimo.
     *
     * Requisições por mínimo nascem com status ABERTA,
     * e requerem aprovação manual antes de serem enviadas ao fornecedor.
     *
     * @param itens   lista de itens (peças/insumos) a comprar
     * @param motivo  razão da compra
     */
    public static RequisicaoCompra criarParaMinimo(List<ItemRequisicaoCompra> itens, MotivoRequisicao motivo) {
        validateItens(itens);
        validateMotivo(motivo);

        return RequisicaoCompra.builder()
                .id(UUID.randomUUID())
                .itens(new ArrayList<>(itens))
                .status(StatusRequisicao.ABERTA)
                .motivo(motivo)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp — preserva exatamente os valores do banco.
     */
    public static RequisicaoCompra reconstitute(UUID id, List<ItemRequisicaoCompra> itens,
            StatusRequisicao status, MotivoRequisicao motivo, LocalDateTime createdAt) {
        validateMotivo(motivo);

        return RequisicaoCompra.builder()
                .id(id)
                .itens(itens == null ? Collections.emptyList() : new ArrayList<>(itens))
                .status(status)
                .motivo(motivo)
                .createdAt(createdAt)
                .build();
    }

    private static void validateItens(List<ItemRequisicaoCompra> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new AppException(400, Messages.get("requisicao_compra.itens.required"));
        }
    }

    private static void validateMotivo(MotivoRequisicao motivo) {
        if (motivo == null) {
            throw new AppException(400, Messages.get("requisicao_compra.motivo.required"));
        }
    }

    /**
     * Verifica se a requisição pode ser enviada (status ABERTA).
     */
    public boolean podeSerEnviada() {
        return status == StatusRequisicao.ABERTA;
    }

    /**
     * Verifica se a requisição pode ser recebida (status ENVIADA ou COMPRADA).
     */
    public boolean podeSerRecebida() {
        return status == StatusRequisicao.ENVIADA || status == StatusRequisicao.COMPRA_APROVADA;
    }
}
