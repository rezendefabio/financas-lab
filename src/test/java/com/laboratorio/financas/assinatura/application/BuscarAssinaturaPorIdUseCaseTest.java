package com.laboratorio.financas.assinatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaNaoEncontradaException;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuscarAssinaturaPorIdUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Money VALOR = new Money(new BigDecimal("29.90"), Currency.getInstance("BRL"));
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    private AssinaturaRepository repository;
    private BuscarAssinaturaPorIdUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AssinaturaRepository.class);
        useCase = new BuscarAssinaturaPorIdUseCase(repository);
    }

    @Test
    void executarComIdExistenteRetornaEntidade() {
        Assinatura existente = new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        Assinatura resultado = useCase.executar(existente.getId());

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Netflix");
    }

    @Test
    void executarComIdInexistenteLancaAssinaturaNaoEncontradaException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(AssinaturaNaoEncontradaException.class);
    }
}
