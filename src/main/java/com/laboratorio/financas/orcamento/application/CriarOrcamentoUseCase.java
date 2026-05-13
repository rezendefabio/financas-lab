package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;

    public CriarOrcamentoUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    public record Comando(UUID categoriaId, BigDecimal valorLimiteValor, String valorLimiteMoeda, LocalDate mesAno) { }

    @Transactional
    public Orcamento executar(Comando comando) {
        Money valorLimite = new Money(comando.valorLimiteValor(), Currency.getInstance(comando.valorLimiteMoeda()));
        Orcamento orcamento = new Orcamento(comando.categoriaId(), valorLimite, comando.mesAno());
        return orcamentoRepository.salvar(orcamento);
    }
}
