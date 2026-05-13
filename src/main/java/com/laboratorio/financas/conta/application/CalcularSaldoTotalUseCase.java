package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CalcularSaldoTotalUseCase {

    private final ContaRepository contaRepository;
    private final CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase;

    public CalcularSaldoTotalUseCase(
            ContaRepository contaRepository,
            CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase
    ) {
        this.contaRepository = contaRepository;
        this.calcularSaldoDaContaUseCase = calcularSaldoDaContaUseCase;
    }

    public record Resultado(Money saldoTotal, int totalContas) { }

    @Transactional(readOnly = true)
    public Resultado executar() {
        List<Conta> contasAtivas = contaRepository.listarAtivas();
        Money total = new Money(BigDecimal.ZERO, Currency.getInstance("BRL"));
        for (Conta conta : contasAtivas) {
            CalcularSaldoDaContaUseCase.Resultado saldo =
                    calcularSaldoDaContaUseCase.executar(conta.getId());
            total = total.somar(saldo.saldoAtual());
        }
        return new Resultado(total, contasAtivas.size());
    }
}
