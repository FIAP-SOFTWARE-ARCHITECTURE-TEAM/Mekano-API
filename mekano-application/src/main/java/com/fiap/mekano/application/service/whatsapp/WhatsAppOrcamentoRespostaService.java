package com.fiap.mekano.application.service.whatsapp;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.in.AprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.in.OrcamentoServicePort;
import com.fiap.mekano.domain.port.in.ReprovarOrcamentoCommand;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processa a resposta do cliente (CONFIRMAR/RECUSAR) recebida via webhook da Evolution API.
 *
 * <p>Fluxo: telefone → cliente → OS mais recente em AGUARDANDO_APROVACAO →
 * orçamento pendente → aprovação/reprovação via {@link OrcamentoServicePort} →
 * confirmação de volta ao cliente via {@link WhatsAppNotifierPort}.
 *
 * <p><b>Sem {@code @Transactional}</b>: as operações de escrita delegam para
 * {@link OrcamentoServicePort} (transacional); a chamada HTTP externa de
 * confirmação não pode segurar conexão de banco (D-11).
 *
 * <p>PII (V8): telefone nunca logado completo — apenas o UUID do cliente.
 */
@ApplicationScoped
public class WhatsAppOrcamentoRespostaService {

    private static final String STATUS_AGUARDANDO_APROVACAO = "AGUARDANDO_APROVACAO";

    private final ClienteRepositoryPort clienteRepository;
    private final OrdemDeServicoRepositoryPort osRepository;
    private final OrcamentoRepositoryPort orcamentoRepository;
    private final OrcamentoServicePort orcamentoService;
    private final WhatsAppNotifierPort notifier;

    public WhatsAppOrcamentoRespostaService(ClienteRepositoryPort clienteRepository,
                                            OrdemDeServicoRepositoryPort osRepository,
                                            OrcamentoRepositoryPort orcamentoRepository,
                                            OrcamentoServicePort orcamentoService,
                                            WhatsAppNotifierPort notifier) {
        this.clienteRepository = clienteRepository;
        this.osRepository = osRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.orcamentoService = orcamentoService;
        this.notifier = notifier;
    }

    /**
     * Processa a resposta recebida. Retorna {@code true} se alguma ação foi tomada.
     *
     * @param telefone remoteJid normalizado (dígitos, DDI opcional)
     * @param texto    texto bruto da mensagem recebida
     */
    public boolean processarResposta(String telefone, String texto) {
        Log.info("Processando resposta WhatsApp de orçamento");
        String resposta = normalizar(texto);
        String[] tokens = resposta.split("\\s+");
        String palavra = tokens.length == 0 ? "" : tokens[0];

        // WR-03: casa apenas a primeira palavra EXATA — "não entendi..." ou
        // "simples assim" NÃO acionam aprovação/reprovação de orçamento.
        if (!palavra.equals("sim") && !palavra.equals("s")
                && !palavra.equals("confirmar") && !palavra.equals("1")
                && !palavra.equals("nao") && !palavra.equals("não") && !palavra.equals("n")
                && !palavra.equals("recusar") && !palavra.equals("2")) {
            Log.info("Resposta WhatsApp não reconhecida — ignorando");
            return false;
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByTelefone(normalizarTelefone(telefone));
        if (clienteOpt.isEmpty()) {
            Log.info("Telefone WhatsApp sem cliente cadastrado — ignorando");
            return false;
        }
        Cliente cliente = clienteOpt.get();

        List<OrdemDeServico> osList = osRepository.findAllWithFilters(
                STATUS_AGUARDANDO_APROVACAO, cliente.getId(), null, null, null, 0, 1);

        if (osList.isEmpty()) {
            Log.infof("Cliente %s sem OS aguardando aprovação — ignorando", cliente.getId());
            return false;
        }

        OrdemDeServico os = osList.get(0);
        var orcamento = orcamentoRepository.findByOrdemServicoUuid(os.getId());
        if (orcamento.isEmpty()) {
            Log.warnf("Cliente %s com OS %s sem orçamento — ignorando", cliente.getId(), os.getId());
            return false;
        }

        UUID orcamentoUuid = orcamento.get().getId();
        boolean aprovado = palavra.equals("sim") || palavra.equals("s") || palavra.equals("confirmar") || palavra.equals("1");

        if (aprovado) {
            notifier.notificarRespostaOrcamento(cliente.getTelefone().getValue(), true);
            orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamentoUuid));
            Log.infof("Orçamento %s aprovado via WhatsApp pelo cliente %s", orcamentoUuid, cliente.getId());
        } else {
            notifier.notificarRespostaOrcamento(cliente.getTelefone().getValue(), false);
            orcamentoService.reprovar(new ReprovarOrcamentoCommand(orcamentoUuid, "Reprovado via WhatsApp"));
            Log.infof("Orçamento %s reprovado via WhatsApp pelo cliente %s", orcamentoUuid, cliente.getId());
        }

        return true;
    }

    /**
     * Remove DDI 55 quando presente: "5591984847811" → "91984847811".
     */
    private String normalizarTelefone(String telefone) {
        String digits = telefone.replaceAll("\\D", "");
        return digits.startsWith("55") && digits.length() > 11 ? digits.substring(2) : digits;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase().trim();
    }
}
