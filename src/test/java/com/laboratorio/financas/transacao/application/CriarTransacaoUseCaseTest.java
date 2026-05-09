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
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarTransacaoUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2025, 1, 15);

    private TransacaoRepository transacaoRepository;
    private ContaRepository contaRepository;
    private CategoriaRepository categoriaRepository;
    private CriarTransacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        contaRepository = Mockito.mock(ContaRepository.class);
        categoriaRepository = Mockito.mock(CategoriaRepository.class);
        useCase = new CriarTransacaoUseCase(transacaoRepository, contaRepository, categoriaRepository);
    }

    private Conta contaValida() {
        return new Conta("Corrente", TipoConta.CORRENTE, new Money(BigDecimal.ZERO, BRL));
    }

    private Transacao transacaoReceitaSalva(UUID contaId) {
        return new Transacao(
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(100), BRL),
                DATA,
                "Salario",
                contaId,
                null,
                null
        );
    }

    @Test
    void executarReceitaComFkValidaRetornaTransacao() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao salva = transacaoReceitaSalva(contaId);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(salva);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When
        Transacao resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipo()).isEqualTo(TipoTransacao.RECEITA);
    }

    @Test
    void executarDespesaComFkValidaRetornaTransacao() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao salva = new Transacao(
                TipoTransacao.DESPESA,
                new Money(BigDecimal.valueOf(50), BRL),
                DATA,
                "Mercado",
                contaId,
                null,
                null
        );
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(salva);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.DESPESA, BigDecimal.valueOf(50), "BRL",
                DATA, "Mercado", contaId, null, null
        );

        // When
        Transacao resultado = useCase.executar(comando);

        // Then
        assertThat(resultado.getTipo()).isEqualTo(TipoTransacao.DESPESA);
    }

    @Test
    void executarTransferenciaComFksValidasRetornaTransacao() {
        // Given
        UUID contaId = UUID.randomUUID();
        UUID contaDestinoId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao salva = new Transacao(
                TipoTransacao.TRANSFERENCIA,
                new Money(BigDecimal.valueOf(200), BRL),
                DATA,
                "Transferencia entre contas",
                contaId,
                contaDestinoId,
                null
        );
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(contaRepository.buscarPorId(contaDestinoId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(salva);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.TRANSFERENCIA, BigDecimal.valueOf(200), "BRL",
                DATA, "Transferencia entre contas", contaId, contaDestinoId, null
        );

        // When
        Transacao resultado = useCase.executar(comando);

        // Then
        assertThat(resultado.getTipo()).isEqualTo(TipoTransacao.TRANSFERENCIA);
    }

    @Test
    void executarComContaIdInexistenteLancaReferenciaInvalidaException() {
        // Given
        UUID contaId = UUID.randomUUID();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(TransacaoComReferenciaInvalidaException.class)
                .satisfies(ex -> {
                    TransacaoComReferenciaInvalidaException e =
                            (TransacaoComReferenciaInvalidaException) ex;
                    assertThat(e.getRecurso()).isEqualTo("conta");
                    assertThat(e.getId()).isEqualTo(contaId);
                });
    }

    @Test
    void executarTransferenciaComContaDestinoInexistenteLancaReferenciaInvalidaException() {
        // Given
        UUID contaId = UUID.randomUUID();
        UUID contaDestinoId = UUID.randomUUID();
        Conta conta = contaValida();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(contaRepository.buscarPorId(contaDestinoId)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.TRANSFERENCIA, BigDecimal.valueOf(200), "BRL",
                DATA, "Transferencia", contaId, contaDestinoId, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(TransacaoComReferenciaInvalidaException.class)
                .satisfies(ex -> {
                    TransacaoComReferenciaInvalidaException e =
                            (TransacaoComReferenciaInvalidaException) ex;
                    assertThat(e.getRecurso()).isEqualTo("contaDestino");
                });
    }

    @Test
    void executarComCategoriaIdInexistenteLancaReferenciaInvalidaException() {
        // Given
        UUID contaId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        Conta conta = contaValida();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.empty());

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, categoriaId
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(TransacaoComReferenciaInvalidaException.class)
                .satisfies(ex -> {
                    TransacaoComReferenciaInvalidaException e =
                            (TransacaoComReferenciaInvalidaException) ex;
                    assertThat(e.getRecurso()).isEqualTo("categoria");
                });
    }

    @Test
    void executarTransferenciaSemContaDestinoLancaIllegalArgumentException() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.TRANSFERENCIA, BigDecimal.valueOf(200), "BRL",
                DATA, "Transferencia invalida", contaId, null, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void executarCaminhoFelizChamaRepositorioSalvarUmaVez() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        Transacao salva = transacaoReceitaSalva(contaId);
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));
        when(transacaoRepository.salvar(any(Transacao.class))).thenReturn(salva);

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When
        useCase.executar(comando);

        // Then
        verify(transacaoRepository, times(1)).salvar(any(Transacao.class));
    }

    @Test
    void executarComValorZeroLancaIllegalArgumentException() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.ZERO, "BRL",
                DATA, "Salario", contaId, null, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void executarComMoedaInvalidaLancaIllegalArgumentException() {
        // Given
        UUID contaId = UUID.randomUUID();
        Conta conta = contaValida();
        when(contaRepository.buscarPorId(contaId)).thenReturn(Optional.of(conta));

        CriarTransacaoUseCase.Comando comando = new CriarTransacaoUseCase.Comando(
                TipoTransacao.RECEITA, BigDecimal.valueOf(100), "XYZ",
                DATA, "Salario", contaId, null, null
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
