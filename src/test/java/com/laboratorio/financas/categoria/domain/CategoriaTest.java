package com.laboratorio.financas.categoria.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoriaTest {

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosCriaCategoria() {
        // Given
        Instant antes = Instant.now();

        // When
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA);

        // Then
        Instant depois = Instant.now();
        assertThat(categoria.getId()).isNotNull();
        assertThat(categoria.getCriadoEm()).isBetween(antes, depois);
        assertThat(categoria.getAtualizadoEm()).isEqualTo(categoria.getCriadoEm());
        assertThat(categoria.getCategoriaPaiId()).isNull();
    }

    @Test
    void construtorNovoComCategoriaPaiIdPreservaCampo() {
        UUID paiId = UUID.randomUUID();
        Categoria categoria = new Categoria("Mercado", TipoCategoria.DESPESA, paiId);
        assertThat(categoria.getCategoriaPaiId()).isEqualTo(paiId);
    }

    @Test
    void construtorNovoSemCategoriaPaiIdRetornaNull() {
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA, null);
        assertThat(categoria.getCategoriaPaiId()).isNull();
    }

    @Test
    void construtorNovoComNomeComEspacosNasPontasTrimaONome() {
        Categoria categoria = new Categoria("  Alimentacao  ", TipoCategoria.DESPESA);
        assertThat(categoria.getNome()).isEqualTo("Alimentacao");
    }

    @Test
    void construtorNovoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Categoria(null, TipoCategoria.RECEITA))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Categoria("   ", TipoCategoria.RECEITA))
                .withMessageContaining("nome");
    }

    @Test
    void construtorNovoComNomeAcimaMaximoLancaIllegalArgumentException() {
        String nomeLongo = "a".repeat(101);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Categoria(nomeLongo, TipoCategoria.RECEITA))
                .withMessageContaining("100");
    }

    @Test
    void construtorNovoComNomeExatamente100CharsAceita() {
        String nome100 = "a".repeat(100);
        Categoria categoria = new Categoria(nome100, TipoCategoria.DESPESA);
        assertThat(categoria.getNome()).hasSize(100);
    }

    @Test
    void construtorNovoComTipoNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Categoria("Salario", null))
                .withMessageContaining("tipo");
    }

    @Test
    void construtorNovoAceitaTipoReceitaEDespesa() {
        Categoria receita = new Categoria("Salario", TipoCategoria.RECEITA);
        Categoria despesa = new Categoria("Aluguel", TipoCategoria.DESPESA);
        assertThat(receita.getTipo()).isEqualTo(TipoCategoria.RECEITA);
        assertThat(despesa.getTipo()).isEqualTo(TipoCategoria.DESPESA);
    }

    @Test
    void duasCategoriasNovasTenIdsDiferentes() {
        Categoria c1 = new Categoria("Salario", TipoCategoria.RECEITA);
        Categoria c2 = new Categoria("Freelance", TipoCategoria.RECEITA);
        assertThat(c1.getId()).isNotEqualTo(c2.getId());
    }

    // --- Construtor de reconstrucao ---

    @Test
    void construtorReconstrucaoComTodosOsCamposValidosPreservaValores() {
        // Given
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Instant atualizadoEm = Instant.parse("2026-01-02T12:00:00Z");

        // When
        Categoria categoria = new Categoria(id, "Aluguel", TipoCategoria.DESPESA, criadoEm, atualizadoEm);

        // Then
        assertThat(categoria.getId()).isEqualTo(id);
        assertThat(categoria.getNome()).isEqualTo("Aluguel");
        assertThat(categoria.getTipo()).isEqualTo(TipoCategoria.DESPESA);
        assertThat(categoria.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(categoria.getAtualizadoEm()).isEqualTo(atualizadoEm);
        assertThat(categoria.getCategoriaPaiId()).isNull();
    }

    @Test
    void construtorReconstrucaoComCategoriaPaiIdPreservaCampo() {
        UUID id = UUID.randomUUID();
        UUID paiId = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Categoria categoria = new Categoria(id, "Mercado", TipoCategoria.DESPESA, paiId, criadoEm, null);
        assertThat(categoria.getCategoriaPaiId()).isEqualTo(paiId);
    }

    @Test
    void construtorReconstrucaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Categoria(null, "Aluguel", TipoCategoria.DESPESA, Instant.now(), null))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstrucaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Categoria(UUID.randomUUID(), "Aluguel", TipoCategoria.DESPESA, null, null))
                .withMessageContaining("criadoEm");
    }

    @Test
    void construtorReconstrucaoComAtualizadoEmNuloDefaultaParaCriadoEm() {
        Instant criadoEm = Instant.parse("2026-01-01T10:00:00Z");
        Categoria categoria = new Categoria(UUID.randomUUID(), "Salario", TipoCategoria.RECEITA, criadoEm, null);
        assertThat(categoria.getAtualizadoEm()).isEqualTo(criadoEm);
    }

    // --- equals e hashCode ---

    @Test
    void duasCategoriasComMesmoIdSaoIguais() {
        UUID id = UUID.randomUUID();
        Instant t = Instant.now();
        Categoria c1 = new Categoria(id, "Nome A", TipoCategoria.RECEITA, t, t);
        Categoria c2 = new Categoria(id, "Nome B", TipoCategoria.DESPESA, t, t);
        assertThat(c1).isEqualTo(c2);
    }

    @Test
    void equalsRetornaFalseParaNullEOutroTipo() {
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA);
        assertThat(categoria.equals(null)).isFalse();
        assertThat(categoria.equals("string")).isFalse();
    }

    // --- toString ---

    @Test
    void toStringContemIdNomeETipo() {
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA);
        String resultado = categoria.toString();
        assertThat(resultado)
                .contains(categoria.getId().toString())
                .contains("Salario")
                .contains("RECEITA");
    }

    @Test
    void toStringNaoContemTimestamps() {
        Categoria categoria = new Categoria("Salario", TipoCategoria.RECEITA);
        String resultado = categoria.toString();
        assertThat(resultado)
                .doesNotContain("criadoEm")
                .doesNotContain("atualizadoEm");
    }
}
