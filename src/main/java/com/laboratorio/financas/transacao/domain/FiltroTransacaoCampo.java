package com.laboratorio.financas.transacao.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Set;

/**
 * Campos de dominio aceitos nos filtros adicionais ({@link FiltroGenerico}) da
 * listagem de transacoes, com o tipo de cada campo e os operadores validos.
 *
 * <p>Funciona como whitelist: campos fora deste enum nao podem ser filtrados,
 * o que evita filtragem por campo arbitrario. As regras de quais operadores
 * sao validos para cada tipo de campo sao regra de dominio e vivem aqui.
 */
public enum FiltroTransacaoCampo {

    DESCRICAO("descricao", Tipo.STRING),
    VALOR("valor", Tipo.NUMBER),
    DATA("data", Tipo.DATE);

    /** Tipo logico de um campo filtravel. */
    public enum Tipo { STRING, NUMBER, DATE }

    private static final Set<String> OPERADORES_STRING =
            Set.of("contains", "not_contains", "eq", "neq");
    private static final Set<String> OPERADORES_COMPARACAO =
            Set.of("eq", "neq", "gt", "gte", "lt", "lte");

    private final String nome;
    private final Tipo tipo;

    FiltroTransacaoCampo(String nome, Tipo tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    public String nome() {
        return nome;
    }

    public Tipo tipo() {
        return tipo;
    }

    /**
     * Resolve o campo a partir do nome de dominio.
     *
     * @throws IllegalArgumentException se o nome nao corresponder a um campo
     *         filtravel (campo fora da whitelist)
     */
    public static FiltroTransacaoCampo fromNome(String nome) {
        return Arrays.stream(values())
                .filter(c -> c.nome.equals(nome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campo de filtro nao permitido: '" + nome + "'. Campos validos: "
                                + Arrays.stream(values()).map(c -> c.nome).toList() + "."));
    }

    /** Operadores validos para o tipo deste campo. */
    public Set<String> operadoresValidos() {
        return (tipo == Tipo.STRING) ? OPERADORES_STRING : OPERADORES_COMPARACAO;
    }

    /**
     * Valida um {@link FiltroGenerico} de forma ansiosa: campo na whitelist,
     * operador compativel com o tipo do campo e valor parseavel para o tipo.
     *
     * <p>Validacao ansiosa garante HTTP 400 em vez de 500 -- o erro e detectado
     * antes da consulta, nao em tempo de execucao da query.
     *
     * @throws IllegalArgumentException para qualquer violacao
     */
    public static void validar(FiltroGenerico fg) {
        FiltroTransacaoCampo campo = fromNome(fg.campo());
        if (!campo.operadoresValidos().contains(fg.operador())) {
            throw new IllegalArgumentException(
                    "Operador '" + fg.operador() + "' invalido para o campo '"
                            + campo.nome + "' (tipo " + campo.tipo + ").");
        }
        switch (campo.tipo) {
            case NUMBER -> parseNumero(fg.valor());
            case DATE -> parseData(fg.valor());
            case STRING -> { /* string nao exige parse */ }
            default -> throw new IllegalArgumentException("Tipo de campo nao suportado");
        }
    }

    private static void parseNumero(String valor) {
        try {
            new BigDecimal(valor.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Valor numerico invalido para filtro: '" + valor + "'.");
        }
    }

    private static void parseData(String valor) {
        try {
            LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Valor de data invalido para filtro: '" + valor + "'.");
        }
    }
}
