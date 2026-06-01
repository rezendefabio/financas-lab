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
        Payee existente = payee(id, USER_ID, "Supermercado");
        when(repository.findById(id)).thenReturn(Optional.of(existente));

        useCase.executar(id);

        verify(repository).deleteById(id);
    }

    @Test
    void executarPayeeNaoEncontradoLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(PayeeNaoEncontradoException.class);
    }

    @Test
    void executarDeletaPayeeCriadoPorOutroUsuario() {
        UUID id = UUID.randomUUID();
        UUID outroUserId = UUID.randomUUID();
        Payee existente = payee(id, outroUserId, "Supermercado");
        when(repository.findById(id)).thenReturn(Optional.of(existente));

        useCase.executar(id);

        verify(repository).deleteById(id);
    }

    private Payee payee(UUID id, UUID userId, String nome) {
        Instant now = Instant.now();
        return new Payee(id, userId, nome, null, now, now);
    }
}
