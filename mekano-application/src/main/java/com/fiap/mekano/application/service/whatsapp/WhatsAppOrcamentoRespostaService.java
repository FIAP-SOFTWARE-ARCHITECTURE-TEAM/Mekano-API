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
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(WhatsAppOrcamentoRespostaService.class);
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
        String resposta = normalizar(texto);
        String[] tokens = resposta.split("\\s+");
        String palavra = tokens.length == 0 ? "" : tokens[0];

        // WR-03: casa apenas a primeira palavra EXATA — "não entendi..." ou
        // "simples assim" NÃO acionam aprovação/reprovação de orçamento.
        if (!palavra.equals("sim") && !palavra.equals("s")
                && !palavra.equals("confirmar") && !palavra.equals("1")
                && !palavra.equals("nao") && !palavra.equals("não") && !palavra.equals("n")
                && !palavra.equals("recusar") && !palavra.equals("2")) {
            log.info("Resposta WhatsApp não reconhecida — ignorando");
            return false;
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByTelefone(normalizarTelefone(telefone));
        if (clienteOpt.isEmpty()) {
            log.info("Telefone WhatsApp sem cliente cadastrado — ignorando");
            return false;
        }
        Cliente cliente = clienteOpt.get();

        List<OrdemDeServico> osList = osRepository.findAllWithFilters(
                STATUS_AGUARDANDO_APROVACAO, cliente.getId(), null, null, null, 0, 1);

        if (osList.isEmpty()) {
            log.info("Cliente {} sem OS aguardando aprovação — ignorando", cliente.getId());
            return false;
        }

        OrdemDeServico os = osList.get(0);
        var orcamento = orcamentoRepository.findByOrdemServicoUuid(os.getId());
        if (orcamento.isEmpty()) {
            log.warn("Cliente {} com OS {} sem orçamento — ignorando", cliente.getId(), os.getId());
            return false;
        }

        UUID orcamentoUuid = orcamento.get().getId();
        boolean aprovado = palavra.equals("sim") || palavra.equals("s") || palavra.equals("confirmar") || palavra.equals("1");

        if (aprovado) {
            orcamentoService.aprovar(new AprovarOrcamentoCommand(orcamentoUuid));
            log.info("Orçamento {} aprovado via WhatsApp pelo cliente {}", orcamentoUuid, cliente.getId());
        } else {
            orcamentoService.reprovar(new ReprovarOrcamentoCommand(orcamentoUuid, "Reprovado via WhatsApp"));
            log.info("Orçamento {} reprovado via WhatsApp pelo cliente {}", orcamentoUuid, cliente.getId());
        }

        notifier.notificarRespostaOrcamento(cliente.getTelefone().getValue(), aprovado);
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
