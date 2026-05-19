package com.laboratorio.financas.transacao.domain;

/**
 * Direcao de ordenacao de uma listagem de transacoes.
 *
 * <p>Enum de dominio puro: carrega apenas o conceito semantico de direcao
 * ({@code ASC}/{@code DESC}). Nao conhece nenhum tipo de framework de
 * persistencia. A traducao para o tipo de direcao do Spring Data
 * ({@code org.springframework.data.domain.Sort.Direction}) vive na camada
 * de infraestrutura.
 */
public enum DirecaoOrdenacao {

    ASC,
    DESC
}
