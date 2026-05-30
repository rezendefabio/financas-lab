package com.laboratorio.financas.lembrete.interfaces.rest;

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

    private Map<String, Object> corpoValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Pagar boleto");
        body.put("descricao", "Conta de luz");
        body.put("dataLembrete", "2026-06-15");
        body.put("prioridade", "MEDIA");
        body.put("concluido", false);
        return body;
    }

    private String criarLembreteRetornaId() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido()))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo", equalTo("Pagar boleto")))
                .andExpect(jsonPath("$.prioridade", equalTo("MEDIA")))
                .andExpect(jsonPath("$.dataLembrete", equalTo("2026-06-15")))
                .andExpect(jsonPath("$.concluido", equalTo(false)));
    }

    @Test
    void postTituloBlankRetorna400() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("titulo", "   ");
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo", notNullValue()));
    }

    @Test
    void postTituloAcimaDe100RetornaBadRequest() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("titulo", "a".repeat(101));
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.titulo", notNullValue()));
    }

    @Test
    void postSemDataRetorna400() throws Exception {
        Map<String, Object> body = corpoValido();
        body.remove("dataLembrete");
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.dataLembrete", notNullValue()));
    }

    @Test
    void postPrioridadeInvalidaRetorna400() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("prioridade", "INEXISTENTE");
        mockMvc.perform(comAuth(post("/api/lembretes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetorna200ComLembretes() throws Exception {
        criarLembreteRetornaId();

        mockMvc.perform(comAuth(get("/api/lembretes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getPorIdRetorna200() throws Exception {
        String id = criarLembreteRetornaId();

        mockMvc.perform(comAuth(get("/api/lembretes/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/lembretes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putExistenteRetorna200() throws Exception {
        String id = criarLembreteRetornaId();
        Map<String, Object> body = corpoValido();
        body.put("titulo", "Atualizado");
        body.put("prioridade", "ALTA");
        body.put("concluido", true);

        mockMvc.perform(comAuth(put("/api/lembretes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", equalTo("Atualizado")))
                .andExpect(jsonPath("$.prioridade", equalTo("ALTA")))
                .andExpect(jsonPath("$.concluido", equalTo(true)));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/lembretes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204EFazDeleteFisico() throws Exception {
        String id = criarLembreteRetornaId();

        mockMvc.perform(comAuth(delete("/api/lembretes/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/lembretes/" + id)))
                .andExpect(status().isNotFound());
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
