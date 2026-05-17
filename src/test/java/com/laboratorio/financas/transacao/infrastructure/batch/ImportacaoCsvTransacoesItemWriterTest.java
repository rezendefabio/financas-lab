package com.laboratorio.financas.transacao.infrastructure.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.Chunk;

class ImportacaoCsvTransacoesItemWriterTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private TransacaoRepository transacaoRepository;
    private ImportacaoCsvTransacoesItemWriter writer;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        writer = new ImportacaoCsvTransacoesItemWriter(transacaoRepository);
    }

    private Transacao transacao() {
        return new Transacao(
                TipoTransacao.RECEITA,
                new Money(BigDecimal.valueOf(100), BRL),
                LocalDate.of(2025, 1, 1),
                "Teste",
                UUID.randomUUID(),
                null,
                null,
                StatusTransacao.CLEARED,
                null,
                Collections.emptyList()
        );
    }

    @Test
    void persisteCadaTransacaoDoChunk() {
        Chunk<Transacao> chunk = new Chunk<>(List.of(transacao(), transacao(), transacao()));

        writer.write(chunk);

        verify(transacaoRepository, times(3)).salvar(any(Transacao.class));
    }

    @Test
    void naoChamaRepositorioParaChunkVazio() {
        writer.write(new Chunk<>(Collections.emptyList()));

        verifyNoInteractions(transacaoRepository);
    }
}
