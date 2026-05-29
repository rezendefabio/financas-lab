package com.laboratorio.financas.limite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarLimiteUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private LimiteRepository repository;
    private CriarLimiteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LimiteRepository.class);
        useCase = new CriarLimiteUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaLimiteCriado() {
        Money valor = new Money(new BigDecimal("250.00"), BRL);
        Limite salvo = new Limite(USER_ID, "Mensal", TipoLimite.MENSAL, valor);
        when(repository.salvar(any(Limite.class))).thenReturn(salvo);

        CriarLimiteUseCase.Comando cmd =
                new CriarLimiteUseCase.Comando(USER_ID, "Mensal", TipoLimite.MENSAL, valor);
        Limite resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Mensal");
        assertThat(resultado.getTipo()).isEqualTo(TipoLimite.MENSAL);
        verify(repository, times(1)).salvar(any(Limite.class));
    }
}
