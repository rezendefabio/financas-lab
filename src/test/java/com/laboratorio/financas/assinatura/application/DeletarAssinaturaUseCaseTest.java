package com.laboratorio.financas.assinatura.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

class DeletarAssinaturaUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Money VALOR = new Money(new BigDecimal("29.90"), Currency.getInstance("BRL"));
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    private AssinaturaRepository repository;
    private DeletarAssinaturaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AssinaturaRepository.class);
        useCase = new DeletarAssinaturaUseCase(repository);
    }

    @Test
    void executarComIdExistenteDeleta() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(
                new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO)));

        useCase.executar(id);

        verify(repository).deletar(id);
    }

    @Test
    void executarComIdInexistenteLancaExcecaoENaoDeleta() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(AssinaturaNaoEncontradaException.class);

        verify(repository, never()).deletar(any());
    }
}
