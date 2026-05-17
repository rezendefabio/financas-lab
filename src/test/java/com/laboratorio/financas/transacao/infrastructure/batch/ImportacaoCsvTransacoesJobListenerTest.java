package com.laboratorio.financas.transacao.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

class ImportacaoCsvTransacoesJobListenerTest {

    private ImportacaoCsvTransacoesJobListener listener;

    @BeforeEach
    void setUp() {
        listener = new ImportacaoCsvTransacoesJobListener();
    }

    private JobExecution jobExecution() {
        JobInstance instance = new JobInstance(1L, "importacaoCsvTransacoesJob");
        return new JobExecution(instance, 1L, new JobParameters());
    }

    @Test
    void beforeJobNaoLancaExcecao() {
        assertThatCode(() -> listener.beforeJob(jobExecution()))
                .doesNotThrowAnyException();
    }

    @Test
    void afterJobAgregaContadoresDosStepsSemLancarExcecao() {
        JobExecution execution = jobExecution();
        execution.setStatus(BatchStatus.COMPLETED);

        StepExecution step = new StepExecution("importacaoCsvTransacoesStep", execution);
        step.setReadCount(100);
        step.setWriteCount(95);
        step.setReadSkipCount(2);
        step.setProcessSkipCount(1);
        step.setWriteSkipCount(2);
        execution.addStepExecutions(java.util.List.of(step));

        assertThatCode(() -> listener.afterJob(execution))
                .doesNotThrowAnyException();
    }

    @Test
    void afterJobLidaComJobSemSteps() {
        assertThatCode(() -> listener.afterJob(jobExecution()))
                .doesNotThrowAnyException();
    }
}
