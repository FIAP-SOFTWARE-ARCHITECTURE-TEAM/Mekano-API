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
 * - Logradouro e cidade são obrigatórios (não null/blank)
 * - UF normalizada para uppercase via Locale.ROOT (2 caracteres)
 * - CEP normalizado para apenas dígitos (8 dígitos)
 * - Numero e bairro são opcionais (nullable)
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

    public Endereco(String logradouro, String numero, String bairro,
                    String cidade, String uf, String cep) {
        if (logradouro == null || logradouro.isBlank()) {
            throw new AppException(400, Messages.get("endereco.logradouro.required"));
        }
        if (cidade == null || cidade.isBlank()) {
            throw new AppException(400, Messages.get("endereco.cidade.required"));
        }
        if (uf == null || uf.strip().length() != 2) {
            throw new AppException(400, Messages.get("endereco.uf.invalid"));
        }
        if (cep == null || cep.strip().replaceAll("\\D", "").length() != 8) {
            throw new AppException(400, Messages.get("endereco.cep.invalid"));
        }
        this.logradouro = logradouro.strip();
        this.numero = numero == null ? null : numero.strip();
        this.bairro = bairro == null ? null : bairro.strip();
        this.cidade = cidade.strip();
        this.uf = uf.strip().toUpperCase(Locale.ROOT);
        this.cep = cep.strip().replaceAll("\\D", "");
    }
}
