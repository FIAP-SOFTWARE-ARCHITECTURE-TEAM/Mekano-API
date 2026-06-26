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
 * - Deve conter exatamente 11 dígitos (após remover formatação)
 * - Rejeita CPFs conhecidamente inválidos (11111111111, 00000000000, etc.)
 * - Valida algoritmo de checksum (módulo 11 para os dois dígitos verificadores)
 * - Armazenado sem formatação (apenas dígitos)
 *
 * Imutável por design: campo final, sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Cpf {

    private final String value;

    /**
     * Construtor com validação.
     * Lança {@link AppException} (400) se o valor for null, blank, mal formatado ou inválido.
     *
     * Aceita CPF com ou sem formatação (XXX.XXX.XXX-XX ou XXXXXXXXXXX).
     * Armazena apenas dígitos.
     *
     * @param value CPF a ser validado e armazenado
     */
    public Cpf(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("cpf.invalid.format", value == null ? "null" : value.strip()));
        }

        String cleaned = value.replaceAll("[^0-9]", "");

        if (cleaned.length() != 11) {
            throw new AppException(400, Messages.get("cpf.invalid.format", value.strip()));
        }

        if (isAllSameDigit(cleaned)) {
            throw new AppException(400, Messages.get("cpf.invalid.format", value.strip()));
        }

        if (!isValidChecksum(cleaned)) {
            throw new AppException(400, Messages.get("cpf.invalid.checksum", value.strip()));
        }

        this.value = cleaned;
    }

    /**
     * Verifica se todos os 11 dígitos são iguais (CPFs inválidos conhecidos).
     */
    private static boolean isAllSameDigit(String cpf) {
        return cpf.matches("(\\d)\\1{10}");
    }

    /**
     * Valida os dois dígitos verificadores do CPF via módulo 11.
     */
    private static boolean isValidChecksum(String cpf) {
        int[] digits = cpf.chars().map(Character::getNumericValue).toArray();

        int first = calculateCheckDigit(digits, 9);
        int second = calculateCheckDigit(digits, 10);

        return digits[9] == first && digits[10] == second;
    }

    /**
     * Calcula um dígito verificador do CPF.
     *
     * @param digits array de 11 dígitos
     * @param position posição do dígito a validar (9 ou 10)
     * @return dígito verificador esperado
     */
    private static int calculateCheckDigit(int[] digits, int position) {
        int sum = 0;
        int multiplier = position + 2;

        for (int i = 0; i < position; i++) {
            sum += digits[i] * (multiplier - i);
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /**
     * Retorna o CPF formatado como XXX.XXX.XXX-XX.
     */
    public String formatted() {
        return String.format("%s.%s.%s-%s",
                value.substring(0, 3),
                value.substring(3, 6),
                value.substring(6, 9),
                value.substring(9, 11));
    }
}
