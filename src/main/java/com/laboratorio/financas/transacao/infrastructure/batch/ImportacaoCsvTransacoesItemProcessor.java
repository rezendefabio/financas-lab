package com.laboratorio.financas.transacao.infrastructure.batch;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.infrastructure.batch.ImportacaoCsvTransacoesItemReader.TransacaoCsvLinha;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Currency;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * ItemProcessor do job de importacao de CSV de transacoes.
 *
 * <p>Valida e transforma cada {@link TransacaoCsvLinha} em uma {@link Transacao}
 * de dominio. Itens invalidos retornam {@code null} -- o Spring Batch os filtra
 * automaticamente do fluxo e contabiliza como filtered.
 */
@Component
public class ImportacaoCsvTransacoesItemProcessor
        implements ItemProcessor<TransacaoCsvLinha, Transacao> {

    private static final Logger LOG =
            LoggerFactory.getLogger(ImportacaoCsvTransacoesItemProcessor.class);

    private static final Currency BRL = Currency.getInstance("BRL");

    /**
     * Converte uma linha do CSV em Transacao de dominio.
     *
     * @param linha linha bruta lida do CSV
     * @return Transacao valida, ou {@code null} se a linha for invalida
     */
    @Override
    public Transacao process(TransacaoCsvLinha linha) {
        try {
            // TODO: substituir pelas regras reais de validacao e enriquecimento.
            // O userId provavelmente vira de JobParameters, nao do CSV.
            TipoTransacao tipo = TipoTransacao.valueOf(linha.tipo().trim().toUpperCase());
            Money valor = new Money(new BigDecimal(linha.valor().trim()), BRL);
            LocalDate data = LocalDate.parse(linha.data().trim());
            String descricao = linha.descricao() != null ? linha.descricao().trim() : "";
            UUID contaId = UUID.fromString(linha.contaId().trim());
            UUID categoriaId = linha.categoriaId() != null && !linha.categoriaId().isBlank()
                    ? UUID.fromString(linha.categoriaId().trim())
                    : null;

            return new Transacao(
                    tipo,
                    valor,
                    data,
                    descricao,
                    contaId,
                    categoriaId,
                    null,
                    StatusTransacao.CLEARED,
                    null,
                    Collections.emptyList()
            );
        } catch (RuntimeException e) {
            LOG.warn("Linha de CSV invalida ignorada: {} -- motivo: {}", linha, e.getMessage());
            return null;
        }
    }
}
