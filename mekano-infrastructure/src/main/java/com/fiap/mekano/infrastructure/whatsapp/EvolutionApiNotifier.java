package com.fiap.mekano.infrastructure.whatsapp;

import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import com.fiap.mekano.infrastructure.whatsapp.dto.SendTextRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementação do {@link WhatsAppNotifierPort} via Evolution API.
 *
 * <p><b>Sem {@code @Transactional}</b> (D-11): chamada HTTP externa não deve
 * segurar conexão de banco — side effect fora da transação do fluxo OS.
 *
 * <p>Fault tolerance (T-05-03): {@link Retry} + {@link Fallback} — se a
 * Evolution API estiver fora, loga warning e segue; transação principal não é afetada.
 *
 * <p>PII (V8 / T-05-02): telefone NUNCA logado completo — {@link #maskPhone}.
 */
@ApplicationScoped
public class EvolutionApiNotifier implements WhatsAppNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiNotifier.class);
    private static final String COUNTRY_CODE = "55";

    private final EvolutionApiRestClient restClient;
    private final String apiKey;
    private final String instanceName;

    @Inject
    public EvolutionApiNotifier(@RestClient EvolutionApiRestClient restClient,
                                @ConfigProperty(name = "evolution.api-key", defaultValue = "dev-key") String apiKey,
                                @ConfigProperty(name = "evolution.instance-name", defaultValue = "mekano") String instanceName) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.instanceName = instanceName;
    }

    @Override
    @Retry(maxRetries = 2, delay = 1000)
    @Fallback(fallbackMethod = "logFailureOrcamento")
    public void notificarOrcamento(String telefone, String nomeCliente, String marca, String modelo,
                                   String placa, BigDecimal valorTotal) {
        String number = formatPhone(telefone);
        String veiculo = (marca == null || marca.isBlank() ? "" : marca + " ")
                + (modelo == null || modelo.isBlank() ? "" : modelo + " ")
                + "(" + placa + ")";
        var request = new SendTextRequest(number,
                "Olá " + nomeCliente + ", seu orçamento para o veículo " + veiculo.strip()
                        + " ficou em R$ " + valorTotal + ". Deseja aprovar? Responda SIM ou NÃO.");
        log.info("Enviando notificação de orçamento para {} (instance={})",
                maskPhone(number), instanceName);
        restClient.sendText(instanceName, apiKey, request);
    }

    @Override
    @Retry(maxRetries = 2, delay = 1000)
    @Fallback(fallbackMethod = "logFailureRespostaOrcamento")
    public void notificarRespostaOrcamento(String telefone, String nomeCliente, boolean aprovado) {
        String number = formatPhone(telefone);
        String mensagem = aprovado
                ? "Orçamento aprovado! Sua ordem de serviço será iniciada em breve."
                : "Orçamento não aprovado. Se precisar de ajustes, fale conosco.";
        var request = new SendTextRequest(number, "Olá " + nomeCliente + ", " + mensagem);
        log.info("Enviando confirmação de {} de orçamento para {} (instance={})",
                aprovado ? "aprovação" : "reprovação", maskPhone(number), instanceName);
        restClient.sendText(instanceName, apiKey, request);
    }

    @Override
    @Retry(maxRetries = 2, delay = 1000)
    @Fallback(fallbackMethod = "logFailureRetirada")
    public void notificarRetirada(String telefone, String nomeCliente, String placa, UUID osUuid) {
        String number = formatPhone(telefone);
        var request = new SendTextRequest(number,
                "🚗 Olá " + nomeCliente + "! Seu veículo " + placa
                        + " já está pronto para retirada na Oficina Mekano.\n\n"
                        + "📍 Rua das Oficinas, 100 - Centro\n"
                        + "📅 Seg-Sex: 08h-18h\n\n"
                        + "Acompanhe o status: https://mekano.app/os/" + osUuid + "/status");
        log.info("Enviando notificação de retirada para {} (OS {})",
                maskPhone(number), osUuid);
        restClient.sendText(instanceName, apiKey, request);
    }

    /**
     * Formata telefone para E.164: dígitos (ex.: 11999999999) → 55 + número.
     * Se já vier com 55, não duplica o prefixo.
     */
    private String formatPhone(String telefone) {
        String digits = telefone.replaceAll("\\D", "");
        return digits.startsWith(COUNTRY_CODE) ? digits : COUNTRY_CODE + digits;
    }

    /**
     * Mascara número em logs: primeiros 4 + "****" + últimos 2 (PII — V8).
     */
    private String maskPhone(String telefone) {
        if (telefone == null || telefone.length() < 6) {
            return "****";
        }
        return telefone.substring(0, 4) + "****" + telefone.substring(telefone.length() - 2);
    }

    private void logFailureOrcamento(String telefone, String nomeCliente, String marca, String modelo,
                                     String placa, BigDecimal valorTotal, Throwable ex) {
        log.warn("Falha ao notificar orçamento para {} — Evolution API indisponível: {}",
                maskPhone(telefone), ex.getMessage());
    }

    private void logFailureRespostaOrcamento(String telefone, String nomeCliente, boolean aprovado,
                                             Throwable ex) {
        log.warn("Falha ao notificar confirmação de orçamento para {} — Evolution API indisponível: {}",
                maskPhone(telefone), ex.getMessage());
    }

    private void logFailureRetirada(String telefone, String nomeCliente, String placa,
                                    UUID osUuid, Throwable ex) {
        log.warn("Falha ao notificar retirada OS {} — Evolution API indisponível: {}",
                osUuid, ex.getMessage());
    }
}