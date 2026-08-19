package com.fiap.mekano.infrastructure.whatsapp;

import com.fiap.mekano.infrastructure.whatsapp.dto.SendMessageResponse;
import com.fiap.mekano.infrastructure.whatsapp.dto.SendTextRequest;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client (Quarkus) para a Evolution API self-hosted — canal WhatsApp.
 *
 * <p>Configuração em mekano-rest/src/main/resources/api-config.yml sob
 * {@code quarkus.rest-client.evolution-api}: base URL + timeouts.
 *
 * <p>Autenticação: {@code apikey} no header (AUTHENTICATION_API_KEY da instância).
 */
@RegisterRestClient(configKey = "evolution-api")
@Path("/message")
public interface EvolutionApiRestClient {

    /**
     * Envia mensagem de texto simples para um número.
     */
    @POST
    @Path("/sendText/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    SendMessageResponse sendText(@PathParam("instanceName") String instanceName,
                                 @HeaderParam("apikey") String apiKey,
                                 SendTextRequest request);
}