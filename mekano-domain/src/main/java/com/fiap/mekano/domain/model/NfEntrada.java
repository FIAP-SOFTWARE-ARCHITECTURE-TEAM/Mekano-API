package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio NfEntrada — representa uma Nota Fiscal de entrada de mercadoria.
 *
 * Regras:
 * - Criação APENAS via factory method {@link #create} ou {@link #reconstitute}.
 * - O builder é privado para forçar o uso dos factory methods.
 * - Chave de acesso NFe (44 dígitos) é obrigatória
 * - Imutável após criação: campos final, sem setters.
 *
 * Mapeamento JPA (NfEntradaEntity) é responsabilidade do módulo infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class NfEntrada {

    private final UUID id;
    private final String chaveAcesso;
    private final BigDecimal valorTotal;
    private final UUID pecaId;
    private final UUID requisicaoCompraId;
    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de uma nota fiscal.
     */
    public static NfEntrada create(String chaveAcesso, BigDecimal valorTotal,
                                   UUID pecaId, UUID requisicaoCompraId) {
        validateChaveAcesso(chaveAcesso);
        validateValorTotal(valorTotal);
        validatePecaId(pecaId);
        validateRequisicaoCompraId(requisicaoCompraId);

        return NfEntrada.builder()
                .id(UUID.randomUUID())
                .chaveAcesso(chaveAcesso)
                .valorTotal(valorTotal)
                .pecaId(pecaId)
                .requisicaoCompraId(requisicaoCompraId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     */
    public static NfEntrada reconstitute(UUID id, String chaveAcesso, BigDecimal valorTotal,
                                         UUID pecaId, UUID requisicaoCompraId,
                                         LocalDateTime createdAt) {
        validateChaveAcesso(chaveAcesso);
        validateValorTotal(valorTotal);
        validatePecaId(pecaId);
        validateRequisicaoCompraId(requisicaoCompraId);

        return NfEntrada.builder()
                .id(id)
                .chaveAcesso(chaveAcesso)
                .valorTotal(valorTotal)
                .pecaId(pecaId)
                .requisicaoCompraId(requisicaoCompraId)
                .createdAt(createdAt)
                .build();
    }

    private static void validateChaveAcesso(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.chave_acesso.required"));
        }
        String cleaned = chave.replaceAll("[^0-9]", "");
        if (cleaned.length() != 44) {
            throw new AppException(400, Messages.get("nf_entrada.chave_acesso.invalid", chave));
        }
    }

    private static void validateValorTotal(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, Messages.get("nf_entrada.valor_mercadoria.invalido"));
        }
    }

    private static void validatePecaId(UUID pecaId) {
        if (pecaId == null) {
            throw new AppException(400, "ID da peça é obrigatório");
        }
    }

    private static void validateRequisicaoCompraId(UUID requisicaoCompraId) {
        if (requisicaoCompraId == null) {
            throw new AppException(400, "ID da requisição de compra é obrigatório");
        }
    }

    /**
     * Retorna a chave de acesso formatada (XX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXX).
     */
    public String chaveAcessoFormatada() {
        if (chaveAcesso == null || chaveAcesso.length() != 44) {
            return chaveAcesso;
        }
        return String.format("%s.%s.%s.%s.%s.%s.%s.%s.%s.%s",
                chaveAcesso.substring(0, 2),
                chaveAcesso.substring(2, 6),
                chaveAcesso.substring(6, 10),
                chaveAcesso.substring(10, 14),
                chaveAcesso.substring(14, 18),
                chaveAcesso.substring(18, 22),
                chaveAcesso.substring(22, 26),
                chaveAcesso.substring(26, 30),
                chaveAcesso.substring(30, 34),
                chaveAcesso.substring(34, 44));
    }
}
