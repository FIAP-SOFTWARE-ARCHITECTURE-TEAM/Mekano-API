package com.fiap.mekano.infrastructure.whatsapp.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resposta da Evolution API para sendText. Campos não são
 * consumidos pelo Mekano (fire-and-forget) — existem para desserialização.
 */
public record SendMessageResponse(JsonNode key, JsonNode message, String status) {
}