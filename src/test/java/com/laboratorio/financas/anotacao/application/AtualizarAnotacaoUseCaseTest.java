package com.laboratorio.financas.anotacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarAnotacaoUseCaseTest {

    private AnotacaoRepository repository;
    private BuscarAnotacaoPorIdUseCase buscarUseCase;
    private AtualizarAnotacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AnotacaoRepository.class);
        buscarUseCase = new BuscarAnotacaoPorIdUseCase(repository);
        useCase = new AtualizarAnotacaoUseCase(buscarUseCase, repository);
    }

    @Test
    void executarAtualizaERetornaAnotacao() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Anotacao existente = new Anotacao(
                id, userId, "Titulo original", null,
                TipoAnotacao.LEMBRETE, PrioridadeAnotacao.BAIXA,
                null, null, java.time.Instant.now(), java.time.Instant.now()
        );
        when(repository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(repository.salvar(any(Anotacao.class))).thenReturn(existente);

        // When
        Anotacao resultado = useCase.executar(
                id, "Novo titulo", "Conteudo", TipoAnotacao.ALERTA, PrioridadeAnotacao.ALTA, null, null
        );

        // Then
        assertThat(resultado.getTitulo()).isEqualTo("Novo titulo");
        assertThat(resultado.getTipo()).isEqualTo(TipoAnotacao.ALERTA);
        verify(repository).salvar(any(Anotacao.class));
    }
}
