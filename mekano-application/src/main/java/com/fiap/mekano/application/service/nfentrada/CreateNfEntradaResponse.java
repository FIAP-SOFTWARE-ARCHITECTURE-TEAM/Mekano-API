package com.fiap.mekano.application.service.nfentrada;

import java.util.UUID;
import java.time.LocalDateTime;

public record CreateNfEntradaResponse(
    UUID id,
    Integer quantidade,
    LocalDateTime dataRecebimento
) {}
