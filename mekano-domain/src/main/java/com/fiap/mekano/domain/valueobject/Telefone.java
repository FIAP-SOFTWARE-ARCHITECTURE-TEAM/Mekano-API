package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class Telefone {

    private final String value;

    public Telefone(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("telefone.invalid", value == null ? "null" : value.strip()));
        }
        String digits = value.strip().replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw new AppException(400, Messages.get("telefone.invalid", value.strip()));
        }
        this.value = digits;
    }
}
