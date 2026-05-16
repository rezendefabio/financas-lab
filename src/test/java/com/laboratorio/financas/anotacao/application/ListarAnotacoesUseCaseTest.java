package com.laboratorio.financas.anotacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarAnotacoesUseCaseTest {

    private AnotacaoRepository repository;
    private ListarAnotacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AnotacaoRepository.class);
        useCase = new ListarAnotacoesUseCase(repository);
    }

    @Test
    void executarRetornaListaDeAnotacoesDoUsuario() {
        // Given
        UUID usuarioId = UUID.randomUUID();
        Anotacao a1 = new Anotacao(
                usuarioId, "Titulo 1", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA, null, null
        );
        Anotacao a2 = new Anotacao(
                usuarioId, "Titulo 2", null, TipoAnotacao.ALERTA, PrioridadeAnotacao.ALTA, null, null
        );
        when(repository.listarPorUsuario(usuarioId)).thenReturn(List.of(a1, a2));

        // When
        List<Anotacao> resultado = useCase.executar(usuarioId);

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactlyInAnyOrder(a1, a2);
    }

    @Test
    void executarRetornaListaVaziaQuandoNaoHaAnotacoes() {
        // Given
        UUID usuarioId = UUID.randomUUID();
        when(repository.listarPorUsuario(usuarioId)).thenReturn(List.of());

        // When
        List<Anotacao> resultado = useCase.executar(usuarioId);

        // Then
        assertThat(resultado).isEmpty();
    }
}
