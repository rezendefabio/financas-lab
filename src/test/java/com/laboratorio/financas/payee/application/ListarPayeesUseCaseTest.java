package com.laboratorio.financas.payee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarPayeesUseCaseTest {

    private PayeeRepository repository;
    private ListarPayeesUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PayeeRepository.class);
        useCase = new ListarPayeesUseCase(repository);
    }

    @Test
    void executarRetornaPayeesDoUsuario() {
        List<Payee> payees = List.of(
                new Payee(USER_ID, "Supermercado", null),
                new Payee(USER_ID, "Farmacia", null)
        );
        when(repository.findByUserId(USER_ID)).thenReturn(payees);

        List<Payee> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).isSameAs(payees);
    }

    @Test
    void executarRetornaListaVaziaQuandoNaoHaPayees() {
        when(repository.findByUserId(USER_ID)).thenReturn(List.of());

        List<Payee> resultado = useCase.executar(USER_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void executarChamaRepositorioComUserIdCorreto() {
        when(repository.findByUserId(USER_ID)).thenReturn(List.of());

        useCase.executar(USER_ID);

        verify(repository).findByUserId(USER_ID);
    }
}
