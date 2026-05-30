package com.laboratorio.financas.lembrete.interfaces;

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
import com.laboratorio.financas.lembrete.infrastructure.persistence.LembreteJpaRepository;
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

class LembreteControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LembreteJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> bodyValido(String titulo) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", titulo);
        m.put("descricao", "Descricao");
        m.put("dataLembrete", "2026-06-15");
        m.put("prioridade", "MEDIA");
        return m;
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Pagar")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo", equalTo("Pagar")))
                .andExpect(jsonPath("$.prioridade", equalTo("MEDIA")))
                .andExpect(jsonPath("$.concluido", equalTo(false)));
    }

    @Test
    void postTituloBlankRetorna400() throws Exception {
        Map<String, Object> body = bodyValido("   ");
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo", notNullValue()));
    }

    @Test
    void postSemDataRetorna400() throws Exception {
        Map<String, Object> body = bodyValido("Pagar");
        body.remove("dataLembrete");
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.dataLembrete", notNullValue()));
    }

    @Test
    void postSemPrioridadeRetorna400() throws Exception {
        Map<String, Object> body = bodyValido("Pagar");
        body.remove("prioridade");
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.prioridade", notNullValue()));
    }

    @Test
    void getListaRetorna200() throws Exception {
        mockMvc.perform(comAuth(post("/api/lembretes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bodyValido("A")))));

        mockMvc.perform(comAuth(get("/api/lembretes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getPorIdExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Detalhe")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/lembretes/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", equalTo("Detalhe")));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/lembretes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Antigo")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        Map<String, Object> putBody = new HashMap<>(bodyValido("Novo"));
        putBody.put("concluido", true);
        putBody.put("prioridade", "ALTA");

        mockMvc.perform(comAuth(put("/api/lembretes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putBody))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", equalTo("Novo")))
                .andExpect(jsonPath("$.concluido", equalTo(true)))
                .andExpect(jsonPath("$.prioridade", equalTo("ALTA")));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        Map<String, Object> putBody = new HashMap<>(bodyValido("X"));
        putBody.put("concluido", false);
        mockMvc.perform(comAuth(put("/api/lembretes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putBody))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyValido("Remover")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/lembretes/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/lembretes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/lembretes")).andExpect(status().isUnauthorized());
    }
}
