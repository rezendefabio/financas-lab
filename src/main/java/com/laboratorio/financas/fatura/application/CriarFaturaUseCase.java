package com.laboratorio.financas.fatura.application;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarFaturaUseCase {

    private final FaturaRepository repository;

    public CriarFaturaUseCase(FaturaRepository repository) {
        this.repository = repository;
    }

    public record Comando(
            UUID userId,
            UUID contaId,
            String nome,
            LocalDate dataVencimento,
            LocalDate dataFechamento,
            BigDecimal valorTotalValor,
            String valorTotalMoeda
    ) { }

    @Transactional
    public Fatura executar(Comando comando) {
        Money valorTotal = null;
        if (comando.valorTotalValor() != null) {
            valorTotal = new Money(
                    comando.valorTotalValor(),
                    Currency.getInstance(comando.valorTotalMoeda())
            );
        }
        Fatura fatura = new Fatura(
                comando.userId(),
                comando.contaId(),
                comando.nome(),
                comando.dataVencimento(),
                comando.dataFechamento(),
                valorTotal
        );
        return repository.salvar(fatura);
    }
}
