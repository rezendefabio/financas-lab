package com.laboratorio.financas.assinatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarAssinaturasUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Money VALOR = new Money(new BigDecimal("29.90"), Currency.getInstance("BRL"));
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    private AssinaturaRepository repository;
    private ListarAssinaturasUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AssinaturaRepository.class);
        useCase = new ListarAssinaturasUseCase(repository);
    }

    @Test
    void executarRetornaListaDoRepository() {
        when(repository.listarPorUserId(USER_ID)).thenReturn(List.of(
                new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO),
                new Assinatura(USER_ID, "Spotify", TipoAssinatura.STREAMING, VALOR, RENOVACAO)));

        List<Assinatura> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(a -> a.getUserId().equals(USER_ID));
    }
}
