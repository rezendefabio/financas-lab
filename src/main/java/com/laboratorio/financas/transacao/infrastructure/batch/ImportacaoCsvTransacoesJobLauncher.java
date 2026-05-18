package com.laboratorio.financas.transacao.infrastructure.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint REST de disparo do job de importacao de CSV de transacoes.
 *
 * <p>{@code POST /api/jobs/importacao-csv-transacoes} recebe o arquivo CSV,
 * grava-o em arquivo temporario, monta os {@link JobParameters} e dispara o job
 * de forma assincrona retornando {@code 202 Accepted} com o id da execucao.
 *
 * <p>{@code GET /api/jobs/importacao-csv-transacoes/{jobExecutionId}} consulta
 * o status de uma execucao via {@link JobExplorer}.
 */
@RestController
@RequestMapping("/api/jobs/importacao-csv-transacoes")
public class ImportacaoCsvTransacoesJobLauncher {

    private static final Logger LOG =
            LoggerFactory.getLogger(ImportacaoCsvTransacoesJobLauncher.class);

    private final JobLauncher jobLauncher;
    private final Job importacaoCsvTransacoesJob;
    private final JobExplorer jobExplorer;

    public ImportacaoCsvTransacoesJobLauncher(
            JobLauncher jobLauncher,
            Job importacaoCsvTransacoesJob,
            JobExplorer jobExplorer
    ) {
        this.jobLauncher = jobLauncher;
        this.importacaoCsvTransacoesJob = importacaoCsvTransacoesJob;
        this.jobExplorer = jobExplorer;
    }

    /**
     * Dispara o job de importacao a partir de um arquivo CSV enviado.
     *
     * @param arquivo arquivo CSV com as transacoes
     * @return 202 Accepted com o id da execucao do job
     * @throws Exception se o arquivo nao puder ser persistido ou o job nao puder iniciar
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> disparar(
            @RequestParam("arquivo") MultipartFile arquivo
    ) throws Exception {
        Path temporario = persistirTemporario(arquivo);

        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            nomeOriginal = "desconhecido.csv";
        }

        // timestamp garante JobParameters unicos -- evita "JobInstance already exists".
        JobParameters parametros = new JobParametersBuilder()
                .addString("caminhoArquivo", temporario.toAbsolutePath().toString())
                .addString("nomeArquivoOriginal", nomeOriginal)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(importacaoCsvTransacoesJob, parametros);
        LOG.info("Job de importacao disparado. jobExecutionId={}", execution.getId());

        return ResponseEntity.accepted().body(Map.of(
                "jobExecutionId", execution.getId(),
                "status", execution.getStatus().toString()
        ));
    }

    /**
     * Consulta o status de uma execucao do job.
     *
     * @param jobExecutionId id da execucao
     * @return status atual da execucao, ou 404 se nao existir
     */
    @GetMapping("/{jobExecutionId}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "jobExecutionId", execution.getId(),
                "status", execution.getStatus().toString(),
                "exitCode", execution.getExitStatus().getExitCode()
        ));
    }

    /**
     * Entrega o arquivo CSV modelo para download.
     *
     * <p>O layout (delimitador {@code ;}, 6 colunas) corresponde exatamente ao
     * formato esperado pelo {@code ImportacaoCsvTransacoesItemReader}. A linha
     * {@code RECEITA} termina sem {@code categoriaId} de proposito -- demonstra
     * que o campo e opcional.
     *
     * @return 200 OK com o conteudo do CSV como anexo
     */
    @GetMapping("/csv/modelo")
    public ResponseEntity<byte[]> downloadModelo() {
        String csv = """
                tipo;valor;data;descricao;contaId;categoriaId
                DESPESA;50.00;2026-05-01;Mercado;00000000-0000-0000-0000-000000000001;00000000-0000-0000-0000-000000000002
                RECEITA;3000.00;2026-05-05;Salario;00000000-0000-0000-0000-000000000001;
                """;
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"modelo-importacao-transacoes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    private Path persistirTemporario(MultipartFile arquivo) throws IOException {
        Path temporario = Files.createTempFile("importacao-csv-transacoes-", ".csv");
        arquivo.transferTo(temporario);
        return temporario;
    }
}
