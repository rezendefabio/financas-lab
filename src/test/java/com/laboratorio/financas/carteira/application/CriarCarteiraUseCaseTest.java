package com.laboratorio.financas.carteira.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CriarCarteiraUseCaseTest {

    private CarteiraRepository carteiraRepository;
    private CriarCarteiraUseCase useCase;

    @BeforeEach
    void setUp() {
        carteiraRepository = Mockito.mock(CarteiraRepository.class);
        useCase = new CriarCarteiraUseCase(carteiraRepository);
    }

    @Test
    void executarCriaCarteiraESalvaNoRepositorio() {
        UUID userId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        when(carteiraRepository.salvar(any(Carteira.class))).thenAnswer(inv -> inv.getArgument(0));

        Carteira resultado = useCase.executar(
                new CriarCarteiraUseCase.Comando(userId, contaId, "Tesouro", TipoCarteira.RENDA_FIXA));

        assertThat(resultado.getUserId()).isEqualTo(userId);
        assertThat(resultado.getContaId()).isEqualTo(contaId);
        assertThat(resultado.getNome()).isEqualTo("Tesouro");
        assertThat(resultado.getTipo()).isEqualTo(TipoCarteira.RENDA_FIXA);
        assertThat(resultado.isAtivo()).isTrue();
    }

    @Test
    void executarChamaSalvarUmaVezComCarteiraConstruida() {
        UUID userId = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        when(carteiraRepository.salvar(any(Carteira.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(new CriarCarteiraUseCase.Comando(userId, contaId, "Acoes", TipoCarteira.RENDA_VARIAVEL));

        ArgumentCaptor<Carteira> captor = ArgumentCaptor.forClass(Carteira.class);
        verify(carteiraRepository, times(1)).salvar(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("Acoes");
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoCarteira.RENDA_VARIAVEL);
    }
}
