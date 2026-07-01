package com.fiap.mekano.rest.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Finalização do diagnóstico com itens para geração do orçamento")
public class FinalizarDiagnosticoRequest {

    @NotBlank
    @Schema(description = "Descrição do diagnóstico", example = "Troca de óleo e filtros")
    private String descricao;

    @NotEmpty
    @Valid
    @Schema(description = "Itens diagnosticados (peças e serviços já cadastrados)")
    private List<ItemDiagnosticado> itens;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public List<ItemDiagnosticado> getItens() { return itens; }
    public void setItens(List<ItemDiagnosticado> itens) { this.itens = itens; }

    @Schema(description = "Item do diagnóstico referenciando peça ou serviço existente")
    public static class ItemDiagnosticado {

        @NotNull
        @Schema(description = "UUID da peça ou serviço", example = "123e4567-e89b-12d3-a456-426614174000")
        private UUID referenciaUuid;

        @NotBlank
        @Schema(description = "Tipo: PECA ou SERVICO", example = "PECA")
        private String tipo;

        @NotNull
        @Positive
        @Schema(description = "Quantidade", example = "2")
        private Long quantidade;

        public UUID getReferenciaUuid() { return referenciaUuid; }
        public void setReferenciaUuid(UUID referenciaUuid) { this.referenciaUuid = referenciaUuid; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Long getQuantidade() { return quantidade; }
        public void setQuantidade(Long quantidade) { this.quantidade = quantidade; }
    }
}