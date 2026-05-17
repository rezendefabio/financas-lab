package com.laboratorio.financas.transacao.infrastructure.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

/**
 * JobExecutionListener do job de importacao de CSV de transacoes.
 *
 * <p>Loga inicio e conclusao do job, incluindo contadores agregados de
 * leitura, escrita e skip somados sobre todos os steps.
 */
@Component
public class ImportacaoCsvTransacoesJobListener implements JobExecutionListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(ImportacaoCsvTransacoesJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOG.info(
                "Job ImportacaoCsvTransacoes iniciado. id={} parametros={}",
                jobExecution.getId(),
                jobExecution.getJobParameters()
        );
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long lidos = 0;
        long escritos = 0;
        long pulados = 0;
        for (StepExecution step : jobExecution.getStepExecutions()) {
            lidos += step.getReadCount();
            escritos += step.getWriteCount();
            pulados += step.getReadSkipCount()
                    + step.getProcessSkipCount()
                    + step.getWriteSkipCount();
        }
        LOG.info(
                "Job ImportacaoCsvTransacoes concluido. id={} status={} lidos={} escritos={} pulados={}",
                jobExecution.getId(),
                jobExecution.getStatus(),
                lidos,
                escritos,
                pulados
        );
    }
}
