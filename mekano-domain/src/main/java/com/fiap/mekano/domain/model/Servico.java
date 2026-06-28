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
 * Entidade de domínio Servico — POJO puro sem anotações JPA.
 *
 * <p>Representa um tipo de serviço oferecido pela oficina mecânica (ex: troca de óleo,
 * alinhamento, balanceamento). Validação de valor > 0 no domínio.
 *
 * <p>Regras:
 * <ul>
 *   <li>Criação APENAS via factory method {@link #create(String, String, BigDecimal)}.</li>
 *   <li>Atualização via {@link #atualizar(String, String, BigDecimal)}.</li>
 *   <li>Reconstrução de dados persistidos via {@link #reconstitute}.</li>
 *   <li>Imutável após criação (exceto via atualizar()).</li>
 * </ul>
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class Servico {

    private final UUID id;
    private String nome;
    private String descricao;
    private BigDecimal valor;
    private final LocalDateTime createdAt;

    /**
     * Factory method — cria um novo serviço com validação de negócio.
     *
     * @param nome      nome do serviço (obrigatório)
     * @param descricao descrição opcional do serviço
     * @param valor     preço do serviço (deve ser > 0)
     * @return nova instância de Servico
     * @throws AppException se nome nulo/vazio ou valor <= 0
     */
    public static Servico create(String nome, String descricao, BigDecimal valor) {
        validateNome(nome);
        validateValor(valor);

        return Servico.builder()
                .id(UUID.randomUUID())
                .nome(nome.strip())
                .descricao(descricao != null ? descricao.strip() : null)
                .valor(valor)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp.
     */
    public static Servico reconstitute(UUID id, String nome, String descricao,
                                        BigDecimal valor, LocalDateTime createdAt) {
        return Servico.builder()
                .id(id)
                .nome(nome)
                .descricao(descricao)
                .valor(valor)
                .createdAt(createdAt)
                .build();
    }

    /**
     * Atualiza os campos mutáveis do serviço com validação.
     *
     * @param nome      novo nome (obrigatório)
     * @param descricao nova descrição (pode ser null)
     * @param valor     novo valor (deve ser > 0)
     * @throws AppException se nome nulo/vazio ou valor <= 0
     */
    public void atualizar(String nome, String descricao, BigDecimal valor) {
        validateNome(nome);
        validateValor(valor);

        this.nome = nome.strip();
        this.descricao = descricao != null ? descricao.strip() : null;
        this.valor = valor;
    }

    private static void validateNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new AppException(400, Messages.get("servico.nome.required"));
        }
    }

    private static void validateValor(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, Messages.get("servico.valor.invalid"));
        }
    }
}
