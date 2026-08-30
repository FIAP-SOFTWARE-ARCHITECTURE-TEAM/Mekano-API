package com.fiap.mekano.rest.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fiap.mekano.application.service.whatsapp.WhatsAppOrcamentoRespostaService;
import io.quarkus.logging.Log;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Webhook de entrada da Evolution API (WPP-01).
 *
 * <p>Recebe eventos {@code MESSAGES_UPSERT} e processa respostas CONFIRMAR/RECUSAR
 * do cliente ao orçamento via {@link WhatsAppOrcamentoRespostaService}.
 *
 * <p>{@code @PermitAll} (G8): a Evolution API não envia JWT — a autenticação
 * é feita pelo header {@code x-webhook-token} (CR-02).
 *
 * <p><b>Fail closed (CR-02)</b>: sem token configurado ({@code evolution.webhook-token})
 * ou com token ausente/vazio/incorreto, retorna 401 — o evento nunca é processado
 * anonimamente.
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
    private static final String HEADER_WEBHOOK_TOKEN = "x-webhook-token";

    private final WhatsAppOrcamentoRespostaService respostaService;
    private final Optional<String> webhookToken;

    public WebhookEvolutionResource(WhatsAppOrcamentoRespostaService respostaService,
                                    @ConfigProperty(name = "evolution.webhook-token") Optional<String> webhookToken) {
        this.respostaService = respostaService;
        this.webhookToken = webhookToken;
    }

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
                                   @HeaderParam(HEADER_WEBHOOK_TOKEN) String token) {
        if (!tokenValido(token, payload)) {
            Log.warnf("Webhook Evolution rejeitado: token ausente ou inválido");
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
     * Fail closed (CR-02): token obrigatório — 401 quando ausente, vazio ou
     * inválido. Comparação em tempo constante via {@link MessageDigest#isEqual}
     * para evitar timing side channel (IN-08).
     *
     * <p>Aceita duas formas de autenticação:
     * <ol>
     *   <li>Header {@code x-webhook-token} — usado quando a Evolution API tem
     *       webhook configurado por instância com headers customizados.</li>
     *   <li>Campo {@code apikey} do corpo JSON — a Evolution API envia
     *       automaticamente sua {@code AUTHENTICATION_API_KEY} no payload
     *       do webhook global.</li>
     * </ol>
     */
    private boolean tokenValido(String headerToken, JsonNode payload) {
        if (webhookToken.isEmpty() || webhookToken.get().isBlank()) {
            return false;
        }
        byte[] expected = webhookToken.get().getBytes(StandardCharsets.UTF_8);

        if (headerToken != null && MessageDigest.isEqual(expected, headerToken.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }

        String bodyApikey = payload != null ? payload.path("apikey").asText(null) : null;
        return bodyApikey != null && MessageDigest.isEqual(expected, bodyApikey.getBytes(StandardCharsets.UTF_8));
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
