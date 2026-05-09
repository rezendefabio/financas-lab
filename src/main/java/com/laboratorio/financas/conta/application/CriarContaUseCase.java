package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarContaUseCase {

    private final ContaRepository repository;

    public CriarContaUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    public record Comando(
            String nome,
            TipoConta tipo,
            BigDecimal saldoInicialValor,
            String saldoInicialMoeda
    ) { }

    @Transactional
    public Conta executar(Comando comando) {
        Currency moeda = Currency.getInstance(comando.saldoInicialMoeda());
        Money saldoInicial = new Money(comando.saldoInicialValor(), moeda);
        Conta nova = new Conta(comando.nome(), comando.tipo(), saldoInicial);
        return repository.salvar(nova);
    }
}
