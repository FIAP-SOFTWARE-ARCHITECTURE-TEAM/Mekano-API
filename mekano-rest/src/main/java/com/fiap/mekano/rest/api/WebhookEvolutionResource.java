package com.fiap.mekano.rest.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.mekano.application.service.whatsapp.WhatsAppOrcamentoRespostaService;
import io.quarkus.logging.Log;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Optional;

/**
 * Webhook de entrada da Evolution API (WPP-01).
 *
 * <p>Recebe eventos {@code MESSAGES_UPSERT} e processa respostas SIM/NÃO
 * do cliente ao orçamento via {@link WhatsAppOrcamentoRespostaService}.
 *
 * <p>{@code @PermitAll} (G8): a Evolution API não envia JWT — a autenticação
 * é feita pelo header {@code x-webhook-token} quando configurado.
 *
 * <p>Retorna 200 sempre (mesmo ignorando o evento) para não gerar retries.
 */
@Path("/webhooks/evolution")
@RequestScoped
@PermitAll
@Tag(name = "Webhook Evolution", description = "Recepção de eventos da Evolution API")
public class WebhookEvolutionResource {

    private static final String EVENT_MESSAGES_UPSERT = "MESSAGES_UPSERT";
    private static final String EVENT_MESSAGES_UPSERT_EVOLUTION = "messages.upsert";

    @Inject
    WhatsAppOrcamentoRespostaService respostaService;

    @ConfigProperty(name = "evolution.webhook-token")
    Optional<String> webhookToken;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Recebe eventos da Evolution API",
            description = "Processa respostas SIM/NÃO de clientes a orçamentos enviados via WhatsApp")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(example = """
                    {
                      "event": "MESSAGES_UPSERT",
                      "instance": "mekano",
                      "data": {
                        "key": { "remoteJid": "5591984847811@s.whatsapp.net", "fromMe": false },
                        "message": { "conversation": "sim" },
                        "messageType": "conversation"
                      }
                    }""")))
    @APIResponse(responseCode = "200", description = "Evento recebido (200 mesmo quando ignorado)")
    @APIResponse(responseCode = "401", description = "Token inválido")
    public Response receberEvento(JsonNode payload,
                                  @HeaderParam("x-webhook-token") String token) {
        if (webhookToken.isPresent() && !webhookToken.get().isBlank()
                && !webhookToken.get().equals(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            if (!isRespostaOrcamento(payload)) {
                return Response.ok().build();
            }

            Log.debug("Webhook Evolution de mensagem recebido");

            String remoteJid = payload.path("data").path("key").path("remoteJid").asText();
            String texto = extrairTexto(payload.path("data").path("message"));

            respostaService.processarResposta(remoteJid, texto);
        } catch (Exception ex) {
            Log.warnf("Falha ao processar webhook Evolution: %s", ex.getMessage());
        }
        return Response.ok().build();
    }

    /**
     * Aceita apenas mensagens de texto recebidas (fromMe=false) — nunca ecoa o próprio envio.
     */
    private boolean isRespostaOrcamento(JsonNode payload) {
        if (payload == null) {
            return false;
        }

        String event = payload.path("event").asText();
        if (!EVENT_MESSAGES_UPSERT.equals(event) && !EVENT_MESSAGES_UPSERT_EVOLUTION.equals(event)) {
            return false;
        }

        JsonNode data = payload.path("data");
        if (data.path("key").path("fromMe").asBoolean(false)) {
            return false;
        }

        return extrairTexto(data.path("message")) != null;
    }

    /**
     * Texto simples chega em {@code message.conversation}; com link/contexto,
     * em {@code message.extendedTextMessage.text} (Baileys).
     */
    private String extrairTexto(JsonNode message) {
        if (message == null || message.isMissingNode() || !message.isObject()) {
            return null;
        }
        String conversation = message.path("conversation").asText(null);
        if (conversation != null) {
            return conversation;
        }
        return message.path("extendedTextMessage").path("text").asText(null);
    }
}