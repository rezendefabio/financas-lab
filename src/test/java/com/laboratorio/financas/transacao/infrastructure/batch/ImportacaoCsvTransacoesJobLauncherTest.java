package com.laboratorio.financas.transacao.infrastructure.batch;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoJpaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Teste E2E do endpoint de disparo do job Spring Batch de importacao de CSV.
 *
 * <p>Exercita o {@code ImportacaoCsvTransacoesJobLauncher} (POST/GET) e, de forma
 * transitiva, o {@code ImportacaoCsvTransacoesJobConfig} (Job, Step e reader
 * step-scoped) executando o job ponta a ponta contra o PostgreSQL do Testcontainers.
 */
class ImportacaoCsvTransacoesJobLauncherTest extends AbstractAuthenticatedIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private ContaRepositoryImpl contaRepositoryImpl;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    @Autowired
    private TransacaoJpaRepository transacaoJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void limpar() {
        transacaoJpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
    }

    private UUID criarContaPersistida() {
        Conta conta = new Conta(
                "Conta " + UUID.randomUUID().toString().substring(0, 8),
                TipoConta.CORRENTE,
                new Money(BigDecimal.ZERO, BRL)
        );
        contaRepositoryImpl.salvar(conta);
        return conta.getId();
    }

    @Test
    void disparaJobImportaCsvEPersisteTransacoes() throws Exception {
        UUID contaId = criarContaPersistida();
        String csv = "tipo;valor;data;descricao;contaId;categoriaId\n"
                + "RECEITA;100.00;2025-01-10;Salario;" + contaId + ";\n"
                + "DESPESA;45.50;2025-01-12;Mercado;" + contaId + ";\n";
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "transacoes.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(
                        comAuth(multipart("/api/jobs/importacao-csv-transacoes").file(arquivo)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobExecutionId").isNumber())
                .andReturn();

        long jobExecutionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("jobExecutionId").asLong();

        mockMvc.perform(comAuth(get("/api/jobs/importacao-csv-transacoes/" + jobExecutionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void statusDeJobExecutionInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/jobs/importacao-csv-transacoes/999999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadModelo_retornaArquivoCsvComCabecalho() throws Exception {
        mockMvc.perform(comAuth(get("/api/jobs/importacao-csv-transacoes/csv/modelo")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("modelo-importacao-transacoes.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("tipo;valor;data")));
    }
}
