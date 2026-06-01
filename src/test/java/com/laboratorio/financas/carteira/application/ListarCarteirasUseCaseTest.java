package com.laboratorio.financas.carteira.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarCarteirasUseCaseTest {

    private CarteiraRepository carteiraRepository;
    private ListarCarteirasUseCase useCase;

    @BeforeEach
    void setUp() {
        carteiraRepository = Mockito.mock(CarteiraRepository.class);
        useCase = new ListarCarteirasUseCase(carteiraRepository);
    }

    @Test
    void executarRetornaTodasAsCarteiras() {
        UUID contaId = UUID.randomUUID();
        List<Carteira> carteiras = List.of(
                new Carteira(UUID.randomUUID(), contaId, "Tesouro", TipoCarteira.RENDA_FIXA),
                new Carteira(UUID.randomUUID(), contaId, "Acoes", TipoCarteira.RENDA_VARIAVEL)
        );
        when(carteiraRepository.listarTodos()).thenReturn(carteiras);

        List<Carteira> resultado = useCase.executar();

        assertThat(resultado).hasSize(2);
        verify(carteiraRepository, times(1)).listarTodos();
    }

    @Test
    void executarRetornaListaVaziaQuandoNaoHaCarteiras() {
        when(carteiraRepository.listarTodos()).thenReturn(List.of());

        List<Carteira> resultado = useCase.executar();

        assertThat(resultado).isEmpty();
    }
}
