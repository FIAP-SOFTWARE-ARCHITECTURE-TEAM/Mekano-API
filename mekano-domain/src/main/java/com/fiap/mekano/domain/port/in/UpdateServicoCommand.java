package com.fiap.mekano.domain.port.in;

import java.math.BigDecimal;

/**
 * Comando de entrada para atualização de um serviço.
 *
 * Localizado no domínio (não em application) para evitar dependência cíclica.
 */
public record UpdateServicoCommand(String nome, String descricao, BigDecimal valor) {}
