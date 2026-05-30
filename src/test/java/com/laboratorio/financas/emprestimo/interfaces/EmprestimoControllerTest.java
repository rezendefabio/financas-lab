package com.laboratorio.financas.emprestimo.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.emprestimo.infrastructure.persistence.EmprestimoJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.HashMap;
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

    private Map<String, Object> bodyValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("descricao", "Teste");
        body.put("nomeTerceiro", "Joao");
        body.put("tipo", "CONCEDIDO");
        body.put("valor", 150.00);
        body.put("dataEmprestimo", "2026-01-15");
        body.put("quitado", false);
        return body;
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.descricao").value("Teste"))
                .andExpect(jsonPath("$.tipo").value("CONCEDIDO"))
                .andExpect(jsonPath("$.valor.moeda").value("BRL"));
    }

    @Test
    void postDescricaoBlankRetorna400() throws Exception {
        Map<String, Object> body = bodyValido();
        body.put("descricao", "  ");
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postValorNegativoRetorna400() throws Exception {
        Map<String, Object> body = bodyValido();
        body.put("valor", -10);
        mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetornaArray() throws Exception {
        mockMvc.perform(comAuth(post("/api/emprestimos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bodyValido()))));

        mockMvc.perform(comAuth(get("/api/emprestimos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPorIdExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido()))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/emprestimos/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Teste"));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/emprestimos/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putExistenteRetorna200ComCamposAtualizados() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido()))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> update = bodyValido();
        update.put("descricao", "Atualizado");
        update.put("tipo", "RECEBIDO");
        update.put("quitado", true);

        mockMvc.perform(comAuth(put("/api/emprestimos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Atualizado"))
                .andExpect(jsonPath("$.tipo").value("RECEBIDO"))
                .andExpect(jsonPath("$.quitado").value(true));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/emprestimos/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/emprestimos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido()))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/emprestimos/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/emprestimos")).andExpect(status().isUnauthorized());
    }
}
