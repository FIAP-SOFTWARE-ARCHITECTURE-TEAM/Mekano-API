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
 * - Número da NF + série + CNPJ fornecedor formam uma chave única no sistema
 * - Data emissão não pode ser no futuro
 * - ICMS, IPI e impostos devem ser >= 0
 * - valorTotal deve ser >= valorMercadoria
 * - Imutável após criação: campos final, sem setters.
 *
 * Mapeamento JPA (NfEntradaEntity) é responsabilidade do módulo infrastructure.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class NfEntrada {

    private final UUID id;
    private final String numero;
    private final String serie;
    private final String cnpjFornecedor;
    private final String nomeFornecedor;
    private final LocalDateTime dataEmissao;
    private final BigDecimal valorMercadoria;
    private final BigDecimal icms;
    private final BigDecimal ipi;
    private final BigDecimal outrosImpostos;
    private final BigDecimal valorTotal;
    private final String chaveAcesso;
    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de uma nota fiscal.
     *
     * @param numero número da nota fiscal
     * @param serie série da nota fiscal (tipicamente "1")
     * @param cnpjFornecedor CNPJ do fornecedor (apenas dígitos)
     * @param nomeFornecedor nome comercial do fornecedor
     * @param dataEmissao data de emissão da NF (não pode ser no futuro)
     * @param valorMercadoria valor base das mercadorias
     * @param icms valor do imposto ICMS
     * @param ipi valor do imposto IPI
     * @param outrosImpostos valor de outros impostos
     * @param chaveAcesso chave de acesso NFe (44 dígitos)
     */
    public static NfEntrada create(String numero, String serie, String cnpjFornecedor, String nomeFornecedor,
                                   LocalDateTime dataEmissao, BigDecimal valorMercadoria,
                                   BigDecimal icms, BigDecimal ipi, BigDecimal outrosImpostos,
                                   String chaveAcesso) {
        validateNumero(numero);
        validateSerie(serie);
        validateCnpjFornecedor(cnpjFornecedor);
        validateNomeFornecedor(nomeFornecedor);
        validateDataEmissao(dataEmissao);
        validateValorMercadoria(valorMercadoria);
        validateIcms(icms);
        validateIpi(ipi);
        validateOutrosImpostos(outrosImpostos);
        validateChaveAcesso(chaveAcesso);

        BigDecimal total = calcularValorTotal(valorMercadoria, icms, ipi, outrosImpostos);

        return NfEntrada.builder()
                .id(UUID.randomUUID())
                .numero(numero.strip())
                .serie(serie.strip())
                .cnpjFornecedor(cnpjFornecedor)
                .nomeFornecedor(nomeFornecedor.strip())
                .dataEmissao(dataEmissao)
                .valorMercadoria(valorMercadoria)
                .icms(icms)
                .ipi(ipi)
                .outrosImpostos(outrosImpostos)
                .valorTotal(total)
                .chaveAcesso(chaveAcesso)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * NÃO gera novo UUID nem timestamp — preserva exatamente os valores do banco.
     */
    public static NfEntrada reconstitute(UUID id, String numero, String serie, String cnpjFornecedor,
                                         String nomeFornecedor, LocalDateTime dataEmissao, BigDecimal valorMercadoria,
                                         BigDecimal icms, BigDecimal ipi, BigDecimal outrosImpostos,
                                         BigDecimal valorTotal, String chaveAcesso, LocalDateTime createdAt) {
        validateNumero(numero);
        validateSerie(serie);
        validateCnpjFornecedor(cnpjFornecedor);
        validateNomeFornecedor(nomeFornecedor);
        validateDataEmissao(dataEmissao);
        validateValorMercadoria(valorMercadoria);
        validateIcms(icms);
        validateIpi(ipi);
        validateOutrosImpostos(outrosImpostos);
        validateChaveAcesso(chaveAcesso);

        BigDecimal calculado = calcularValorTotal(valorMercadoria, icms, ipi, outrosImpostos);
        if (valorTotal.compareTo(calculado) != 0) {
            throw new AppException(400, Messages.get("nf_entrada.valor_total.inconsistente", valorTotal, calculado));
        }

        return NfEntrada.builder()
                .id(id)
                .numero(numero.strip())
                .serie(serie.strip())
                .cnpjFornecedor(cnpjFornecedor)
                .nomeFornecedor(nomeFornecedor.strip())
                .dataEmissao(dataEmissao)
                .valorMercadoria(valorMercadoria)
                .icms(icms)
                .ipi(ipi)
                .outrosImpostos(outrosImpostos)
                .valorTotal(valorTotal)
                .chaveAcesso(chaveAcesso)
                .createdAt(createdAt)
                .build();
    }

    private static void validateNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.numero.required"));
        }
    }

    private static void validateSerie(String serie) {
        if (serie == null || serie.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.serie.required"));
        }
    }

    private static void validateCnpjFornecedor(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.cnpj_fornecedor.required"));
        }
        String cleaned = cnpj.replaceAll("[^0-9]", "");
        if (cleaned.length() != 14) {
            throw new AppException(400, Messages.get("nf_entrada.cnpj_fornecedor.invalid", cnpj));
        }
    }

    private static void validateNomeFornecedor(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new AppException(400, Messages.get("nf_entrada.nome_fornecedor.required"));
        }
    }

    private static void validateDataEmissao(LocalDateTime data) {
        if (data == null) {
            throw new AppException(400, Messages.get("nf_entrada.data_emissao.required"));
        }
        if (data.isAfter(LocalDateTime.now())) {
            throw new AppException(400, Messages.get("nf_entrada.data_emissao.futuro"));
        }
    }

    private static void validateValorMercadoria(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, Messages.get("nf_entrada.valor_mercadoria.invalido"));
        }
    }

    private static void validateIcms(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(400, Messages.get("nf_entrada.icms.invalido"));
        }
    }

    private static void validateIpi(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(400, Messages.get("nf_entrada.ipi.invalido"));
        }
    }

    private static void validateOutrosImpostos(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(400, Messages.get("nf_entrada.outros_impostos.invalido"));
        }
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

    /**
     * Calcula o valor total: mercadoria + ICMS + IPI + outros impostos.
     */
    private static BigDecimal calcularValorTotal(BigDecimal valorMercadoria, BigDecimal icms,
                                                 BigDecimal ipi, BigDecimal outrosImpostos) {
        return valorMercadoria
                .add(icms)
                .add(ipi)
                .add(outrosImpostos);
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
