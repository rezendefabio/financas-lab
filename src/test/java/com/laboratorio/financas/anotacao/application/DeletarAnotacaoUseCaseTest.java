package com.laboratorio.financas.anotacao.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

class DeletarAnotacaoUseCaseTest {

    private AnotacaoRepository repository;
    private BuscarAnotacaoPorIdUseCase buscarUseCase;
    private DeletarAnotacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AnotacaoRepository.class);
        buscarUseCase = new BuscarAnotacaoPorIdUseCase(repository);
        useCase = new DeletarAnotacaoUseCase(buscarUseCase, repository);
    }

    @Test
    void executarDeletaAnotacaoExistente() {
        // Given
        UUID usuarioId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Anotacao existente = new Anotacao(
                id, usuarioId, "Titulo", null,
                TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA,
                null, null, java.time.Instant.now(), java.time.Instant.now()
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(existente));

        // When
        useCase.executar(id);

        // Then
        verify(repository, times(1)).deletar(id);
    }

    @Test
    void executarLancaExcecaoQuandoNaoExiste() {
        // Given
        UUID idInexistente = UUID.randomUUID();
        when(repository.buscarPorId(idInexistente)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.executar(idInexistente))
                .isInstanceOf(AnotacaoNaoEncontradaException.class);
    }
}
