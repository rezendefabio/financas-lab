package com.laboratorio.financas.fatura.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.fatura.infrastructure.persistence.FaturaJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class FaturaControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FaturaJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> faturaBasica(String nome) {
        Map<String, Object> body = new HashMap<>();
        body.put("contaId", UUID.randomUUID().toString());
        body.put("nome", nome);
        body.put("dataVencimento", "2026-06-10");
        return body;
    }

    private String criarRetornaId(String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faturaBasica(nome)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postFaturaValidaRetorna201() throws Exception {
        Map<String, Object> body = faturaBasica("Cartao Maio");
        body.put("dataFechamento", "2026-06-03");
        body.put("valorTotalValor", 1500.00);
        body.put("valorTotalMoeda", "BRL");

        mockMvc.perform(comAuth(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Cartao Maio")))
                .andExpect(jsonPath("$.dataVencimento", equalTo("2026-06-10")))
                .andExpect(jsonPath("$.dataFechamento", equalTo("2026-06-03")))
                .andExpect(jsonPath("$.valorTotal.valor", equalTo(1500.00)))
                .andExpect(jsonPath("$.valorTotal.moeda", equalTo("BRL")))
                .andExpect(jsonPath("$.paga", equalTo(false)));
    }

    @Test
    void postFaturaSemValorTotalRetornaValorNulo() throws Exception {
        mockMvc.perform(comAuth(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faturaBasica("Cartao")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal", nullValue()));
    }

    @Test
    void postFaturaNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = faturaBasica("   ");
        mockMvc.perform(comAuth(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postFaturaNomeAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = faturaBasica("a".repeat(101));
        mockMvc.perform(comAuth(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postFaturaSemContaIdRetorna400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Cartao");
        body.put("dataVencimento", "2026-06-10");
        mockMvc.perform(comAuth(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetornaFaturasDoUsuario() throws Exception {
        criarRetornaId("Maio");
        criarRetornaId("Junho");

        mockMvc.perform(comAuth(get("/api/faturas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getListaRetornaVazioSemRegistros() throws Exception {
        mockMvc.perform(comAuth(get("/api/faturas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPorIdRetornaFaturaExistente() throws Exception {
        String id = criarRetornaId("Cartao");

        mockMvc.perform(comAuth(get("/api/faturas/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.nome", equalTo("Cartao")));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/faturas/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putAtualizaNomeEVencimento() throws Exception {
        String id = criarRetornaId("Cartao");

        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Cartao Julho");
        body.put("dataVencimento", "2026-07-10");

        mockMvc.perform(comAuth(put("/api/faturas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Cartao Julho")))
                .andExpect(jsonPath("$.dataVencimento", equalTo("2026-07-10")));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "X");
        body.put("dataVencimento", "2026-07-10");
        mockMvc.perform(comAuth(put("/api/faturas/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemoveFaturaERetorna204() throws Exception {
        String id = criarRetornaId("Cartao");

        mockMvc.perform(comAuth(delete("/api/faturas/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/faturas/" + id)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/faturas/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void postSemAuthRetorna401() throws Exception {
        mockMvc.perform(post("/api/faturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(faturaBasica("Cartao"))))
                .andExpect(status().isUnauthorized());
    }
}
