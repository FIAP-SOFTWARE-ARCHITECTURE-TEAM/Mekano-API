package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
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
 *
 * Mapeamento JPA (RequisicaoCompraEntity) é responsabilidade do módulo
 * infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class RequisicaoCompra {

    private final UUID id;
    private final UUID pecaId;
    private final Long quantidade;
    private final StatusRequisicao status;
    private final MotivoRequisicao motivo;
    private final LocalDateTime createdAt;

    /**
     * Factory method para criar requisição vinculada a um orçamento.
     *
     * Requisições vinculadas a orçamentos já nascem com status COMPRADA,
     * pois presumem-se aprovadas como parte do orçamento do cliente.
     *
     * @param pecaId     referência à peça
     * @param quantidade quantidade a comprar
     * @param motivo     razão da compra
     */
    public static RequisicaoCompra criarParaOrcamento(UUID pecaId, Long quantidade, MotivoRequisicao motivo) {
        validatePecaId(pecaId);
        validateQuantidade(quantidade);
        validateMotivo(motivo);

        return RequisicaoCompra.builder()
                .id(UUID.randomUUID())
                .pecaId(pecaId)
                .quantidade(quantidade)
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
     * @param pecaId     referência à peça
     * @param quantidade quantidade a comprar
     * @param motivo     razão da compra
     */
    public static RequisicaoCompra criarParaMinimo(UUID pecaId, Long quantidade, MotivoRequisicao motivo) {
        validatePecaId(pecaId);
        validateQuantidade(quantidade);
        validateMotivo(motivo);

        return RequisicaoCompra.builder()
                .id(UUID.randomUUID())
                .pecaId(pecaId)
                .quantidade(quantidade)
                .status(StatusRequisicao.ABERTA)
                .motivo(motivo)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp — preserva exatamente os valores do banco.
     */
    public static RequisicaoCompra reconstitute(UUID id, UUID pecaId, Long quantidade,
            StatusRequisicao status, MotivoRequisicao motivo, LocalDateTime createdAt) {
        validatePecaId(pecaId);
        validateQuantidade(quantidade);
        validateMotivo(motivo);

        return RequisicaoCompra.builder()
                .id(id)
                .pecaId(pecaId)
                .quantidade(quantidade)
                .status(status)
                .motivo(motivo)
                .createdAt(createdAt)
                .build();
    }

    private static void validatePecaId(UUID pecaId) {
        if (pecaId == null) {
            throw new AppException(400, Messages.get("requisicao_compra.peca_id.required"));
        }
    }

    private static void validateQuantidade(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new AppException(400, Messages.get("requisicao_compra.quantidade.invalida"));
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
