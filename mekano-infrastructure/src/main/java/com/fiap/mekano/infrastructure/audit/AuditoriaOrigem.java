package com.fiap.mekano.infrastructure.audit;

import java.util.UUID;

/**
 * Códigos reservados de origem para os campos de auditoria {@code createdBy}/{@code updatedBy}.
 *
 * <p>Quando o registro é criado/atualizado por um usuário autenticado, o UUID do sujeito do
 * JWT é gravado diretamente. Quando não há usuário autenticado em uma requisição, usa-se
 * {@link #PUBLICO}; quando a operação é executada por rotina interna do sistema (jobs,
 * tarefas agendadas, queries nativas), usa-se {@link #SISTEMA}.
 */
public enum AuditoriaOrigem {

    /** Acesso público — requisição sem usuário autenticado. */
    PUBLICO(UUID.fromString("00000000-0000-0000-0000-000000000001")),

    /** Rotina interna do sistema — sem contexto de requisição HTTP. */
    SISTEMA(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    private final UUID codigo;

    AuditoriaOrigem(UUID codigo) {
        this.codigo = codigo;
    }

    public UUID getCodigo() {
        return codigo;
    }

    /**
     * Resolve o ator a partir do nome do principal (subject do JWT).
     *
     * @param principalName subject do token ou {@code null}/vazio quando não autenticado
     * @return o {@link UUID} do usuário quando o subject é um UUID válido; caso contrário {@link #PUBLICO}
     */
    public static UUID resolver(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return PUBLICO.codigo;
        }
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            return PUBLICO.codigo;
        }
    }
}