package com.laboratorio.financas.payee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.payee.application.dto.AtualizarPayeeComando;
import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeNaoEncontradoException;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarPayeeUseCaseTest {

    private PayeeRepository repository;
    private AtualizarPayeeUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PayeeRepository.class);
        useCase = new AtualizarPayeeUseCase(repository);
    }

    @Test
    void executarCaminhoFelizAtualizaENome() {
        Payee existente = payeeExistente("Supermercado", null);
        Payee atualizado = payeeExistente("Mercado Extra", null);
        when(repository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.save(any(Payee.class))).thenReturn(atualizado);
        AtualizarPayeeComando comando = new AtualizarPayeeComando(existente.getId(), "Mercado Extra", null);

        Payee resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Mercado Extra");
    }

    @Test
    void executarPayeeNaoEncontradoLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        AtualizarPayeeComando comando = new AtualizarPayeeComando(id, "Novo Nome", null);

        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(PayeeNaoEncontradoException.class);
    }

    @Test
    void executarAtualizaPayeeCriadoPorOutroUsuario() {
        UUID outroUserId = UUID.randomUUID();
        Instant now = Instant.now();
        Payee existente = new Payee(UUID.randomUUID(), outroUserId, "Supermercado", null, now, now);
        when(repository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.save(any(Payee.class))).thenAnswer(inv -> inv.getArgument(0));
        AtualizarPayeeComando comando = new AtualizarPayeeComando(existente.getId(), "Mercado Extra", null);

        Payee resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Mercado Extra");
        assertThat(resultado.getUserId()).isEqualTo(outroUserId);
    }

    @Test
    void executarMantendoNomeExistenteQuandoNomeNulo() {
        Payee existente = payeeExistente("Farmacia", null);
        Payee atualizado = payeeExistente("Farmacia", null);
        when(repository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.save(any(Payee.class))).thenReturn(atualizado);
        AtualizarPayeeComando comando = new AtualizarPayeeComando(existente.getId(), null, null);

        Payee resultado = useCase.executar(comando);

        assertThat(resultado.getNome()).isEqualTo("Farmacia");
    }

    private Payee payeeExistente(String nome, UUID categoriaId) {
        Instant now = Instant.now();
        return new Payee(UUID.randomUUID(), USER_ID, nome, categoriaId, now, now);
    }
}
