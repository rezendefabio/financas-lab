package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
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
            String saldoInicialMoeda,
            UUID userId,
            BigDecimal limiteCreditoValor,
            String limiteCreditoMoeda,
            Integer diaFechamento,
            Integer diaVencimento
    ) { }

    @Transactional
    public Conta executar(Comando comando) {
        Currency moeda = Currency.getInstance(comando.saldoInicialMoeda());
        Money saldoInicial = new Money(comando.saldoInicialValor(), moeda);

        Money limiteCredito = null;
        if (comando.limiteCreditoValor() != null && comando.limiteCreditoMoeda() != null) {
            Currency moedaLimite = Currency.getInstance(comando.limiteCreditoMoeda());
            limiteCredito = new Money(comando.limiteCreditoValor(), moedaLimite);
        }

        Conta nova = new Conta(
                UUID.randomUUID(),
                comando.userId(),
                comando.nome(),
                comando.tipo(),
                saldoInicial,
                saldoInicial,
                limiteCredito,
                comando.diaFechamento(),
                comando.diaVencimento(),
                true,
                java.time.Instant.now(),
                null
        );
        return repository.salvar(nova);
    }
}
