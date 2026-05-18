package com.laboratorio.financas.transacao.domain;

import java.util.UUID;

/**
 * Evento de dominio publicado quando uma {@link Transacao} e criada.
 *
 * <p>Pertence ao bounded context {@code transacao}: e publicado por ele e
 * consumido por outros contextos (ex.: {@code orcamento}) de forma desacoplada
 * via {@code ApplicationEventPublisher} + {@code @EventListener}.
 */
public record TransacaoCriadaEvent(
        UUID transacaoId,
        UUID categoriaId,
        UUID contaId,
        UUID usuarioId,
        java.math.BigDecimal valor,
        java.time.LocalDate data,
        String tipo  // "DESPESA", "RECEITA", "TRANSFERENCIA"
) { }
