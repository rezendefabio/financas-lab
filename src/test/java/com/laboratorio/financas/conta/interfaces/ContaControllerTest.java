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
import java.math.BigDecimal;
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

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        return Map.of(
                "nome", "Carteira",
                "tipo", "CORRENTE",
                "saldoInicialValor", BigDecimal.valueOf(100),
                "saldoInicialMoeda", "BRL"
        );
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
