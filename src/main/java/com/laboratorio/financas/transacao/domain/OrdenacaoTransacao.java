package com.laboratorio.financas.transacao.domain;

import java.util.Arrays;

/**
 * Campos pelos quais uma listagem de transacoes pode ser ordenada.
 *
 * <p>Enum de dominio puro: carrega apenas o conceito semantico do campo
 * ordenavel. Nao conhece detalhes de persistencia (paths JPA, @Embedded).
 * A traducao para o caminho de propriedade real vive na camada de
 * infraestrutura.
 *
 * <p>Cada valor expoe um nome logico em lowercase, usado para parsing do
 * parametro {@code sort} da query string (formato {@code campo:direcao}).
 */
public enum OrdenacaoTransacao {

    DATA("data"),
    VALOR("valor"),
    DESCRICAO("descricao"),
    TIPO("tipo"),
    STATUS("status");

    /** Ordenacao default aplicada quando o cliente nao informa {@code sort}. */
    public static final OrdenacaoTransacao PADRAO = DATA;

    private final String nomeLogico;

    OrdenacaoTransacao(String nomeLogico) {
        this.nomeLogico = nomeLogico;
    }

    /**
     * Nome logico em lowercase aceito na query string. Nao e um path de
     * persistencia -- e o conceito de dominio.
     */
    public String nomeLogico() {
        return nomeLogico;
    }

    /**
     * Converte um nome logico (case-insensitive) no valor de enum correspondente.
     *
     * @param valor nome logico vindo da query string
     * @return o valor de enum correspondente
     * @throws IllegalArgumentException se o nome nao corresponder a nenhum campo
     */
    public static OrdenacaoTransacao fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Campo de ordenacao invalido: nome nao informado. "
                            + "Campos permitidos: " + nomesLogicos());
        }
        String alvo = valor.trim().toLowerCase();
        for (OrdenacaoTransacao ordenacao : values()) {
            if (ordenacao.nomeLogico.equals(alvo)) {
                return ordenacao;
            }
        }
        throw new IllegalArgumentException(
                "Campo de ordenacao invalido: '" + valor.trim()
                        + "'. Campos permitidos: " + nomesLogicos());
    }

    private static String nomesLogicos() {
        return Arrays.stream(values())
                .map(OrdenacaoTransacao::nomeLogico)
                .toList()
                .toString();
    }
}
