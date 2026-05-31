package com.laboratorio.financas.assinatura.application;

import static org.assertj.core.api.Assertions.assertThat;
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

class AtualizarAssinaturaUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Money VALOR = new Money(new BigDecimal("29.90"), Currency.getInstance("BRL"));
    private static final Money NOVO_VALOR = new Money(new BigDecimal("49.90"), Currency.getInstance("BRL"));
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    private AssinaturaRepository repository;
    private AtualizarAssinaturaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AssinaturaRepository.class);
        useCase = new AtualizarAssinaturaUseCase(repository);
    }

    @Test
    void executarComIdExistenteAtualizaCampos() {
        Assinatura existente = new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));

        AtualizarAssinaturaUseCase.Comando cmd = new AtualizarAssinaturaUseCase.Comando(
                existente.getId(), "Spotify", TipoAssinatura.OUTROS, NOVO_VALOR,
                LocalDate.of(2026, 7, 1), false);
        Assinatura resultado = useCase.executar(cmd);

        assertThat(resultado.getNome()).isEqualTo("Spotify");
        assertThat(resultado.getTipo()).isEqualTo(TipoAssinatura.OUTROS);
        assertThat(resultado.getValorMensal()).isEqualTo(NOVO_VALOR);
        assertThat(resultado.isAtiva()).isFalse();
        verify(repository).atualizar(any(Assinatura.class));
    }

    @Test
    void executarComIdInexistenteLancaAssinaturaNaoEncontradaException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new AtualizarAssinaturaUseCase.Comando(
                id, "X", TipoAssinatura.STREAMING, VALOR, RENOVACAO, true)))
                .isInstanceOf(AssinaturaNaoEncontradaException.class);

        verify(repository, never()).atualizar(any());
    }
}
