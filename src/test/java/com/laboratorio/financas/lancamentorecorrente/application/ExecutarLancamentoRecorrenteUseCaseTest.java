package com.laboratorio.financas.lancamentorecorrente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteNaoEncontradoException;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExecutarLancamentoRecorrenteUseCaseTest {

    private LancamentoRecorrenteRepository lancamentoRepository;
    private TransacaoRepository transacaoRepository;
    private ExecutarLancamentoRecorrenteUseCase useCase;

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Money VALOR_200 = new Money(new BigDecimal("200.00"), BRL);

    @BeforeEach
    void setUp() {
        lancamentoRepository = Mockito.mock(LancamentoRecorrenteRepository.class);
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        useCase = new ExecutarLancamentoRecorrenteUseCase(lancamentoRepository, transacaoRepository);
    }

    private LancamentoRecorrente lancamentoAtivo(LocalDate proxima) {
        return new LancamentoRecorrente(
                "Aluguel", TipoTransacao.DESPESA, VALOR_200,
                UUID.randomUUID(), null, Periodicidade.MENSAL, proxima);
    }

    private Transacao transacaoSalva() {
        return new Transacao(
                TipoTransacao.DESPESA, VALOR_200, LocalDate.now(),
                "Aluguel", UUID.randomUUID(), null, null);
    }

    @Test
    @DisplayName("executar: cria transacao e avanca proxima ocorrencia")
    void executarCaminhoFeliz() {
        UUID id = UUID.randomUUID();
        LocalDate proxima = LocalDate.of(2026, 6, 1);
        LancamentoRecorrente lancamento = lancamentoAtivo(proxima);
        Transacao transacao = transacaoSalva();
        when(lancamentoRepository.buscarPorId(id)).thenReturn(Optional.of(lancamento));
        when(transacaoRepository.salvar(any())).thenReturn(transacao);
        when(lancamentoRepository.atualizar(any())).thenReturn(lancamento);

        ExecutarLancamentoRecorrenteUseCase.Resultado resultado = useCase.executar(id);

        assertThat(resultado.transacaoId()).isEqualTo(transacao.getId());
        assertThat(resultado.lancamentoRecorrenteId()).isEqualTo(lancamento.getId());
        assertThat(resultado.dataExecutada()).isEqualTo(proxima);
        assertThat(resultado.novaProximaOcorrencia()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("executar: salva transacao no transacaoRepository")
    void executarSalvaTransacao() {
        UUID id = UUID.randomUUID();
        LancamentoRecorrente lancamento = lancamentoAtivo(LocalDate.now());
        when(lancamentoRepository.buscarPorId(id)).thenReturn(Optional.of(lancamento));
        when(transacaoRepository.salvar(any())).thenReturn(transacaoSalva());
        when(lancamentoRepository.atualizar(any())).thenReturn(lancamento);

        useCase.executar(id);

        verify(transacaoRepository).salvar(any(Transacao.class));
    }

    @Test
    @DisplayName("executar: atualiza lancamento apos execucao")
    void executarAtualizaLancamento() {
        UUID id = UUID.randomUUID();
        LancamentoRecorrente lancamento = lancamentoAtivo(LocalDate.now());
        when(lancamentoRepository.buscarPorId(id)).thenReturn(Optional.of(lancamento));
        when(transacaoRepository.salvar(any())).thenReturn(transacaoSalva());
        when(lancamentoRepository.atualizar(any())).thenReturn(lancamento);

        useCase.executar(id);

        verify(lancamentoRepository).atualizar(lancamento);
    }

    @Test
    @DisplayName("executar: lanca LancamentoRecorrenteNaoEncontradoException quando ausente")
    void executarLancaExceptionQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(lancamentoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(LancamentoRecorrenteNaoEncontradoException.class);
    }

    @Test
    @DisplayName("executar: lanca IllegalStateException quando lancamento inativo")
    void executarLancaExceptionQuandoInativo() {
        UUID id = UUID.randomUUID();
        LancamentoRecorrente lancamento = lancamentoAtivo(LocalDate.now());
        lancamento.desativar();
        when(lancamentoRepository.buscarPorId(id)).thenReturn(Optional.of(lancamento));

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inativo");
    }
}
