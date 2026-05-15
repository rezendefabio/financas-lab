package com.laboratorio.financas.payee.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeNaoEncontradoException;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletarPayeeUseCaseTest {

    private PayeeRepository repository;
    private DeletarPayeeUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PayeeRepository.class);
        useCase = new DeletarPayeeUseCase(repository);
    }

    @Test
    void executarCaminhoFelizDeletaPayee() {
        UUID id = UUID.randomUUID();
        Payee existente = payee(id, "Supermercado");
        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(existente));

        useCase.executar(id, USER_ID);

        verify(repository).deleteById(id);
    }

    @Test
    void executarPayeeNaoEncontradoLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id, USER_ID))
                .isInstanceOf(PayeeNaoEncontradoException.class);
    }

    private Payee payee(UUID id, String nome) {
        Instant now = Instant.now();
        return new Payee(id, USER_ID, nome, null, now, now);
    }
}
