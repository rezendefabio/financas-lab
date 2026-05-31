package com.laboratorio.financas.lancamentorecorrente.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.lancamentorecorrente.infrastructure.persistence.LancamentoRecorrenteJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class LancamentoRecorrenteControllerTest extends AbstractAuthenticatedIntegrationTest {

    private static final String PROXIMA = LocalDate.now().plusMonths(1).toString();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LancamentoRecorrenteJpaRepository lancamentoJpaRepository;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    @Autowired
    private ContaRepositoryImpl contaRepositoryImpl;

    @Autowired
    private TransacaoJpaRepository transacaoJpaRepository;

    @AfterEach
    void limpar() {
        transacaoJpaRepository.deleteAll();
        lancamentoJpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
    }

    private UUID criarContaPersistida() {
        Money saldo = new Money(BigDecimal.ZERO, Currency.getInstance("BRL"));
        Conta conta = new Conta(
                UUID.randomUUID(),
                authenticatedUserId,
                "Conta " + UUID.randomUUID().toString().substring(0, 8),
                TipoConta.CORRENTE,
                saldo,
                saldo,
                null,
                null,
                null,
                true,
                java.time.Instant.now(),
                null
        );
        contaRepositoryImpl.salvar(conta);
        return conta.getId();
    }

    private String requestValido(UUID contaId) {
        return """
                {
                  "descricao": "Aluguel mensal",
                  "tipo": "DESPESA",
                  "valorValor": 1500.00,
                  "valorMoeda": "BRL",
                  "contaId": "%s",
                  "periodicidade": "MENSAL",
                  "proximaOcorrencia": "%s"
                }
                """.formatted(contaId, PROXIMA);
    }

    private String criarRetornaId(UUID contaId) throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido(contaId))))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postCriaLancamentoValidoRetorna201() throws Exception {
        UUID contaId = criarContaPersistida();

        mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido(contaId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", equalTo(authenticatedUserId.toString())))
                .andExpect(jsonPath("$.descricao", equalTo("Aluguel mensal")))
                .andExpect(jsonPath("$.tipo", equalTo("DESPESA")))
                .andExpect(jsonPath("$.valor.valor", equalTo(1500.0)))
                .andExpect(jsonPath("$.valor.moeda", equalTo("BRL")))
                .andExpect(jsonPath("$.periodicidade", equalTo("MENSAL")))
                .andExpect(jsonPath("$.ativo", equalTo(true)));
    }

    @Test
    void postComDescricaoBlankRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        String body = """
                {
                  "descricao": "   ",
                  "tipo": "DESPESA",
                  "valorValor": 100.00,
                  "valorMoeda": "BRL",
                  "contaId": "%s",
                  "periodicidade": "MENSAL",
                  "proximaOcorrencia": "%s"
                }
                """.formatted(contaId, PROXIMA);

        mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComTipoTransferenciaRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        String body = """
                {
                  "descricao": "Transferencia",
                  "tipo": "TRANSFERENCIA",
                  "valorValor": 100.00,
                  "valorMoeda": "BRL",
                  "contaId": "%s",
                  "periodicidade": "MENSAL",
                  "proximaOcorrencia": "%s"
                }
                """.formatted(contaId, PROXIMA);

        mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetornaVazioQuandoNenhum() throws Exception {
        mockMvc.perform(comAuth(get("/api/lancamentos-recorrentes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getListaRetornaTodosOsLancamentos() throws Exception {
        UUID contaId = criarContaPersistida();
        criarRetornaId(contaId);
        criarRetornaId(contaId);

        mockMvc.perform(comAuth(get("/api/lancamentos-recorrentes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPorIdRetornaLancamentoExistente() throws Exception {
        UUID contaId = criarContaPersistida();
        String id = criarRetornaId(contaId);

        mockMvc.perform(comAuth(get("/api/lancamentos-recorrentes/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.descricao", equalTo("Aluguel mensal")));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/lancamentos-recorrentes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDesativaLancamentoRetorna204() throws Exception {
        UUID contaId = criarContaPersistida();
        String id = criarRetornaId(contaId);

        mockMvc.perform(comAuth(delete("/api/lancamentos-recorrentes/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/lancamentos-recorrentes/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", equalTo(false)));
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/lancamentos-recorrentes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void postExecucaoCriaTransacaoEAvancaProximaOcorrencia() throws Exception {
        UUID contaId = criarContaPersistida();
        String id = criarRetornaId(contaId);

        mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes/" + id + "/execucoes")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transacaoId", notNullValue()))
                .andExpect(jsonPath("$.lancamentoRecorrenteId", equalTo(id)))
                .andExpect(jsonPath("$.dataExecutada", equalTo(PROXIMA)))
                .andExpect(jsonPath("$.novaProximaOcorrencia", notNullValue()));
    }

    @Test
    void postExecucaoEmLancamentoInativoRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        String id = criarRetornaId(contaId);

        mockMvc.perform(comAuth(delete("/api/lancamentos-recorrentes/" + id)));

        mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes/" + id + "/execucoes")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postExecucaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(post("/api/lancamentos-recorrentes/" + UUID.randomUUID() + "/execucoes")))
                .andExpect(status().isNotFound());
    }
}
