package com.laboratorio.financas.anexo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnexoTest {

    private static final String NOME = "comprovante.pdf";
    private static final String TIPO = "application/pdf";
    private static final long TAMANHO = 2048L;
    private static final String ENTIDADE_TIPO = "TRANSACAO";

    // --- Construtor de criacao ---

    @Test
    void construtorDeCriacaoComArgumentosValidosCriaAnexo() {
        // Given
        UUID entidadeId = UUID.randomUUID();
        Instant antes = Instant.now();

        // When
        Anexo anexo = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, entidadeId);

        // Then
        assertThat(anexo.getId()).isNotNull();
        assertThat(anexo.getNome()).isEqualTo(NOME);
        assertThat(anexo.getTipoConteudo()).isEqualTo(TIPO);
        assertThat(anexo.getTamanho()).isEqualTo(TAMANHO);
        assertThat(anexo.getEntidadeTipo()).isEqualTo(ENTIDADE_TIPO);
        assertThat(anexo.getEntidadeId()).isEqualTo(entidadeId);
        assertThat(anexo.getCriadoEm()).isBetween(antes, Instant.now());
    }

    @Test
    void construtorDeCriacaoGeraChaveArmazenamentoComEntidadeTipoEmMinusculo() {
        // Given
        UUID entidadeId = UUID.randomUUID();

        // When
        Anexo anexo = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, entidadeId);

        // Then
        assertThat(anexo.getChaveArmazenamento())
                .isEqualTo("transacao/" + entidadeId + "/" + anexo.getId() + ".pdf");
    }

    @Test
    void construtorDeCriacaoUsaExtensaoBinQuandoNomeNaoTemPonto() {
        // Given
        UUID entidadeId = UUID.randomUUID();

        // When
        Anexo anexo = new Anexo("arquivo_sem_extensao", TIPO, TAMANHO, ENTIDADE_TIPO, entidadeId);

        // Then
        assertThat(anexo.getChaveArmazenamento())
                .isEqualTo("transacao/" + entidadeId + "/" + anexo.getId() + ".bin");
    }

    @Test
    void construtorDeCriacaoUsaExtensaoBinQuandoNomeTerminaComPonto() {
        // Given
        UUID entidadeId = UUID.randomUUID();

        // When
        Anexo anexo = new Anexo("arquivo.", TIPO, TAMANHO, ENTIDADE_TIPO, entidadeId);

        // Then
        assertThat(anexo.getChaveArmazenamento()).endsWith(".bin");
    }

    @Test
    void construtorDeCriacaoNormalizaExtensaoParaMinusculo() {
        // Given
        UUID entidadeId = UUID.randomUUID();

        // When
        Anexo anexo = new Anexo("RELATORIO.PDF", TIPO, TAMANHO, ENTIDADE_TIPO, entidadeId);

        // Then
        assertThat(anexo.getChaveArmazenamento()).endsWith(".pdf");
    }

    @Test
    void construtorDeCriacaoUsaUltimaExtensaoQuandoNomeTemMultiplosPontos() {
        // Given
        UUID entidadeId = UUID.randomUUID();

        // When
        Anexo anexo = new Anexo("backup.tar.gz", TIPO, TAMANHO, ENTIDADE_TIPO, entidadeId);

        // Then
        assertThat(anexo.getChaveArmazenamento()).endsWith(".gz");
    }

    @Test
    void construtorDeCriacaoRejeitaNomeNulo() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Anexo(null, TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaNomeVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anexo("  ", TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaTipoConteudoNulo() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Anexo(NOME, null, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaTipoConteudoVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anexo(NOME, " ", TAMANHO, ENTIDADE_TIPO, UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaEntidadeTipoNulo() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Anexo(NOME, TIPO, TAMANHO, null, UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaEntidadeTipoVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anexo(NOME, TIPO, TAMANHO, "", UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaEntidadeIdNulo() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, null));
    }

    @Test
    void construtorDeCriacaoRejeitaTamanhoZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anexo(NOME, TIPO, 0L, ENTIDADE_TIPO, UUID.randomUUID()));
    }

    @Test
    void construtorDeCriacaoRejeitaTamanhoNegativo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Anexo(NOME, TIPO, -1L, ENTIDADE_TIPO, UUID.randomUUID()));
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorDeReconstrucaoPreservaTodosOsCampos() {
        // Given
        UUID id = UUID.randomUUID();
        UUID entidadeId = UUID.randomUUID();
        Instant criadoEm = Instant.now();
        String chave = "transacao/" + entidadeId + "/" + id + ".pdf";

        // When
        Anexo anexo = new Anexo(id, NOME, TIPO, TAMANHO, chave, ENTIDADE_TIPO, entidadeId, criadoEm);

        // Then
        assertThat(anexo.getId()).isEqualTo(id);
        assertThat(anexo.getNome()).isEqualTo(NOME);
        assertThat(anexo.getTipoConteudo()).isEqualTo(TIPO);
        assertThat(anexo.getTamanho()).isEqualTo(TAMANHO);
        assertThat(anexo.getChaveArmazenamento()).isEqualTo(chave);
        assertThat(anexo.getEntidadeTipo()).isEqualTo(ENTIDADE_TIPO);
        assertThat(anexo.getEntidadeId()).isEqualTo(entidadeId);
        assertThat(anexo.getCriadoEm()).isEqualTo(criadoEm);
    }

    // --- equals / hashCode / toString ---

    @Test
    void equalsRetornaTrueParaMesmoId() {
        // Given
        UUID id = UUID.randomUUID();
        UUID entidadeId = UUID.randomUUID();
        Instant criadoEm = Instant.now();
        Anexo a = new Anexo(id, NOME, TIPO, TAMANHO, "k", ENTIDADE_TIPO, entidadeId, criadoEm);
        Anexo b = new Anexo(id, "outro.txt", "text/plain", 1L, "k2", "CONTA", UUID.randomUUID(), criadoEm);

        // Then
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void equalsRetornaFalseParaIdsDiferentes() {
        // Given
        Anexo a = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID());
        Anexo b = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID());

        // Then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsRetornaTrueParaMesmaInstancia() {
        Anexo a = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID());
        assertThat(a).isEqualTo(a);
    }

    @Test
    void equalsRetornaFalseParaTipoDiferente() {
        Anexo a = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID());
        assertThat(a).isNotEqualTo("nao e anexo");
    }

    @Test
    void toStringContemIdEntidadeTipoENome() {
        // Given
        Anexo anexo = new Anexo(NOME, TIPO, TAMANHO, ENTIDADE_TIPO, UUID.randomUUID());

        // Then
        assertThat(anexo.toString())
                .contains(anexo.getId().toString())
                .contains(NOME)
                .contains(ENTIDADE_TIPO);
    }
}
