package com.laboratorio.financas.incidente.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ErroRegistradoTest {

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosPreencheTodosOsCampos() {
        // Given
        Instant antes = Instant.now();

        // When
        ErroRegistrado erro = new ErroRegistrado(
                "POST /api/transacoes", "NullPointerException", "valor nulo", "stack...");

        // Then
        Instant depois = Instant.now();
        assertThat(erro.getId()).isNotNull();
        assertThat(erro.getOperacao()).isEqualTo("POST /api/transacoes");
        assertThat(erro.getClasseErro()).isEqualTo("NullPointerException");
        assertThat(erro.getMensagem()).isEqualTo("valor nulo");
        assertThat(erro.getStackTrace()).isEqualTo("stack...");
        assertThat(erro.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorNovoGeraCodigoNoFormatoEsperado() {
        ErroRegistrado erro = new ErroRegistrado("op", "Classe", "msg", "stack");

        assertThat(erro.getCodigo()).matches("ERR-[0-9A-F]{8}");
        assertThat(erro.getCodigo()).hasSize(12);
    }

    @Test
    void construtorNovoDerivaCodigoDosPrimeirosOitoCaracteresDoId() {
        ErroRegistrado erro = new ErroRegistrado("op", "Classe", "msg", "stack");

        String esperado = "ERR-" + erro.getId().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        assertThat(erro.getCodigo()).isEqualTo(esperado);
    }

    @Test
    void construtorNovoComDoisErrosGeraIdsECodigosDiferentes() {
        ErroRegistrado e1 = new ErroRegistrado("op", "Classe", "msg", "stack");
        ErroRegistrado e2 = new ErroRegistrado("op", "Classe", "msg", "stack");

        assertThat(e1.getId()).isNotEqualTo(e2.getId());
        assertThat(e1.getCodigo()).isNotEqualTo(e2.getCodigo());
    }

    @Test
    void construtorNovoComMensagemNulaUsaTextoPadrao() {
        ErroRegistrado erro = new ErroRegistrado("op", "Classe", null, "stack");

        assertThat(erro.getMensagem()).isEqualTo("(sem mensagem)");
    }

    @Test
    void construtorNovoTruncaMensagemEm500Caracteres() {
        String mensagemLonga = "x".repeat(600);

        ErroRegistrado erro = new ErroRegistrado("op", "Classe", mensagemLonga, "stack");

        assertThat(erro.getMensagem()).hasSize(500);
    }

    @Test
    void construtorNovoTruncaStackTraceEm4000Caracteres() {
        String stackLongo = "y".repeat(5000);

        ErroRegistrado erro = new ErroRegistrado("op", "Classe", "msg", stackLongo);

        assertThat(erro.getStackTrace()).hasSize(4000);
    }

    @Test
    void construtorNovoTruncaOperacaoEm100Caracteres() {
        String operacaoLonga = "z".repeat(200);

        ErroRegistrado erro = new ErroRegistrado(operacaoLonga, "Classe", "msg", "stack");

        assertThat(erro.getOperacao()).hasSize(100);
    }

    @Test
    void construtorNovoTruncaClasseErroEm200Caracteres() {
        String classeLonga = "w".repeat(300);

        ErroRegistrado erro = new ErroRegistrado("op", classeLonga, "msg", "stack");

        assertThat(erro.getClasseErro()).hasSize(200);
    }

    @Test
    void construtorNovoComStackTraceNuloPreservaNulo() {
        ErroRegistrado erro = new ErroRegistrado("op", "Classe", "msg", null);

        assertThat(erro.getStackTrace()).isNull();
    }

    @Test
    void construtorNovoNaoTruncaQuandoCamposDentroDoLimite() {
        ErroRegistrado erro = new ErroRegistrado("op", "Classe", "curta", "stack curto");

        assertThat(erro.getOperacao()).isEqualTo("op");
        assertThat(erro.getClasseErro()).isEqualTo("Classe");
        assertThat(erro.getMensagem()).isEqualTo("curta");
        assertThat(erro.getStackTrace()).isEqualTo("stack curto");
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoPreservaTodosOsCampos() {
        // Given
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-05-17T10:00:00Z");

        // When
        ErroRegistrado erro = new ErroRegistrado(
                id, "ERR-ABCDEF12", "GET /api/contas", "IllegalStateException",
                "estado invalido", "stack completo", criadoEm);

        // Then
        assertThat(erro.getId()).isEqualTo(id);
        assertThat(erro.getCodigo()).isEqualTo("ERR-ABCDEF12");
        assertThat(erro.getOperacao()).isEqualTo("GET /api/contas");
        assertThat(erro.getClasseErro()).isEqualTo("IllegalStateException");
        assertThat(erro.getMensagem()).isEqualTo("estado invalido");
        assertThat(erro.getStackTrace()).isEqualTo("stack completo");
        assertThat(erro.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void construtorReconstrucaoNaoAplicaTruncamentoNemTextoPadrao() {
        // Reconstrucao confia nos dados persistidos: nao trunca nem substitui nulos.
        ErroRegistrado erro = new ErroRegistrado(
                UUID.randomUUID(), "ERR-00000000", null, null, null, null, Instant.now());

        assertThat(erro.getOperacao()).isNull();
        assertThat(erro.getMensagem()).isNull();
    }
}
