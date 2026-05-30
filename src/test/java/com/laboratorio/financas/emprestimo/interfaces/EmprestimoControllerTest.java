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

    private Map<String, Object> bodyValido(String descricao) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("descricao", descricao);
        body.put("nomeTerceiro", "Joao");
        body.put("tipo", "CONCEDIDO");
        body.put("valor", "150.75");
        body.put("moeda", "BRL");
        body.put("dataEmprestimo", "2026-01-20");
        body.put("quitado", false);
        return body;
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Emprestimo Joao")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.descricao", equalTo("Emprestimo Joao")))
                .andExpect(jsonPath("$.tipo", equalTo("CONCEDIDO")))
                .andExpect(jsonPath("$.valor.valor", equalTo(150.75)))
                .andExpect(jsonPath("$.valor.moeda", equalTo("BRL")));
    }

    @Test
    void postDescricaoBlankRetorna400() throws Exception {
        Map<String, Object> body = bodyValido("   ");
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao", notNullValue()));
    }

    @Test
    void getListaRetorna200ComItensCriados() throws Exception {
        mockMvc.perform(comAuth(post("/api/emprestimos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bodyValido("A")))));

        mockMvc.perform(comAuth(get("/api/emprestimos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getPorIdExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("X")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/emprestimos/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("X")));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/emprestimos/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Antiga")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> body = bodyValido("Nova");
        body.put("quitado", true);

        mockMvc.perform(comAuth(put("/api/emprestimos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Nova")))
                .andExpect(jsonPath("$.quitado", equalTo(true)));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/emprestimos/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("X")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Remover")))))
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
