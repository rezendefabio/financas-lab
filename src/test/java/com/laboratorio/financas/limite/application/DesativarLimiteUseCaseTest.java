package com.laboratorio.financas.limite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteNaoEncontradoException;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DesativarLimiteUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private LimiteRepository repository;
    private DesativarLimiteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LimiteRepository.class);
        useCase = new DesativarLimiteUseCase(repository);
    }

    @Test
    void executarFazSoftDeleteDesativandoLimite() {
        Money valor = new Money(new BigDecimal("50.00"), BRL);
        Limite existente = new Limite(USER_ID, "X", TipoLimite.MENSAL, valor);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));

        useCase.executar(existente.getId());

        ArgumentCaptor<Limite> captor = ArgumentCaptor.forClass(Limite.class);
        verify(repository).atualizar(captor.capture());
        assertThat(captor.getValue().isAtivo()).isFalse();
    }

    @Test
    void executarComIdInexistenteLancaLimiteNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(LimiteNaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }

    @Test
    void executarDesativaLimiteCriadoPorOutroUsuario() {
        Money valor = new Money(new BigDecimal("50.00"), BRL);
        Limite outro = new Limite(UUID.randomUUID(), "X", TipoLimite.MENSAL, valor);
        when(repository.buscarPorId(outro.getId())).thenReturn(Optional.of(outro));

        useCase.executar(outro.getId());

        ArgumentCaptor<Limite> captor = ArgumentCaptor.forClass(Limite.class);
        verify(repository).atualizar(captor.capture());
        assertThat(captor.getValue().isAtivo()).isFalse();
    }
}
