package com.laboratorio.financas.transacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoComReferenciaInvalidaException;
import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EditarTransacaoUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2025, 1, 15);

    private TransacaoRepository transacaoRepository;
    private ContaRepository contaRepository;
    private CategoriaRepository categoriaRepository;
    private EditarTransacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        contaRepository = Mockito.mock(ContaRepository.class);
        categoriaRepository = Mockito.mock(CategoriaRepository.class);
        useCase = new EditarTransacaoUseCase(transacaoRepository, contaRepository, categoriaRepository);
    }

    private Conta contaValida() {
        return new Conta("Corrente", TipoConta.CORRENTE, new Money(BigDecimal.ZERO, BRL));
    }

    private Transacao transacaoExistente(UUID id, UUID contaId) {
        return new Transacao(
                id,
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(100), BRL),
                DATA,
                "Salario antigo",
                contaId,
                null,
                null,
                Instant.parse("2025-01-01T10:00:00Z"),
                null
        );
    }

    @Test
    void executarCaminhoFelizRetornaTransacaoAtualizada() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao existente = transacaoExistente(id, contaId);
        Transacao atualizada = new Transacao(
                id,
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(150), BRL),
                DATA,
                "Salario atualizado",
                contaId,
                null,
                null,
                existente.getCriadoEm(),
                Instant.now()
        );
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(atualizada);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(150), "BRL",
                DATA, "Salario atualizado", contaId, null, null
        );

        // When
        Transacao resultado = useCase.executar(id, comando);

        // Then
        assertThat(resultado).isNotNull();
        verify(transacaoRepository, times(1)).salvar(any(Transacao.class));
    }

    @Test
    void executarLancaTransacaoNaoEncontradaExceptionQuandoIdNaoExiste() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id, comando))
                .isInstanceOf(TransacaoNaoEncontradaException.class)
                .satisfies(ex -> {
                    TransacaoNaoEncontradaException tnee = (TransacaoNaoEncontradaException) ex;
                    assertThat(tnee.getId()).isEqualTo(id);
                });
    }

    @Test
    void executarComContaIdInexistenteLancaReferenciaInvalidaException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        Transacao existente = transacaoExistente(id, contaId);
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id, comando))
                .isInstanceOf(TransacaoComReferenciaInvalidaException.class)
                .satisfies(ex -> {
                    TransacaoComReferenciaInvalidaException e =
                            (TransacaoComReferenciaInvalidaException) ex;
                    assertThat(e.getRecurso()).isEqualTo("conta");
                });
    }

    @Test
    void executarPreservaCriadoEmDaTransacaoExistente() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        Instant criadoEmOriginal = Instant.parse("2025-01-01T10:00:00Z");
        Transacao existente = new Transacao(
                id,
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(100), BRL),
                DATA,
                "Salario",
                contaId,
                null,
                null,
                criadoEmOriginal,
                null
        );
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(200), "BRL",
                DATA, "Salario editado", contaId, null, null
        );

        // When
        Transacao resultado = useCase.executar(id, comando);

        // Then
        assertThat(resultado.getCriadoEm()).isEqualTo(criadoEmOriginal);
    }

    @Test
    void executarNaoSalvaQuandoTransacaoNaoEncontrada() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When
        try {
            useCase.executar(id, comando);
        } catch (TransacaoNaoEncontradaException e) {
            // esperado
        }

        // Then
        verify(transacaoRepository, times(0)).salvar(any(Transacao.class));
    }

    @Test
    void executarPermiteAlterarTipoParaTransferencia() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        UUID contaDestinoId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao existente = transacaoExistente(id, contaId);
        Transacao atualizada = new Transacao(
                id,
                TipoTransacao.TRANSFERENCIA,
                new Money(BigDecimal.valueOf(100), BRL),
                DATA,
                "Mudou para transferencia",
                contaId,
                contaDestinoId,
                null,
                existente.getCriadoEm(),
                Instant.now()
        );
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(contaRepository.buscarPorId(contaDestinoId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(atualizada);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.TRANSFERENCIA, BigDecimal.valueOf(100), "BRL",
                DATA, "Mudou para transferencia", contaId, contaDestinoId, null
        );

        // When
        Transacao resultado = useCase.executar(id, comando);

        // Then
        assertThat(resultado.getTipo()).isEqualTo(TipoTransacao.TRANSFERENCIA);
    }

    @Test
    void executarChamaRepositorioBuscarPorIdUmaVez() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao existente = transacaoExistente(id, contaId);
        Transacao atualizada = transacaoExistente(id, contaId);
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(atualizada);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When
        useCase.executar(id, comando);

        // Then
        verify(transacaoRepository, times(1)).buscarPorId(id);
    }

    @Test
    void executarComContaDestinoInexistenteLancaReferenciaInvalidaException() {
        // Given
        UUID id = UUID.randomUUID();
        UUID contaId = UUID.randomUUID();
        UUID contaDestinoId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao existente = transacaoExistente(id, contaId);
        when(transacaoRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(contaRepository.buscarPorId(contaDestinoId)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.TRANSFERENCIA, BigDecimal.valueOf(100), "BRL",
                DATA, "Transferencia", contaId, contaDestinoId, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(id, comando))
                .isInstanceOf(TransacaoComReferenciaInvalidaException.class)
                .satisfies(ex -> {
                    TransacaoComReferenciaInvalidaException e =
                            (TransacaoComReferenciaInvalidaException) ex;
                    assertThat(e.getRecurso()).isEqualTo("contaDestino");
                });
    }
}
