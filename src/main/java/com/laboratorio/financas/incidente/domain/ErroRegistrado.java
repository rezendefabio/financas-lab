package com.laboratorio.financas.incidente.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutavel de um erro nao tratado. Cada instancia carrega um codigo
 * unico legivel ({@code ERR-XXXXXXXX}) que o usuario informa ao suporte.
 */
public final class ErroRegistrado {

    private static final int MAX_OPERACAO = 100;
    private static final int MAX_CLASSE_ERRO = 200;
    private static final int MAX_MENSAGEM = 500;
    private static final int MAX_STACK_TRACE = 4000;
    private static final String SEM_MENSAGEM = "(sem mensagem)";

    private final UUID id;
    private final String codigo;
    private final String operacao;
    private final String classeErro;
    private final String mensagem;
    private final String stackTrace;
    private final Instant criadoEm;

    public ErroRegistrado(String operacao, String classeErro, String mensagem, String stackTrace) {
        this.id = UUID.randomUUID();
        this.codigo = gerarCodigo(this.id);
        this.operacao = truncar(operacao, MAX_OPERACAO);
        this.classeErro = truncar(classeErro, MAX_CLASSE_ERRO);
        this.mensagem = mensagem != null ? truncar(mensagem, MAX_MENSAGEM) : SEM_MENSAGEM;
        this.stackTrace = truncar(stackTrace, MAX_STACK_TRACE);
        this.criadoEm = Instant.now();
    }

    public ErroRegistrado(
            UUID id,
            String codigo,
            String operacao,
            String classeErro,
            String mensagem,
            String stackTrace,
            Instant criadoEm
    ) {
        this.id = id;
        this.codigo = codigo;
        this.operacao = operacao;
        this.classeErro = classeErro;
        this.mensagem = mensagem;
        this.stackTrace = stackTrace;
        this.criadoEm = criadoEm;
    }

    private static String gerarCodigo(UUID id) {
        return "ERR-" + id.toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getClasseErro() {
        return classeErro;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
