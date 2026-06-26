package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

/**
 * Value Object que encapsula e valida um endereço brasileiro.
 *
 * Invariantes garantidas pelo construtor:
 * - Logradouro: não-null, não-blank, máx 100 caracteres
 * - Número: não-null, não-blank, máx 10 caracteres (pode conter letras: "100A")
 * - Bairro: não-null, não-blank, máx 50 caracteres
 * - Cidade: não-null, não-blank, máx 50 caracteres
 * - UF: exatamente 2 letras, normalizado para UPPERCASE (evita divergências São Paulo / sao paulo)
 * - CEP: exatamente 8 dígitos (sem formatação)
 *
 * Imutável por design: campos final, sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Endereco {

    private final String logradouro;
    private final String numero;
    private final String bairro;
    private final String cidade;
    private final String uf;
    private final String cep;

    /**
     * Construtor com validação.
     * Lança {@link AppException} (400) se qualquer campo violar os invariantes.
     *
     * @param logradouro nome da rua/avenida/etc
     * @param numero número do imóvel (pode incluir complementos como "100A")
     * @param bairro nome do bairro
     * @param cidade nome da cidade
     * @param uf unidade federativa (AC, AL, etc.)
     * @param cep código postal (apenas dígitos)
     */
    public Endereco(String logradouro, String numero, String bairro, String cidade, String uf, String cep) {
        this.logradouro = validateLogradouro(logradouro);
        this.numero = validateNumero(numero);
        this.bairro = validateBairro(bairro);
        this.cidade = validateCidade(cidade);
        this.uf = validateUf(uf);
        this.cep = validateCep(cep);
    }

    private static String validateLogradouro(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("endereco.logradouro.required"));
        }
        String trimmed = value.strip();
        if (trimmed.length() > 100) {
            throw new AppException(400, Messages.get("endereco.logradouro.max_length", "100"));
        }
        return trimmed;
    }

    private static String validateNumero(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("endereco.numero.required"));
        }
        String trimmed = value.strip();
        if (trimmed.length() > 10) {
            throw new AppException(400, Messages.get("endereco.numero.max_length", "10"));
        }
        return trimmed;
    }

    private static String validateBairro(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("endereco.bairro.required"));
        }
        String trimmed = value.strip();
        if (trimmed.length() > 50) {
            throw new AppException(400, Messages.get("endereco.bairro.max_length", "50"));
        }
        return trimmed;
    }

    private static String validateCidade(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("endereco.cidade.required"));
        }
        String trimmed = value.strip();
        if (trimmed.length() > 50) {
            throw new AppException(400, Messages.get("endereco.cidade.max_length", "50"));
        }
        return trimmed;
    }

    private static String validateUf(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("endereco.uf.required"));
        }
        String cleaned = value.toUpperCase(Locale.ROOT).strip();
        if (cleaned.length() != 2 || !cleaned.matches("[A-Z]{2}")) {
            throw new AppException(400, Messages.get("endereco.uf.invalid", value));
        }
        return cleaned;
    }

    private static String validateCep(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("endereco.cep.required"));
        }
        String cleaned = value.replaceAll("[^0-9]", "");
        if (cleaned.length() != 8) {
            throw new AppException(400, Messages.get("endereco.cep.invalid", value));
        }
        return cleaned;
    }

    /**
     * Retorna o CEP formatado como 12345-678.
     */
    public String cepFormatted() {
        return String.format("%s-%s", cep.substring(0, 5), cep.substring(5));
    }

    /**
     * Retorna endereço formatado para exibição.
     */
    public String formatted() {
        return String.format("%s, %s - %s, %s - %s %s",
                logradouro, numero, bairro, cidade, uf, cepFormatted());
    }
}
