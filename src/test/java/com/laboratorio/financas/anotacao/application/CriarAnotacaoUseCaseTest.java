package com.laboratorio.financas.anotacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarAnotacaoUseCaseTest {

    private AnotacaoRepository repository;
    private CriarAnotacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AnotacaoRepository.class);
        useCase = new CriarAnotacaoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaAnotacaoCriada() {
        // Given
        UUID userId = UUID.randomUUID();
        Anotacao anotacaoSalva = new Anotacao(
                userId, "Lembrete", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA, null, null
        );
        when(repository.salvar(any(Anotacao.class))).thenReturn(anotacaoSalva);

        // When
        Anotacao resultado = useCase.executar(
                userId, "Lembrete", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA, null, null
        );

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Lembrete");
        verify(repository, times(1)).salvar(any(Anotacao.class));
    }

    @Test
    void executarChamaRepositorioSalvarUmaVez() {
        // Given
        UUID userId = UUID.randomUUID();
        Anotacao anotacaoSalva = new Anotacao(
                userId, "Observacao", null, TipoAnotacao.OBSERVACAO, PrioridadeAnotacao.BAIXA, null, null
        );
        when(repository.salvar(any(Anotacao.class))).thenReturn(anotacaoSalva);

        // When
        useCase.executar(userId, "Observacao", null, TipoAnotacao.OBSERVACAO, PrioridadeAnotacao.BAIXA, null, null);

        // Then
        verify(repository, times(1)).salvar(any(Anotacao.class));
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        // Given
        UUID userId = UUID.randomUUID();
        Anotacao esperado = new Anotacao(
                userId, "Alerta", null, TipoAnotacao.ALERTA, PrioridadeAnotacao.URGENTE, null, null
        );
        when(repository.salvar(any(Anotacao.class))).thenReturn(esperado);

        // When
        Anotacao resultado = useCase.executar(
                userId, "Alerta", null, TipoAnotacao.ALERTA, PrioridadeAnotacao.URGENTE, null, null
        );

        // Then
        assertThat(resultado).isSameAs(esperado);
    }
}
