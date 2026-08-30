package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class ItemOs {

    private final UUID id;
    private final UUID osUuid;
    private final UUID referenciaUuid;
    private final String tipo;
    private final String descricao;
    private final Long quantidade;
    private final LocalDateTime createdAt;
    private final Boolean isActive;

    public static ItemOs create(UUID osUuid, UUID referenciaUuid, String tipo,
                                String descricao, Long quantidade) {
        validateOsUuid(osUuid);
        validateReferenciaUuid(referenciaUuid);
        validateTipo(tipo);
        validateQuantidade(quantidade);

        return ItemOs.builder()
                .id(UUID.randomUUID())
                .osUuid(osUuid)
                .referenciaUuid(referenciaUuid)
                .tipo(tipo.toUpperCase().strip())
                .descricao(descricao != null ? descricao.strip() : null)
                .quantidade(quantidade != null ? quantidade : 1L)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    public static ItemOs reconstitute(UUID id, UUID osUuid, UUID referenciaUuid, String tipo,
                                      String descricao, Long quantidade, LocalDateTime createdAt,
                                      Boolean isActive) {
        validateOsUuid(osUuid);
        validateReferenciaUuid(referenciaUuid);
        validateTipo(tipo);

        Long qty = quantidade != null ? quantidade : 1L;
        if (qty <= 0) {
            throw new AppException(400, Messages.get("itemos.quantidade.invalida"));
        }

        return ItemOs.builder()
                .id(id)
                .osUuid(osUuid)
                .referenciaUuid(referenciaUuid)
                .tipo(tipo.toUpperCase().strip())
                .descricao(descricao)
                .quantidade(qty)
                .createdAt(createdAt)
                .isActive(isActive)
                .build();
    }

    public boolean isPeca() {
        return "PECA".equals(tipo);
    }

    public boolean isServico() {
        return "SERVICO".equals(tipo);
    }

    private static void validateOsUuid(UUID osUuid) {
        if (osUuid == null) {
            throw new AppException(400, Messages.get("itemos.os.required"));
        }
    }

    private static void validateReferenciaUuid(UUID referenciaUuid) {
        if (referenciaUuid == null) {
            throw new AppException(400, Messages.get("itemos.referencia.required"));
        }
    }

    private static void validateTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new AppException(400, Messages.get("itemos.tipo.required"));
        }
        String normalized = tipo.toUpperCase().strip();
        if (!Objects.equals(normalized, "PECA") && !Objects.equals(normalized, "SERVICO")) {
            throw new AppException(400, Messages.get("itemos.tipo.invalido", tipo));
        }
    }

    private static void validateQuantidade(Long quantidade) {
        if (quantidade != null && quantidade <= 0) {
            throw new AppException(400, Messages.get("itemos.quantidade.invalida"));
        }
    }
}
