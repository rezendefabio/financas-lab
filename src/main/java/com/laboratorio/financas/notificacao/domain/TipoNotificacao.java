package com.laboratorio.financas.notificacao.domain;

/**
 * Tipos de notificacao derivados do estado de orcamentos e metas.
 *
 * <ul>
 *   <li>{@code ORCAMENTO_ATENCAO}  -- orcamento com status ATENCAO (&gt;= 80% do limite).</li>
 *   <li>{@code ORCAMENTO_EXCEDIDO} -- orcamento com status EXCEDIDO (&gt; 100%).</li>
 *   <li>{@code META_VENCENDO}      -- meta EM_ANDAMENTO com prazo em &lt;= 7 dias.</li>
 *   <li>{@code META_VENCIDA}       -- meta EM_ANDAMENTO com prazo &lt; hoje.</li>
 * </ul>
 */
public enum TipoNotificacao {
    ORCAMENTO_ATENCAO,
    ORCAMENTO_EXCEDIDO,
    META_VENCENDO,
    META_VENCIDA
}
