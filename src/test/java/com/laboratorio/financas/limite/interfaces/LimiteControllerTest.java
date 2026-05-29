package com.laboratorio.financas.limite.interfaces;

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
import com.laboratorio.financas.limite.infrastructure.persistence.LimiteJpaRepository;
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

class LimiteControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LimiteJpaRepository jpaRepository;

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
        body.put("nome", "Limite Mensal");
        body.put("tipo", "MENSAL");
        body.put("valor", 500.00);
        return body;
    }

    private String criarLimiteRetornaId() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/limites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido()))))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/limites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Limite Mensal")))
                .andExpect(jsonPath("$.tipo", equalTo("MENSAL")))
                .andExpect(jsonPath("$.valor", equalTo(500.00)))
                .andExpect(jsonPath("$.ativo", equalTo(true)));
    }

    @Test
    void postNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("nome", "   ");
        mockMvc.perform(comAuth(post("/api/limites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postNomeAcimaDe100RetornaBadRequest() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("nome", "a".repeat(101));
        mockMvc.perform(comAuth(post("/api/limites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postValorNegativoRetorna400() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("valor", -10.00);
        mockMvc.perform(comAuth(post("/api/limites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor", notNullValue()));
    }

    @Test
    void postTipoInvalidoRetorna400() throws Exception {
        Map<String, Object> body = corpoValido();
        body.put("tipo", "INEXISTENTE");
        mockMvc.perform(comAuth(post("/api/limites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetorna200ComLimites() throws Exception {
        criarLimiteRetornaId();

        mockMvc.perform(comAuth(get("/api/limites")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getPorIdRetorna200() throws Exception {
        String id = criarLimiteRetornaId();

        mockMvc.perform(comAuth(get("/api/limites/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/limites/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putExistenteRetorna200() throws Exception {
        String id = criarLimiteRetornaId();
        Map<String, Object> body = corpoValido();
        body.put("nome", "Limite Atualizado");
        body.put("tipo", "ANUAL");
        body.put("valor", 1000.00);

        mockMvc.perform(comAuth(put("/api/limites/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Limite Atualizado")))
                .andExpect(jsonPath("$.tipo", equalTo("ANUAL")))
                .andExpect(jsonPath("$.valor", equalTo(1000.00)));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/limites/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpoValido()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204EFazSoftDelete() throws Exception {
        String id = criarLimiteRetornaId();

        mockMvc.perform(comAuth(delete("/api/limites/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/limites/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", equalTo(false)));
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/limites/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/limites")).andExpect(status().isUnauthorized());
    }
}
