package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Value Object que encapsula e valida um CPF brasileiro.
 *
 * Invariantes garantidas pelo construtor:
 * - Não aceita null nem blank
 * - Normalizado para apenas dígitos (remove máscara)
 * - Exatamente 11 dígitos
 * - Rejeita sequências com todos os dígitos iguais (ex.: 111.111.111-11)
 * - Valida dígitos verificadores (módulo 11)
 *
 * Imutável por design: campo final, sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Cpf {

    private final String value;

    public Cpf(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("cpf.invalid", value == null ? "null" : value.strip()));
        }
        String digits = value.strip().replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new AppException(400, Messages.get("cpf.invalid", value.strip()));
        }
        if (digits.chars().distinct().count() == 1) {
            throw new AppException(400, Messages.get("cpf.invalid", value.strip()));
        }
        if (!isValidCheckDigits(digits)) {
            throw new AppException(400, Messages.get("cpf.invalid", value.strip()));
        }
        this.value = digits;
    }

    private static boolean isValidCheckDigits(String digits) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * (10 - i);
        }
        int first = 11 - (sum % 11);
        if (first >= 10) first = 0;
        if (first != (digits.charAt(9) - '0')) return false;

        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (digits.charAt(i) - '0') * (11 - i);
        }
        int second = 11 - (sum % 11);
        if (second >= 10) second = 0;
        return second == (digits.charAt(10) - '0');
    }
}
