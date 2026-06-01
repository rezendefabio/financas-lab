package com.laboratorio.financas.anotacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoNaoEncontradaException;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarAnotacaoPorIdUseCaseTest {

    private AnotacaoRepository repository;
    private BuscarAnotacaoPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AnotacaoRepository.class);
        useCase = new BuscarAnotacaoPorIdUseCase(repository);
    }

    @Test
    void executarRetornaAnotacaoQuandoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Anotacao anotacao = new Anotacao(
                userId, "Titulo", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA, null, null
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(anotacao));

        // When
        Anotacao resultado = useCase.executar(id);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado).isSameAs(anotacao);
    }

    @Test
    void executarLancaExcecaoQuandoNaoExiste() {
        // Given
        UUID idInexistente = UUID.randomUUID();
        when(repository.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(idInexistente))
                .isInstanceOf(AnotacaoNaoEncontradaException.class)
                .hasMessageContaining(idInexistente.toString());
    }
}
