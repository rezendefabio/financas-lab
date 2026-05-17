package com.laboratorio.financas.transacao.infrastructure.batch;

import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * ItemWriter do job de importacao de CSV de transacoes.
 *
 * <p>Persiste cada chunk de transacoes validas via {@link TransacaoRepository}.
 * O Repository do bounded context nao expoe {@code saveAll}, entao o writer
 * itera o chunk e chama {@code salvar} item a item dentro da transacao do Step.
 */
@Component
public class ImportacaoCsvTransacoesItemWriter implements ItemWriter<Transacao> {

    private static final Logger LOG =
            LoggerFactory.getLogger(ImportacaoCsvTransacoesItemWriter.class);

    private final TransacaoRepository transacaoRepository;

    public ImportacaoCsvTransacoesItemWriter(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    /**
     * Persiste um chunk de transacoes.
     *
     * @param chunk transacoes validas produzidas pelo processor
     */
    @Override
    public void write(Chunk<? extends Transacao> chunk) {
        for (Transacao transacao : chunk) {
            transacaoRepository.salvar(transacao);
        }
        LOG.info("Chunk de {} transacoes persistido.", chunk.size());
    }
}
