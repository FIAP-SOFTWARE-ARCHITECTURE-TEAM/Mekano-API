package com.fiap.mekano.domain.port.in;

import java.math.BigDecimal;

/**
 * Comando de entrada para criação de um serviço.
 *
 * Localizado no domínio (não em application) para evitar dependência cíclica.
 * Validação de constraints é responsabilidade do adapter (Bean Validation).
 */
public record CreateServicoCommand(String nome, String descricao, BigDecimal valor) {}
