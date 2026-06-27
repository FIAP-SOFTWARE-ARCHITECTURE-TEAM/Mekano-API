package com.fiap.mekano.domain.valueobject;

import java.util.Locale;
import java.util.regex.Pattern;

import com.fiap.mekano.domain.exception.AppException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class PlacaVeiculo {

    private static final Pattern PLACA_PATTERN = Pattern.compile( "^(?:[A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2})$" );

    private final String value;

    public PlacaVeiculo(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, "Placa do veículo não pode ser nula ou vazia.");
        }
        
        String normalized = value .trim() .toUpperCase(Locale.ROOT) .replace("-", "");

        if (!PLACA_PATTERN.matcher(normalized).matches()) {
            throw new AppException(400, "Placa do veículo inválida: " + value);
        }
        this.value = normalized;
    }

}
