package com.laboratorio.financas.emprestimo.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.emprestimo.infrastructure.persistence.EmprestimoJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class EmprestimoControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmprestimoJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("descricao", "Emprestimo a Joao");
        body.put("nomeTerceiro", "Joao Silva");
        body.put("tipo", "CONCEDIDO");
        body.put("valor", 500.00);
        body.put("moeda", "BRL");
        body.put("dataEmprestimo", "2026-05-30");
        return body;
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.descricao", equalTo("Emprestimo a Joao")))
                .andExpect(jsonPath("$.tipo", equalTo("CONCEDIDO")))
                .andExpect(jsonPath("$.valor.valor", equalTo(500.00)))
                .andExpect(jsonPath("$.valor.moeda", equalTo("BRL")))
                .andExpect(jsonPath("$.quitado", equalTo(false)));
    }

    @Test
    void postDescricaoBlankRetorna400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("descricao", "  ");
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao", notNullValue()));
    }

    @Test
    void postValorZeroRetorna400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("valor", 0);
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetorna200() throws Exception {
        mockMvc.perform(comAuth(post("/api/emprestimos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validBody()))));

        mockMvc.perform(comAuth(get("/api/emprestimos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getPorIdExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody()))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/emprestimos/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)));
    }

    @Test
    void putExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody()))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> body = validBody();
        body.put("descricao", "Novo");
        body.put("quitado", true);

        mockMvc.perform(comAuth(put("/api/emprestimos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Novo")))
                .andExpect(jsonPath("$.quitado", equalTo(true)));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/emprestimos/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody()))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/emprestimos/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/emprestimos/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/emprestimos")).andExpect(status().isUnauthorized());
    }
}
