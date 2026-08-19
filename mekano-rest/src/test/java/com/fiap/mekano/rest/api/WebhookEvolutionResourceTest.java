package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.whatsapp.WhatsAppOrcamentoRespostaService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebhookEvolutionResourceTest {

    private static final String BASE_PATH = "/api/v1/webhooks/evolution";
    private static final String TOKEN = "test-webhook-token";
    private static final String HEADER_TOKEN = "x-webhook-token";

    @InjectMock
    WhatsAppOrcamentoRespostaService respostaService;

    @Test
    @Order(1)
    @DisplayName("MESSAGES_UPSERT com 'sim' deve processar resposta")
    void eventoSim_deveProcessarResposta() {
        String payload = """
                {
                  "event": "MESSAGES_UPSERT",
                  "instance": "mekano",
                  "data": {
                    "key": { "remoteJid": "5591984847811@s.whatsapp.net", "fromMe": false },
                    "message": { "conversation": "sim" },
                    "messageType": "conversation"
                  }
                }""";

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(payload)
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService).processarResposta("5591984847811@s.whatsapp.net", "sim");
    }

    @Test
    @Order(2)
    @DisplayName("mensagem de própria autoria (fromMe=true) deve ser ignorada")
    void eventoFromMe_deveSerIgnorado() {
        String payload = """
                {
                  "event": "MESSAGES_UPSERT",
                  "instance": "mekano",
                  "data": {
                    "key": { "remoteJid": "5591984847811@s.whatsapp.net", "fromMe": true },
                    "message": { "conversation": "sim" }
                  }
                }""";

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(payload)
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
    }

    @Test
    @Order(3)
    @DisplayName("formato 'messages.upsert' da Evolution deve processar resposta")
    void eventoEvolution_deveProcessarResposta() {
        String payload = """
                {
                  "event": "messages.upsert",
                  "instance": "mekano",
                  "data": {
                    "key": { "remoteJid": "5591984847811@s.whatsapp.net", "fromMe": false },
                    "message": { "conversation": "sim" },
                    "messageType": "conversation"
                  }
                }""";

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(payload)
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService).processarResposta("5591984847811@s.whatsapp.net", "sim");
    }

    @Test
    @Order(4)
    @DisplayName("evento de outro tipo deve ser ignorado")
    void eventoDiferente_deveSerIgnorado() {
        String payload = """
                {
                  "event": "CONNECTION_UPDATE",
                  "instance": "mekano",
                  "data": { "state": "open" }
                }""";

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(payload)
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
    }

    @Test
    @Order(5)
    @DisplayName("sem token deve retornar 401 e não processar (fail closed)")
    void semToken_deveRetornar401() {
        String payload = """
                {
                  "event": "MESSAGES_UPSERT",
                  "instance": "mekano",
                  "data": {
                    "key": { "remoteJid": "5591984847811@s.whatsapp.net", "fromMe": false },
                    "message": { "conversation": "sim" }
                  }
                }""";

        given()
                .contentType("application/json")
                .body(payload)
                .when().post(BASE_PATH)
                .then().statusCode(401);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
    }

    @Test
    @Order(6)
    @DisplayName("token incorreto deve retornar 401 e não processar")
    void tokenIncorreto_deveRetornar401() {
        String payload = """
                {
                  "event": "MESSAGES_UPSERT",
                  "instance": "mekano",
                  "data": {
                    "key": { "remoteJid": "5591984847811@s.whatsapp.net", "fromMe": false },
                    "message": { "conversation": "sim" }
                  }
                }""";

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, "token-errado")
                .body(payload)
                .when().post(BASE_PATH)
                .then().statusCode(401);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
    }
}