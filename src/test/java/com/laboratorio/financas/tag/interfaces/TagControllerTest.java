package com.laboratorio.financas.tag.interfaces;

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
import com.laboratorio.financas.tag.infrastructure.persistence.TagJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
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
class TagControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        return Map.of("nome", "Essencial", "cor", "#FF0000");
    }

    private String criarTagRetornaId(String nome) throws Exception {
        Map<String, Object> body = Map.of("nome", nome);
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postTagValidaRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Essencial")))
                .andExpect(jsonPath("$.cor", equalTo("#FF0000")));
    }

    @Test
    void postTagSemCorRetorna201ComCorNula() throws Exception {
        Map<String, Object> body = Map.of("nome", "Sem Cor");
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", equalTo("Sem Cor")));
    }

    @Test
    void postTagNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "   ");
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postTagNomeAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "a".repeat(51));
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postTagDuplicadaRetorna400() throws Exception {
        mockMvc.perform(comAuth(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Duplicada")))));

        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Duplicada")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTagsRetornaListaDoUsuario() throws Exception {
        mockMvc.perform(comAuth(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Tag A")))));
        mockMvc.perform(comAuth(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Tag B")))));

        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getTagsRetornaListaVaziaQuandoNaoHaTags() throws Exception {
        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putTagExistenteRetorna200() throws Exception {
        String id = criarTagRetornaId("Antiga");

        Map<String, Object> update = Map.of("nome", "Nova", "cor", "#00FF00");
        mockMvc.perform(comAuth(put("/api/tags/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Nova")))
                .andExpect(jsonPath("$.cor", equalTo("#00FF00")));
    }

    @Test
    void putTagInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        Map<String, Object> update = Map.of("nome", "Nova");
        mockMvc.perform(comAuth(put("/api/tags/" + idInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void deleteTagExistenteRetorna204() throws Exception {
        String id = criarTagRetornaId("Remover");

        mockMvc.perform(comAuth(delete("/api/tags/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTagInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(delete("/api/tags/" + idInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cicloCompletoPostGetPutDelete() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Ciclo", "cor", "#111111")))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", equalTo("Ciclo")));

        mockMvc.perform(comAuth(put("/api/tags/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Ciclo Atualizado")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Ciclo Atualizado")));

        mockMvc.perform(comAuth(delete("/api/tags/" + idStr)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isUnauthorized());
    }
}
