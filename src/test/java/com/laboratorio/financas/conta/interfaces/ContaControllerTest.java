package com.laboratorio.financas.conta.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoJpaRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class ContaControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContaJpaRepository jpaRepository;

    @Autowired
    private TransacaoJpaRepository transacaoJpaRepository;

    @AfterEach
    void limpar() {
        transacaoJpaRepository.deleteAll();
        jpaRepository.deleteAll();
    }

    private static final String DATA = "2025-01-15";

    private Map<String, Object> requestValido() {
        return Map.of(
                "nome", "Carteira",
                "tipo", "CORRENTE",
                "saldoInicialValor", BigDecimal.valueOf(100),
                "saldoInicialMoeda", "BRL"
        );
    }

    private String criarContaRetornaId(BigDecimal saldoInicial) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Conta " + UUID.randomUUID().toString().substring(0, 8));
        body.put("tipo", "CORRENTE");
        body.put("saldoInicialValor", saldoInicial);
        body.put("saldoInicialMoeda", "BRL");
        MvcResult resultado = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    private void criarReceita(String contaId, BigDecimal valor) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", "RECEITA");
        body.put("valor", valor);
        body.put("moeda", "BRL");
        body.put("data", DATA);
        body.put("descricao", "Receita teste");
        body.put("contaId", contaId);
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private void criarDespesa(String contaId, BigDecimal valor) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", "DESPESA");
        body.put("valor", valor);
        body.put("moeda", "BRL");
        body.put("data", DATA);
        body.put("descricao", "Despesa teste");
        body.put("contaId", contaId);
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private void criarTransferencia(String contaId, String contaDestinoId, BigDecimal valor) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", "TRANSFERENCIA");
        body.put("valor", valor);
        body.put("moeda", "BRL");
        body.put("data", DATA);
        body.put("descricao", "Transferencia teste");
        body.put("contaId", contaId);
        body.put("contaDestinoId", contaDestinoId);
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    void postContaCriaContaValidaRetorna201() throws Exception {
        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Carteira")))
                .andExpect(jsonPath("$.ativa", equalTo(true)));
    }

    @Test
    void postContaComNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of(
                "nome", "   ",
                "tipo", "CORRENTE",
                "saldoInicialValor", BigDecimal.valueOf(100),
                "saldoInicialMoeda", "BRL"
        );
        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postContaComNomeMaior100CharsRetorna400() throws Exception {
        Map<String, Object> body = Map.of(
                "nome", "A".repeat(101),
                "tipo", "CORRENTE",
                "saldoInicialValor", BigDecimal.valueOf(100),
                "saldoInicialMoeda", "BRL"
        );
        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postContaComTipoNullRetorna400() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("nome", "Carteira");
        body.put("tipo", null);
        body.put("saldoInicialValor", BigDecimal.valueOf(100));
        body.put("saldoInicialMoeda", "BRL");
        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postContaComMoedaInvalida4CharsRetorna400() throws Exception {
        Map<String, Object> body = Map.of(
                "nome", "Carteira",
                "tipo", "CORRENTE",
                "saldoInicialValor", BigDecimal.valueOf(100),
                "saldoInicialMoeda", "BRLX"
        );
        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postContaComTipoEnumDesconhecidoRetorna400() throws Exception {
        String body = "{\"nome\":\"Carteira\",\"tipo\":\"XYZ\",\"saldoInicialValor\":100,\"saldoInicialMoeda\":\"BRL\"}";
        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getContasRetornaTodasAsContas() throws Exception {
        // Given
        mockMvc.perform(post("/api/contas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestValido())));
        mockMvc.perform(post("/api/contas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "nome", "Poupanca",
                        "tipo", "POUPANCA",
                        "saldoInicialValor", BigDecimal.valueOf(200),
                        "saldoInicialMoeda", "BRL"
                ))));

        // When / Then
        mockMvc.perform(get("/api/contas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getContasComParamAtivaRetornaSoAtivas() throws Exception {
        // Given — cria uma conta e a desativa
        MvcResult resultado = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/contas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "nome", "Poupanca",
                        "tipo", "POUPANCA",
                        "saldoInicialValor", BigDecimal.valueOf(200),
                        "saldoInicialMoeda", "BRL"
                ))));

        mockMvc.perform(delete("/api/contas/" + idStr));

        // When / Then
        mockMvc.perform(get("/api/contas").param("ativa", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getContaPorIdRetornaContaExistente() throws Exception {
        // Given
        MvcResult resultado = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        // When / Then
        mockMvc.perform(get("/api/contas/" + idStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(idStr)));
    }

    @Test
    void getContaPorIdInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(get("/api/contas/" + idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void getContaPorIdMalformadoRetorna400() throws Exception {
        mockMvc.perform(get("/api/contas/nao-e-um-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteContaDesativaExistenteRetorna204() throws Exception {
        // Given
        MvcResult resultado = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        // When / Then
        mockMvc.perform(delete("/api/contas/" + idStr))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/contas/" + idStr))
                .andExpect(jsonPath("$.ativa", equalTo(false)));
    }

    @Test
    void deleteContaInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(delete("/api/contas/" + idInexistente))
                .andExpect(status().isNotFound());
    }

    // Saldo tests

    @Test
    void getSaldoContaSemTransacoesRetornaSaldoIgualAoInicial() throws Exception {
        // Given
        String contaId = criarContaRetornaId(BigDecimal.valueOf(1000));

        // When / Then
        mockMvc.perform(get("/api/contas/" + contaId + "/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaId", equalTo(contaId)))
                .andExpect(jsonPath("$.saldoInicial.valor", equalTo(1000.0)))
                .andExpect(jsonPath("$.saldoInicial.moeda", equalTo("BRL")))
                .andExpect(jsonPath("$.totalReceitas.valor", equalTo(0.0)))
                .andExpect(jsonPath("$.totalDespesas.valor", equalTo(0.0)))
                .andExpect(jsonPath("$.totalTransferenciasEnviadas.valor", equalTo(0.0)))
                .andExpect(jsonPath("$.totalTransferenciasRecebidas.valor", equalTo(0.0)))
                .andExpect(jsonPath("$.saldoAtual.valor", equalTo(1000.0)))
                .andExpect(jsonPath("$.calculadoEm", notNullValue()));
    }

    @Test
    void getSaldoContaComReceitaRefleteNoSaldoAtual() throws Exception {
        // Given
        String contaId = criarContaRetornaId(BigDecimal.valueOf(1000));
        criarReceita(contaId, BigDecimal.valueOf(500));

        // When / Then
        mockMvc.perform(get("/api/contas/" + contaId + "/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas.valor", equalTo(500.0)))
                .andExpect(jsonPath("$.saldoAtual.valor", equalTo(1500.0)));
    }

    @Test
    void getSaldoCenarioCompletoCalculaFormulaCerta() throws Exception {
        // Given
        String contaA = criarContaRetornaId(BigDecimal.valueOf(1000));
        String contaB = criarContaRetornaId(BigDecimal.valueOf(0));
        criarReceita(contaA, BigDecimal.valueOf(800));
        criarDespesa(contaA, BigDecimal.valueOf(200));
        criarTransferencia(contaA, contaB, BigDecimal.valueOf(100));

        // When / Then — A: 1000 + 800 - 200 - 100 = 1500
        mockMvc.perform(get("/api/contas/" + contaA + "/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas.valor", equalTo(800.0)))
                .andExpect(jsonPath("$.totalDespesas.valor", equalTo(200.0)))
                .andExpect(jsonPath("$.totalTransferenciasEnviadas.valor", equalTo(100.0)))
                .andExpect(jsonPath("$.totalTransferenciasRecebidas.valor", equalTo(0.0)))
                .andExpect(jsonPath("$.saldoAtual.valor", equalTo(1500.0)));

        // B: 0 + 100 = 100
        mockMvc.perform(get("/api/contas/" + contaB + "/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransferenciasRecebidas.valor", equalTo(100.0)))
                .andExpect(jsonPath("$.saldoAtual.valor", equalTo(100.0)));
    }

    @Test
    void getSaldoContaInexistenteRetorna404ComProblemDetail() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(get("/api/contas/" + idInexistente + "/saldo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void getSaldoContaInativaRetornaSaldoNormalmente() throws Exception {
        // Given
        String contaId = criarContaRetornaId(BigDecimal.valueOf(500));
        criarReceita(contaId, BigDecimal.valueOf(100));
        mockMvc.perform(delete("/api/contas/" + contaId));

        // When / Then — conta inativa ainda tem saldo calculado
        mockMvc.perform(get("/api/contas/" + contaId + "/saldo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoAtual.valor", equalTo(600.0)));
    }

    @Test
    void cicloCompletoPostGetDeleteGetAtivaFalse() throws Exception {
        // POST
        MvcResult resultado = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        // GET por id — ativa=true
        mockMvc.perform(get("/api/contas/" + idStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa", equalTo(true)));

        // DELETE
        mockMvc.perform(delete("/api/contas/" + idStr))
                .andExpect(status().isNoContent());

        // GET por id — ativa=false
        mockMvc.perform(get("/api/contas/" + idStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa", equalTo(false)));

        // GET ?ativa=true — nao retorna a conta desativada
        mockMvc.perform(get("/api/contas").param("ativa", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
