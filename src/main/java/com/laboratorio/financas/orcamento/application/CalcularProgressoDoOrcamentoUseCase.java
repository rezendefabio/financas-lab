package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoNaoEncontradoException;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.orcamento.domain.StatusProgresso;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CalcularProgressoDoOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;
    private final TransacaoRepository transacaoRepository;

    public CalcularProgressoDoOrcamentoUseCase(OrcamentoRepository orcamentoRepository,
                                               TransacaoRepository transacaoRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public record Resultado(
            UUID orcamentoId,
            UUID categoriaId,
            LocalDate mesAno,
            Money valorLimite,
            Money totalGasto,
            BigDecimal percentualUtilizado,
            StatusProgresso status
    ) { }

    @Transactional(readOnly = true)
    public Resultado executar(UUID orcamentoId) {
        Orcamento orcamento = orcamentoRepository.buscarPorId(orcamentoId)
                .orElseThrow(() -> new OrcamentoNaoEncontradoException(orcamentoId));

        FiltrosTransacao filtros = new FiltrosTransacao(
                null,
                orcamento.getMesAno(),
                orcamento.getMesAno().plusMonths(1).minusDays(1),
                TipoTransacao.DESPESA,
                orcamento.getCategoriaId()
        );

        var transacoes = transacaoRepository.listarComFiltros(filtros, Pageable.unpaged()).getContent();

        BigDecimal totalGasto = transacoes.stream()
                .map(t -> t.getValor().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (orcamento.getValorLimite().valor().compareTo(BigDecimal.ZERO) == 0) {
            return new Resultado(
                    orcamento.getId(),
                    orcamento.getCategoriaId(),
                    orcamento.getMesAno(),
                    orcamento.getValorLimite(),
                    new Money(totalGasto, orcamento.getValorLimite().moeda()),
                    BigDecimal.ZERO,
                    StatusProgresso.EXCEDIDO
            );
        }

        BigDecimal percentual = totalGasto
                .multiply(BigDecimal.valueOf(100))
                .divide(orcamento.getValorLimite().valor(), 2, RoundingMode.HALF_UP);

        StatusProgresso status;
        if (percentual.compareTo(BigDecimal.valueOf(100)) > 0) {
            status = StatusProgresso.EXCEDIDO;
        } else if (percentual.compareTo(BigDecimal.valueOf(100)) == 0) {
            status = StatusProgresso.ATINGIDO;
        } else if (percentual.compareTo(BigDecimal.valueOf(80)) >= 0) {
            status = StatusProgresso.ATENCAO;
        } else {
            status = StatusProgresso.ABAIXO;
        }

        return new Resultado(
                orcamento.getId(),
                orcamento.getCategoriaId(),
                orcamento.getMesAno(),
                orcamento.getValorLimite(),
                new Money(totalGasto, orcamento.getValorLimite().moeda()),
                percentual,
                status
        );
    }
}
