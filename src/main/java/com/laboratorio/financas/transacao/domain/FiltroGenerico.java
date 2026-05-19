package com.laboratorio.financas.transacao.domain;

/**
 * Filtro generico campo-operador-valor aplicado dinamicamente na listagem de
 * transacoes.
 *
 * <p>Diferente dos filtros fixos de {@link FiltrosTransacao} (tipo, status,
 * conta...), estes filtros carregam um operador explicito -- ex.
 * {@code descricao contains "mercado"} ou {@code valor >= 100}. O campo e
 * sempre validado contra uma whitelist na camada de interface; o operador e
 * traduzido para um predicado de Criteria API na camada de infraestrutura.
 *
 * @param campo    nome de dominio do campo filtrado (ex: {@code descricao})
 * @param operador operador textual (ex: {@code contains}, {@code gte})
 * @param valor    valor cru do filtro como texto; vazio para operadores
 *                 booleanos
 */
public record FiltroGenerico(String campo, String operador, String valor) {
}
