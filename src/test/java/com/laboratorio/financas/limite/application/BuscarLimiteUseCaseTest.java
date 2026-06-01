package com.laboratorio.financas.limite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.Mockito;

class BuscarLimiteUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Currency BRL = Currency.getInstance("BRL");

    private LimiteRepository repository;
    private BuscarLimiteUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LimiteRepository.class);
        useCase = new BuscarLimiteUseCase(repository);
    }

    @Test
    void executarComLimiteExistenteRetornaLimite() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        Limite limite = new Limite(USER_ID, "X", TipoLimite.MENSAL, valor);
        when(repository.buscarPorId(limite.getId())).thenReturn(Optional.of(limite));

        Limite resultado = useCase.executar(limite.getId());

        assertThat(resultado).isEqualTo(limite);
    }

    @Test
    void executarComIdInexistenteLancaLimiteNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(LimiteNaoEncontradoException.class);
    }

    @Test
    void executarRetornaLimiteDeQualquerUsuario() {
        Money valor = new Money(new BigDecimal("100.00"), BRL);
        Limite limite = new Limite(UUID.randomUUID(), "X", TipoLimite.MENSAL, valor);
        when(repository.buscarPorId(limite.getId())).thenReturn(Optional.of(limite));

        Limite resultado = useCase.executar(limite.getId());

        assertThat(resultado).isEqualTo(limite);
    }
}
