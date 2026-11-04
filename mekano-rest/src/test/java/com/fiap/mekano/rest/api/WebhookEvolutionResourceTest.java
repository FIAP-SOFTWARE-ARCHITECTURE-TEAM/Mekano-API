package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.whatsapp.WhatsAppOrcamentoRespostaService;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebhookEvolutionResourceTest {

    private static final String BASE_PATH = "/api/v1/webhooks/evolution";
    private static final String TOKEN = "test-webhook-token";
    private static final String HEADER_TOKEN = "x-webhook-token";
    private static final String METRIC_WEBHOOK = "mekano.evolution.webhook";

    @InjectMock
    WhatsAppOrcamentoRespostaService respostaService;

    @Inject
    MeterRegistry meterRegistry;

    @Test
    @Order(1)
    @DisplayName("MESSAGES_UPSERT com 'sim' deve processar resposta e contar processed")
    void eventoSim_deveProcessarResposta() {
        String remoteJid = "5591984847811@s.whatsapp.net";
        when(respostaService.processarResposta(remoteJid, "sim")).thenReturn(true);

        double processedAntes = metricCount("processed");
        double ignoredAntes = metricCount("ignored");
        double errorAntes = metricCount("error");
        double rejectedAntes = metricCount("rejected");

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(mensagemPayload("MESSAGES_UPSERT", false, "sim"))
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService).processarResposta(remoteJid, "sim");
        assertOnlyResultIncremented(
                "processed",
                processedAntes,
                ignoredAntes,
                errorAntes,
                rejectedAntes
        );
    }

    @Test
    @Order(2)
    @DisplayName("mensagem de própria autoria (fromMe=true) deve ser ignorada e contar ignored")
    void eventoFromMe_deveSerIgnorado() {
        double processedAntes = metricCount("processed");
        double ignoredAntes = metricCount("ignored");
        double errorAntes = metricCount("error");
        double rejectedAntes = metricCount("rejected");

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(mensagemPayload("MESSAGES_UPSERT", true, "sim"))
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
        assertOnlyResultIncremented(
                "ignored",
                processedAntes,
                ignoredAntes,
                errorAntes,
                rejectedAntes
        );
    }

    @Test
    @Order(3)
    @DisplayName("formato 'messages.upsert' da Evolution deve processar resposta")
    void eventoEvolution_deveProcessarResposta() {
        String remoteJid = "5591984847811@s.whatsapp.net";
        when(respostaService.processarResposta(remoteJid, "sim")).thenReturn(true);

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(mensagemPayload("messages.upsert", false, "sim"))
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService).processarResposta(remoteJid, "sim");
    }

    @Test
    @Order(4)
    @DisplayName("evento de outro tipo deve ser ignorado")
    void eventoDiferente_deveSerIgnorado() {
        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body("""
                        {
                          "event": "CONNECTION_UPDATE",
                          "instance": "mekano",
                          "data": { "state": "open" }
                        }""")
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
    }

    @Test
    @Order(5)
    @DisplayName("sem token deve retornar 401, não processar e contar rejected")
    void semToken_deveRetornar401() {
        double processedAntes = metricCount("processed");
        double ignoredAntes = metricCount("ignored");
        double errorAntes = metricCount("error");
        double rejectedAntes = metricCount("rejected");

        given()
                .contentType("application/json")
                .body(mensagemPayload("MESSAGES_UPSERT", false, "sim"))
                .when().post(BASE_PATH)
                .then().statusCode(401);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
        assertOnlyResultIncremented(
                "rejected",
                processedAntes,
                ignoredAntes,
                errorAntes,
                rejectedAntes
        );
    }

    @Test
    @Order(6)
    @DisplayName("token incorreto deve retornar 401 e não processar")
    void tokenIncorreto_deveRetornar401() {
        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, "token-errado")
                .body(mensagemPayload("MESSAGES_UPSERT", false, "sim"))
                .when().post(BASE_PATH)
                .then().statusCode(401);

        verify(respostaService, never()).processarResposta(anyString(), anyString());
    }

    @Test
    @Order(7)
    @DisplayName("resposta não processada pelo serviço deve contar ignored")
    void respostaNaoProcessada_deveContarIgnored() {
        String remoteJid = "5591984847811@s.whatsapp.net";
        when(respostaService.processarResposta(remoteJid, "talvez")).thenReturn(false);

        double processedAntes = metricCount("processed");
        double ignoredAntes = metricCount("ignored");
        double errorAntes = metricCount("error");
        double rejectedAntes = metricCount("rejected");

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(mensagemPayload("MESSAGES_UPSERT", false, "talvez"))
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService).processarResposta(remoteJid, "talvez");
        assertOnlyResultIncremented(
                "ignored",
                processedAntes,
                ignoredAntes,
                errorAntes,
                rejectedAntes
        );
    }

    @Test
    @Order(8)
    @DisplayName("erro no processamento deve retornar 200 e contar error")
    void erroProcessamento_deveContarError() {
        String remoteJid = "5591984847811@s.whatsapp.net";
        when(respostaService.processarResposta(remoteJid, "sim"))
                .thenThrow(new RuntimeException("falha simulada"));

        double processedAntes = metricCount("processed");
        double ignoredAntes = metricCount("ignored");
        double errorAntes = metricCount("error");
        double rejectedAntes = metricCount("rejected");

        given()
                .contentType("application/json")
                .header(HEADER_TOKEN, TOKEN)
                .body(mensagemPayload("MESSAGES_UPSERT", false, "sim"))
                .when().post(BASE_PATH)
                .then().statusCode(200);

        verify(respostaService).processarResposta(remoteJid, "sim");
        assertOnlyResultIncremented(
                "error",
                processedAntes,
                ignoredAntes,
                errorAntes,
                rejectedAntes
        );
    }

    private double metricCount(String result) {
        return meterRegistry.counter(METRIC_WEBHOOK, "result", result).count();
    }

    private void assertOnlyResultIncremented(
            String expectedResult,
            double processedAntes,
            double ignoredAntes,
            double errorAntes,
            double rejectedAntes
    ) {
        assertEquals(
                processedAntes + ("processed".equals(expectedResult) ? 1.0 : 0.0),
                metricCount("processed")
        );
        assertEquals(
                ignoredAntes + ("ignored".equals(expectedResult) ? 1.0 : 0.0),
                metricCount("ignored")
        );
        assertEquals(
                errorAntes + ("error".equals(expectedResult) ? 1.0 : 0.0),
                metricCount("error")
        );
        assertEquals(
                rejectedAntes + ("rejected".equals(expectedResult) ? 1.0 : 0.0),
                metricCount("rejected")
        );
    }

    private String mensagemPayload(String event, boolean fromMe, String texto) {
        return """
                {
                  "event": "%s",
                  "instance": "mekano",
                  "data": {
                    "key": {
                      "remoteJid": "5591984847811@s.whatsapp.net",
                      "fromMe": %s
                    },
                    "message": {
                      "conversation": "%s"
                    },
                    "messageType": "conversation"
                  }
                }
                """.formatted(event, fromMe, texto);
    }
}
