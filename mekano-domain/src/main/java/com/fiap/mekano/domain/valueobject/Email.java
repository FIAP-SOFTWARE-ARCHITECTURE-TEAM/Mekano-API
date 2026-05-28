package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.InvalidEmailException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value Object que encapsula e valida um endereço de email.
 *
 * Invariantes garantidas pelo construtor:
 * - Não aceita null nem blank
 * - Deve corresponder ao padrão RFC 5322 simplificado
 * - Normalizado para lowercase (evita duplicatas USER@FIAP.BR vs user@fiap.br)
 *
 * Imutável por design: campo final, sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Email {

    /**
     * Regex RFC 5322 simplificado — cobre 99%+ dos casos sem complexidade de backtracking.
     * Thread-safe: Pattern é compilado uma única vez como constante estática.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final String value;

    /**
     * Construtor com validação.
     * Lança {@link InvalidEmailException} se o valor for null, blank ou não corresponder ao padrão.
     *
     * @param value endereço de email a ser validado e armazenado
     */
    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException(value == null ? "null" : value.strip());
        }
        String trimmed = value.strip();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidEmailException(trimmed);
        }
        this.value = trimmed.toLowerCase(Locale.ROOT);
    }
}
