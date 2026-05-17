package com.laboratorio.financas.transacao.infrastructure.batch;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.Resource;

/**
 * ItemReader do job de importacao de CSV de transacoes.
 *
 * <p>Le um arquivo CSV linha a linha e mapeia cada linha para um
 * {@link TransacaoCsvLinha}. O arquivo CSV chega via endpoint REST como
 * MultipartFile e e gravado em um Resource temporario antes da execucao do job.
 */
public final class ImportacaoCsvTransacoesItemReader {

    private ImportacaoCsvTransacoesItemReader() {
        // Factory de FlatFileItemReader -- nao instanciar.
    }

    /**
     * Constroi o FlatFileItemReader para o arquivo CSV informado.
     *
     * <p>Layout esperado do CSV (com header na primeira linha):
     * {@code tipo;valor;data;descricao;contaId;categoriaId}
     *
     * @param recurso arquivo CSV a ser lido
     * @return reader configurado
     */
    public static FlatFileItemReader<TransacaoCsvLinha> build(Resource recurso) {
        return new FlatFileItemReaderBuilder<TransacaoCsvLinha>()
                .name("importacaoCsvTransacoesItemReader")
                .resource(recurso)
                .linesToSkip(1)
                .delimited()
                .delimiter(";")
                .names("tipo", "valor", "data", "descricao", "contaId", "categoriaId")
                .targetType(TransacaoCsvLinha.class)
                .build();
    }

    /**
     * Representa uma linha bruta do CSV antes de validacao.
     *
     * <p>TODO: ajustar os campos conforme o layout real do CSV de importacao
     * definido pelo negocio.
     */
    public record TransacaoCsvLinha(
            String tipo,
            String valor,
            String data,
            String descricao,
            String contaId,
            String categoriaId
    ) {
    }
}
