package com.laboratorio.financas.carteira.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraNaoEncontradaException;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletarCarteiraUseCaseTest {

    private CarteiraRepository carteiraRepository;
    private DeletarCarteiraUseCase useCase;

    @BeforeEach
    void setUp() {
        carteiraRepository = Mockito.mock(CarteiraRepository.class);
        useCase = new DeletarCarteiraUseCase(carteiraRepository);
    }

    @Test
    void executarDeletaCarteiraExistente() {
        UUID id = UUID.randomUUID();
        Carteira existente = new Carteira(UUID.randomUUID(), UUID.randomUUID(), "Tesouro", TipoCarteira.RENDA_FIXA);
        when(carteiraRepository.buscarPorId(id)).thenReturn(Optional.of(existente));

        useCase.executar(id);

        verify(carteiraRepository, times(1)).deletar(id);
    }

    @Test
    void executarComCarteiraInexistenteLancaExcecaoENaoDeleta() {
        UUID id = UUID.randomUUID();
        when(carteiraRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(CarteiraNaoEncontradaException.class)
                .satisfies(ex -> assertThat(((CarteiraNaoEncontradaException) ex).getId()).isEqualTo(id));

        verify(carteiraRepository, never()).deletar(id);
    }
}
