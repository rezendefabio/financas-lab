package com.laboratorio.financas.incidente.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.FiltrosIncidente;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ErroRegistradoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private ErroRegistradoRepositoryImpl repository;

    @Autowired
    private ErroRegistradoJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarPersisteErroERetornaInstanciaEquivalente() {
        // Given
        ErroRegistrado novo = new ErroRegistrado(
                "POST /api/transacoes", "NullPointerException", "valor nulo", "stack...");

        // When
        ErroRegistrado salvo = repository.salvar(novo);

        // Then
        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getCodigo()).isEqualTo(novo.getCodigo());
        assertThat(salvo.getOperacao()).isEqualTo("POST /api/transacoes");
        assertThat(salvo.getClasseErro()).isEqualTo("NullPointerException");
        assertThat(salvo.getMensagem()).isEqualTo("valor nulo");
        assertThat(salvo.getStackTrace()).isEqualTo("stack...");
        assertThat(salvo.getCriadoEm()).isNotNull();
    }

    @Test
    void buscarPorCodigoRetornaErroQuandoExiste() {
        // Given
        ErroRegistrado novo = new ErroRegistrado("op", "Classe", "msg", "stack");
        repository.salvar(novo);

        // When
        Optional<ErroRegistrado> resultado = repository.buscarPorCodigo(novo.getCodigo());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
        assertThat(resultado.get().getCodigo()).isEqualTo(novo.getCodigo());
        assertThat(resultado.get().getOperacao()).isEqualTo("op");
    }

    @Test
    void buscarPorCodigoRetornaVazioQuandoNaoExiste() {
        // When
        Optional<ErroRegistrado> resultado = repository.buscarPorCodigo("ERR-00000000");

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void salvarPersisteStackTraceLongoTruncadoEm4000() {
        // Given — o truncamento ocorre no construtor do dominio.
        ErroRegistrado novo = new ErroRegistrado("op", "Classe", "msg", "y".repeat(5000));

        // When
        repository.salvar(novo);
        Optional<ErroRegistrado> recuperado = repository.buscarPorCodigo(novo.getCodigo());

        // Then
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getStackTrace()).hasSize(4000);
    }

    @Test
    void salvarComMensagemNulaPersisteTextoPadrao() {
        // Given
        ErroRegistrado novo = new ErroRegistrado("op", "Classe", null, "stack");

        // When
        repository.salvar(novo);
        Optional<ErroRegistrado> recuperado = repository.buscarPorCodigo(novo.getCodigo());

        // Then
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getMensagem()).isEqualTo("(sem mensagem)");
    }

    @Test
    void listarComFiltrosSemFiltrosRetornaTodos() {
        // Given
        repository.salvar(new ErroRegistrado("POST /api/transacoes", "NullPointerException", "m", "s"));
        repository.salvar(new ErroRegistrado("GET /api/contas", "RuntimeException", "m", "s"));

        // When
        List<ErroRegistrado> resultado = repository.listarComFiltros(
                new FiltrosIncidente(null, null, null, null));

        // Then
        assertThat(resultado).hasSize(2);
    }

    @Test
    void listarComFiltrosFiltraPorClasseErroContainsCaseInsensitive() {
        // Given
        repository.salvar(new ErroRegistrado("op", "NullPointerException", "m", "s"));
        repository.salvar(new ErroRegistrado("op", "RuntimeException", "m", "s"));

        // When — fragmento parcial em caixa diferente
        List<ErroRegistrado> resultado = repository.listarComFiltros(
                new FiltrosIncidente(null, null, "nullpointer", null));

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getClasseErro()).isEqualTo("NullPointerException");
    }

    @Test
    void listarComFiltrosFiltraPorOperacaoContains() {
        // Given
        repository.salvar(new ErroRegistrado("POST /api/transacoes", "Classe", "m", "s"));
        repository.salvar(new ErroRegistrado("GET /api/contas", "Classe", "m", "s"));

        // When
        List<ErroRegistrado> resultado = repository.listarComFiltros(
                new FiltrosIncidente(null, null, null, "transacoes"));

        // Then
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getOperacao()).isEqualTo("POST /api/transacoes");
    }

    @Test
    void listarComFiltrosFiltraPorPeriodo() {
        // Given
        repository.salvar(new ErroRegistrado("op", "Classe", "m", "s"));
        Instant futuro = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant passado = Instant.now().minus(1, ChronoUnit.DAYS);

        // When / Then — criadoApartirDe no passado inclui o registro
        assertThat(repository.listarComFiltros(
                new FiltrosIncidente(passado, null, null, null))).hasSize(1);

        // criadoApartirDe no futuro exclui o registro
        assertThat(repository.listarComFiltros(
                new FiltrosIncidente(futuro, null, null, null))).isEmpty();

        // criadoAte no passado exclui o registro
        assertThat(repository.listarComFiltros(
                new FiltrosIncidente(null, passado, null, null))).isEmpty();
    }
}
