package com.laboratorio.financas.transacao.infrastructure.batch;

import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.infrastructure.batch.ImportacaoCsvTransacoesItemReader.TransacaoCsvLinha;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuracao do job Spring Batch de importacao de CSV de transacoes.
 *
 * <p>Define o {@link Job} e o {@link Step} chunk-oriented. O Step e fault-tolerant:
 * itens que lancam excecao sao pulados ate o limite configurado, evitando que
 * uma linha invalida aborte a importacao inteira.
 *
 * <p>Spring Boot 3.x / Spring Batch 5.x ativa o {@code @EnableBatchProcessing}
 * automaticamente quando o starter esta no classpath -- a anotacao explicita
 * desativaria a auto-configuracao, por isso nao e usada aqui.
 */
@Configuration
public class ImportacaoCsvTransacoesJobConfig {

    private static final String JOB_NAME = "importacaoCsvTransacoesJob";
    private static final String STEP_NAME = "importacaoCsvTransacoesStep";
    private static final int SKIP_LIMIT = 10;

    @Value("${batch.importacao-csv-transacoes.chunk-size:100}")
    private int chunkSize;

    /**
     * Define o Job de importacao.
     *
     * @param jobRepository repositorio de metadados do Spring Batch
     * @param importacaoCsvTransacoesStep step de importacao
     * @param listener listener de log de inicio/fim
     * @return job configurado
     */
    @Bean
    public Job importacaoCsvTransacoesJob(
            JobRepository jobRepository,
            Step importacaoCsvTransacoesStep,
            ImportacaoCsvTransacoesJobListener listener
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(listener)
                .start(importacaoCsvTransacoesStep)
                .build();
    }

    /**
     * Reader step-scoped que le o CSV cujo caminho chega como JobParameter.
     *
     * <p>O escopo de Step permite resolver {@code jobParameters[caminhoArquivo]}
     * tardiamente -- so quando o Step inicia, e nao na criacao do contexto.
     *
     * @param caminhoArquivo caminho absoluto do CSV no disco, passado como JobParameter
     * @return FlatFileItemReader configurado para o arquivo
     */
    @Bean
    @StepScope
    public FlatFileItemReader<TransacaoCsvLinha> importacaoCsvTransacoesItemReader(
            @Value("#{jobParameters['caminhoArquivo']}") String caminhoArquivo
    ) {
        return ImportacaoCsvTransacoesItemReader.build(new FileSystemResource(caminhoArquivo));
    }

    /**
     * Define o Step chunk-oriented de importacao.
     *
     * @param jobRepository repositorio de metadados do Spring Batch
     * @param transactionManager gerenciador de transacao do chunk
     * @param reader leitor do CSV
     * @param processor validador/transformador de linha
     * @param writer persistidor de transacoes
     * @return step configurado
     */
    @Bean
    public Step importacaoCsvTransacoesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<TransacaoCsvLinha> reader,
            ItemProcessor<TransacaoCsvLinha, Transacao> processor,
            ItemWriter<Transacao> writer
    ) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<TransacaoCsvLinha, Transacao>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(SKIP_LIMIT)
                .build();
    }
}
