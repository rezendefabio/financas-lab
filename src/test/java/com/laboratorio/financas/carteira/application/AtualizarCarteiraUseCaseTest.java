package com.laboratorio.financas.carteira.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

class AtualizarCarteiraUseCaseTest {

    private CarteiraRepository carteiraRepository;
    private AtualizarCarteiraUseCase useCase;

    @BeforeEach
    void setUp() {
        carteiraRepository = Mockito.mock(CarteiraRepository.class);
        useCase = new AtualizarCarteiraUseCase(carteiraRepository);
    }

    @Test
    void executarAtualizaNomeETipoESalva() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        Carteira existente = new Carteira(id, userId, contaId, "Antiga",
                TipoCarteira.RENDA_FIXA, true, java.time.Instant.now(), java.time.Instant.now());
        when(carteiraRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(carteiraRepository.atualizar(any(Carteira.class))).thenAnswer(inv -> inv.getArgument(0));

        Carteira resultado = useCase.executar(
                new AtualizarCarteiraUseCase.Comando(id, "Nova", TipoCarteira.CRIPTOMOEDA));

        assertThat(resultado.getNome()).isEqualTo("Nova");
        assertThat(resultado.getTipo()).isEqualTo(TipoCarteira.CRIPTOMOEDA);
        verify(carteiraRepository, times(1)).atualizar(any(Carteira.class));
    }

    @Test
    void executarComCarteiraInexistenteLancaExcecaoENaoSalva() {
        UUID id = UUID.randomUUID();
        when(carteiraRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(
                new AtualizarCarteiraUseCase.Comando(id, "Nova", TipoCarteira.OUTROS)))
                .isInstanceOf(CarteiraNaoEncontradaException.class)
                .satisfies(ex -> assertThat(((CarteiraNaoEncontradaException) ex).getId()).isEqualTo(id));

        verify(carteiraRepository, never()).atualizar(any(Carteira.class));
    }
}
