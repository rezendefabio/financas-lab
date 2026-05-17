package com.laboratorio.financas.transacao.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.infrastructure.batch.ImportacaoCsvTransacoesItemReader.TransacaoCsvLinha;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportacaoCsvTransacoesItemProcessorTest {

    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();

    private ImportacaoCsvTransacoesItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ImportacaoCsvTransacoesItemProcessor();
    }

    @Test
    void converteLinhaValidaEmTransacao() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "RECEITA",
                "150.75",
                "2025-01-15",
                "Salario",
                CONTA_ID.toString(),
                CATEGORIA_ID.toString()
        );

        Transacao transacao = processor.process(linha);

        assertThat(transacao).isNotNull();
        assertThat(transacao.getTipo()).isEqualTo(TipoTransacao.RECEITA);
        assertThat(transacao.getValor().valor()).isEqualByComparingTo(new BigDecimal("150.75"));
        assertThat(transacao.getData()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(transacao.getDescricao()).isEqualTo("Salario");
        assertThat(transacao.getContaId()).isEqualTo(CONTA_ID);
        assertThat(transacao.getCategoriaId()).isEqualTo(CATEGORIA_ID);
    }

    @Test
    void aceitaLinhaSemCategoria() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "DESPESA",
                "40.00",
                "2025-02-01",
                "Mercado",
                CONTA_ID.toString(),
                ""
        );

        Transacao transacao = processor.process(linha);

        assertThat(transacao).isNotNull();
        assertThat(transacao.getCategoriaId()).isNull();
    }

    @Test
    void aceitaTipoEmMinusculasComEspacos() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "  receita  ",
                "10.00",
                "2025-03-01",
                "Juros",
                CONTA_ID.toString(),
                null
        );

        Transacao transacao = processor.process(linha);

        assertThat(transacao).isNotNull();
        assertThat(transacao.getTipo()).isEqualTo(TipoTransacao.RECEITA);
    }

    @Test
    void retornaNullParaTipoInvalido() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "INEXISTENTE",
                "10.00",
                "2025-01-01",
                "X",
                CONTA_ID.toString(),
                null
        );

        assertThat(processor.process(linha)).isNull();
    }

    @Test
    void retornaNullParaValorNaoNumerico() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "RECEITA",
                "abc",
                "2025-01-01",
                "X",
                CONTA_ID.toString(),
                null
        );

        assertThat(processor.process(linha)).isNull();
    }

    @Test
    void retornaNullParaDataInvalida() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "RECEITA",
                "10.00",
                "data-errada",
                "X",
                CONTA_ID.toString(),
                null
        );

        assertThat(processor.process(linha)).isNull();
    }

    @Test
    void retornaNullParaContaIdInvalido() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "RECEITA",
                "10.00",
                "2025-01-01",
                "X",
                "nao-e-uuid",
                null
        );

        assertThat(processor.process(linha)).isNull();
    }

    @Test
    void retornaNullParaValorNegativo() {
        TransacaoCsvLinha linha = new TransacaoCsvLinha(
                "RECEITA",
                "-5.00",
                "2025-01-01",
                "X",
                CONTA_ID.toString(),
                null
        );

        assertThat(processor.process(linha)).isNull();
    }
}
