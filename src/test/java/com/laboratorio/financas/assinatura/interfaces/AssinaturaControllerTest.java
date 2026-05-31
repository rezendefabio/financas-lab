package com.laboratorio.financas.assinatura.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.assinatura.infrastructure.persistence.AssinaturaJpaRepository;
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

class AssinaturaControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AssinaturaJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> corpoValido(String nome) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", nome);
        body.put("tipo", "STREAMING");
        body.put("valorMensal", 29.90);
        body.put("moeda", "BRL");
        body.put("dataRenovacao", "2026-06-15");
        return body;
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/assinaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido("Netflix")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Netflix"))
                .andExpect(jsonPath("$.tipo").value("STREAMING"))
                .andExpect(jsonPath("$.valorMensal.valor").value(29.90))
                .andExpect(jsonPath("$.valorMensal.moeda").value("BRL"))
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void postNomeBlankRetorna400() throws Exception {
        mockMvc.perform(comAuth(post("/api/assinaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido("  ")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").isNotEmpty());
    }

    @Test
    void getListaRetornaArray() throws Exception {
        mockMvc.perform(comAuth(post("/api/assinaturas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpoValido("A")))));

        mockMvc.perform(comAuth(get("/api/assinaturas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("A"));
    }

    @Test
    void getPorIdExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/assinaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido("Buscavel")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/assinaturas/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Buscavel"));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/assinaturas/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putExistenteRetorna200ComCamposAtualizados() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/assinaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido("Antigo")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> atualizado = corpoValido("Novo");
        atualizado.put("tipo", "OUTROS");
        atualizado.put("ativa", false);

        mockMvc.perform(comAuth(put("/api/assinaturas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizado))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo"))
                .andExpect(jsonPath("$.tipo").value("OUTROS"))
                .andExpect(jsonPath("$.ativa").value(false));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/assinaturas/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido("X")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/assinaturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido("Apagar")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/assinaturas/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/assinaturas")).andExpect(status().isUnauthorized());
    }
}
