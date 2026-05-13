package com.laboratorio.financas.lancamentorecorrente.application;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import com.laboratorio.financas.lancamentorecorrente.domain.Periodicidade;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CriarLancamentoRecorrenteUseCase {

    private final LancamentoRecorrenteRepository repository;

    public CriarLancamentoRecorrenteUseCase(LancamentoRecorrenteRepository repository) {
        this.repository = repository;
    }

    public record Comando(
            String descricao,
            TipoTransacao tipo,
            BigDecimal valorValor,
            String valorMoeda,
            UUID contaId,
            UUID categoriaId,
            Periodicidade periodicidade,
            LocalDate proximaOcorrencia
    ) { }

    public LancamentoRecorrente executar(Comando comando) {
        Money valor = new Money(comando.valorValor(), Currency.getInstance(comando.valorMoeda()));
        LancamentoRecorrente lancamento = new LancamentoRecorrente(
                comando.descricao(),
                comando.tipo(),
                valor,
                comando.contaId(),
                comando.categoriaId(),
                comando.periodicidade(),
                comando.proximaOcorrencia()
        );
        return repository.salvar(lancamento);
    }
}
