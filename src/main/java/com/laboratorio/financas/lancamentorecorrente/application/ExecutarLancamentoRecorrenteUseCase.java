package com.laboratorio.financas.lancamentorecorrente.application;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteNaoEncontradoException;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExecutarLancamentoRecorrenteUseCase {

    private final LancamentoRecorrenteRepository lancamentoRecorrenteRepository;
    private final TransacaoRepository transacaoRepository;

    public ExecutarLancamentoRecorrenteUseCase(
            LancamentoRecorrenteRepository lancamentoRecorrenteRepository,
            TransacaoRepository transacaoRepository
    ) {
        this.lancamentoRecorrenteRepository = lancamentoRecorrenteRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public record Resultado(
            UUID transacaoId,
            UUID lancamentoRecorrenteId,
            LocalDate dataExecutada,
            LocalDate novaProximaOcorrencia
    ) { }

    @Transactional
    public Resultado executar(UUID id) {
        LancamentoRecorrente lancamento = lancamentoRecorrenteRepository.buscarPorId(id)
                .orElseThrow(() -> new LancamentoRecorrenteNaoEncontradoException(id));

        if (!lancamento.isAtivo()) {
            throw new IllegalStateException("Lancamento recorrente inativo nao pode ser executado");
        }

        LocalDate dataExecutada = lancamento.getProximaOcorrencia();

        Transacao transacao = new Transacao(
                lancamento.getTipo(),
                lancamento.getValor(),
                lancamento.getProximaOcorrencia(),
                lancamento.getDescricao(),
                lancamento.getContaId(),
                lancamento.getCategoriaId(),
                null,
                com.laboratorio.financas.transacao.domain.StatusTransacao.CLEARED,
                null,
                java.util.List.of()
        );
        Transacao transacaoSalva = transacaoRepository.salvar(transacao);

        lancamento.avancarProximaOcorrencia();
        lancamentoRecorrenteRepository.atualizar(lancamento);

        return new Resultado(
                transacaoSalva.getId(),
                lancamento.getId(),
                dataExecutada,
                lancamento.getProximaOcorrencia()
        );
    }
}
