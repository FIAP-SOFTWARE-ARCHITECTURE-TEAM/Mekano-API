package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio Peca — representa uma peça ou insumo do estoque.
 *
 * Regras:
 * - Criação APENAS via factory method {@link #create} ou {@link #reconstitute}.
 * - O builder é privado para forçar o uso dos factory methods.
 * - Saldo nunca fica negativo — {@link #debitarSaldo} valida antes de decrementar.
 * - Estoque mínimo é opcional (nullable) — peças sem mínimo não geram alerta.
 * - Imutável após criação: campos final, sem setters.
 *
 * Métodos de manipulação (debitarSaldo, creditarSaldo) retornam o novo saldo,
 * não modificam estado — o caller é responsável por persistir.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Peca {

    private final UUID id;
    private final String codigo;
    private final String descricao;
    private final BigDecimal valorUnitario;
    private final Long saldoAtual;
    private final Long estoqueMinimo;
    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de uma nova peça.
     *
     * @param codigo identificador único da peça (ex: "PEA-001")
     * @param descricao nome/descrição da peça
     * @param valorUnitario preço unitário
     * @param estoqueMinimo quantidade mínima (opcional, pode ser null)
     */
    public static Peca create(String codigo, String descricao,
                              BigDecimal valorUnitario, Long estoqueMinimo) {
        validateCodigo(codigo);
        validateDescricao(descricao);
        validateValorUnitario(valorUnitario);

        return Peca.builder()
                .id(UUID.randomUUID())
                .codigo(codigo.strip())
                .descricao(descricao.strip())
                .valorUnitario(Objects.requireNonNullElse(valorUnitario, BigDecimal.ZERO))
                .saldoAtual(0L)
                .estoqueMinimo(estoqueMinimo)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp — preserva exatamente os valores do banco.
     */
    public static Peca reconstitute(UUID id, String codigo, String descricao,
                                    BigDecimal valorUnitario,
                                    Long saldoAtual, Long estoqueMinimo, LocalDateTime createdAt) {
        validateCodigo(codigo);
        validateDescricao(descricao);
        validateValorUnitario(valorUnitario);

        if (saldoAtual == null || saldoAtual < 0) {
            throw new AppException(400, Messages.get("peca.saldo.negativo", codigo));
        }

        return Peca.builder()
                .id(id)
                .codigo(codigo.strip())
                .descricao(descricao.strip())
                .valorUnitario(valorUnitario)
                .saldoAtual(saldoAtual)
                .estoqueMinimo(estoqueMinimo)
                .createdAt(createdAt)
                .build();
    }

    private static void validateCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new AppException(400, Messages.get("peca.codigo.required"));
        }
    }

    private static void validateDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new AppException(400, Messages.get("peca.descricao.required"));
        }
    }

    private static void validateValorUnitario(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(400, Messages.get("peca.valor.invalido"));
        }
    }

    /**
     * Debita (decrementa) o saldo de peças.
     * Valida que o novo saldo não ficará negativo antes de permitir.
     *
     * @param quantidade quantidade a debitar
     * @return novo saldo após a operação
     * @throws AppException se quantidade > saldoAtual
     */
    public Long debitarSaldo(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new AppException(400, Messages.get("peca.quantidade.invalida"));
        }
        if (quantidade > saldoAtual) {
            throw new AppException(400, Messages.get("peca.saldo.insuficiente", codigo, saldoAtual, quantidade));
        }
        return saldoAtual - quantidade;
    }

    /**
     * Credita (incrementa) o saldo de peças.
     *
     * @param quantidade quantidade a creditar
     * @return novo saldo após a operação
     * @throws AppException se quantidade <= 0
     */
    public Long creditarSaldo(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new AppException(400, Messages.get("peca.quantidade.invalida"));
        }
        return saldoAtual + quantidade;
    }

    /**
     * Verifica se o saldo caiu abaixo do estoque mínimo.
     * Retorna false se estoqueMinimo for null (peça sem mínimo configurado).
     */
    public boolean isEstoqueMinimoAtingido() {
        return estoqueMinimo != null && saldoAtual < estoqueMinimo;
    }
}
