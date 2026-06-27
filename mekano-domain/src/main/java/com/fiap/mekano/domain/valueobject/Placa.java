package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;

/**
 * Value Object que encapsula e valida uma placa de veículo brasileiro.
 *
 * Invariantes garantidas pelo construtor:
 * - Não aceita null nem blank
 * - Aceita padrões: LLLNNNN (3 letras + 4 números) — placa antiga
 * - Aceita padrões: LLNNNNLL (2 letras + 4 números + 2 letras) — placa Mercosul
 * - Normalizado para UPPERCASE com separador "-" removido
 * - Armazenado sem formatação, apenas alfanuméricos
 *
 * Imutável por design: campo final, sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Placa {

    private final String value;

    /**
     * Construtor com validação.
     * Lança {@link AppException} (400) se o valor for null, blank ou não corresponder aos padrões.
     *
     * Aceita placa com ou sem formatação (ABC-1234 ou ABC1234 ou ABC-1D23).
     * Armazena apenas alfanuméricos em UPPERCASE.
     *
     * @param value placa a ser validada e armazenada
     */
    public Placa(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("placa.invalid.format", value == null ? "null" : value.strip()));
        }

        String cleaned = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");

        if (!isValidPlaca(cleaned)) {
            throw new AppException(400, Messages.get("placa.invalid.format", value.strip()));
        }

        this.value = cleaned;
    }

    /**
     * Verifica se a placa corresponde a um dos padrões válidos.
     */
    private static boolean isValidPlaca(String cleaned) {
        if (cleaned.length() == 7) {
            return cleaned.matches("[A-Z]{3}[0-9]{4}");
        }
        if (cleaned.length() == 8) {
            return cleaned.matches("[A-Z]{2}[0-9]{4}[A-Z]{2}");
        }
        return false;
    }

    /**
     * Retorna a placa formatada.
     * Padrão antigo: ABC-1234
     * Padrão Mercosul: AB-1234-CD
     */
    public String formatted() {
        if (value.length() == 7) {
            return String.format("%s-%s", value.substring(0, 3), value.substring(3));
        }
        if (value.length() == 8) {
            return String.format("%s-%s-%s", value.substring(0, 2), value.substring(2, 6), value.substring(6));
        }
        return value;
    }

    /**
     * Retorna true se é placa Mercosul (8 caracteres).
     */
    public boolean isMercosul() {
        return value.length() == 8;
    }
}
