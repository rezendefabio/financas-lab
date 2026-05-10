package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CalcularSaldoDaContaUseCase {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public CalcularSaldoDaContaUseCase(
            ContaRepository contaRepository,
            TransacaoRepository transacaoRepository
    ) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public record Resultado(
            UUID contaId,
            Money saldoInicial,
            Money totalReceitas,
            Money totalDespesas,
            Money totalTransferenciasEnviadas,
            Money totalTransferenciasRecebidas,
            Money saldoAtual,
            Instant calculadoEm
    ) { }

    @Transactional(readOnly = true)
    public Resultado executar(UUID contaId) {
        Conta conta = contaRepository.buscarPorId(contaId)
                .orElseThrow(() -> new ContaNaoEncontradaException(contaId));

        TotaisTransacaoPorConta totais = transacaoRepository.calcularTotaisPorConta(contaId);

        Currency moeda = conta.getSaldoInicial().moeda();
        Money saldoInicial = conta.getSaldoInicial();
        Money totalReceitas = new Money(totais.totalReceitas(), moeda);
        Money totalDespesas = new Money(totais.totalDespesas(), moeda);
        Money totalTransferenciasEnviadas = new Money(totais.totalTransferenciasEnviadas(), moeda);
        Money totalTransferenciasRecebidas = new Money(totais.totalTransferenciasRecebidas(), moeda);

        BigDecimal saldoAtualValor = saldoInicial.valor()
                .add(totais.totalReceitas())
                .subtract(totais.totalDespesas())
                .subtract(totais.totalTransferenciasEnviadas())
                .add(totais.totalTransferenciasRecebidas());
        Money saldoAtual = new Money(saldoAtualValor, moeda);

        return new Resultado(
                contaId,
                saldoInicial,
                totalReceitas,
                totalDespesas,
                totalTransferenciasEnviadas,
                totalTransferenciasRecebidas,
                saldoAtual,
                Instant.now()
        );
    }
}
