package com.fiap.mekano.domain.valueobject;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

/**
 * Value Object que encapsula e valida um número de telefone brasileiro.
 *
 * Invariantes garantidas pelo construtor:
 * - Não aceita null nem blank
 * - Normalizado para apenas dígitos (remove máscara)
 * - Deve ter 10 (fixo) ou 11 (celular) dígitos
 * - DDD deve ser válido conforme ANATEL
 *
 * Imutável por design: campo final, sem setters, @EqualsAndHashCode por valor.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Telefone {

    private static final Set<String> VALID_DDDS = Set.of(
            "11","12","13","14","15","16","17","18","19",
            "21","22","24","27","28",
            "31","32","33","34","35","37","38",
            "41","42","43","44","45","46",
            "47","48","49",
            "51","53","54","55",
            "61","62","63","64","65","66","67","68","69",
            "71","73","74","75","77","79",
            "81","82","83","84","85","86","87","88","89",
            "91","92","93","94","95","96","97","98","99"
    );

    private final String value;

    public Telefone(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("telefone.invalid", value == null ? "null" : value.strip()));
        }
        String digits = value.strip().replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw new AppException(400, Messages.get("telefone.invalid", value.strip()));
        }
        String ddd = digits.substring(0, 2);
        if (!VALID_DDDS.contains(ddd)) {
            throw new AppException(400, Messages.get("telefone.invalid", value.strip()));
        }
        this.value = digits;
    }
}
