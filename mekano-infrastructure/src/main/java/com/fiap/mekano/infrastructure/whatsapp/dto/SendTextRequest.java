package com.fiap.mekano.infrastructure.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload do endpoint Evolution API {@code /message/sendText/{instance}}.
 * linkPreview é opcional — padrão false: envia como {@code conversation}
 * (texto simples), que renderiza quebras de linha corretamente no WhatsApp.
 * Com {@code link_preview: true} a Evolution envia {@code extendedTextMessage}
 * e o cliente não renderiza {@code \n}.
 */
public record SendTextRequest(String number, String text, @JsonProperty("link_preview") Boolean linkPreview) {

    public SendTextRequest {
        linkPreview = linkPreview == null ? Boolean.FALSE : linkPreview;
    }

    public SendTextRequest(String number, String text) {
        this(number, text, Boolean.FALSE);
    }
}