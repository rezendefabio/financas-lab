package com.laboratorio.financas.limite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarLimitesUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private LimiteRepository repository;
    private ListarLimitesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LimiteRepository.class);
        useCase = new ListarLimitesUseCase(repository);
    }

    @Test
    void executarRetornaLimitesDoUsuario() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(
                new Limite(USER_ID, "A", TipoLimite.DIARIO, valor),
                new Limite(USER_ID, "B", TipoLimite.MENSAL, valor)));

        List<Limite> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
    }
}
