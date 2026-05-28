package com.laboratorio.financas.grupo.interfaces;

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
import com.laboratorio.financas.grupo.infrastructure.persistence.GrupoJpaRepository;
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
class GrupoControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GrupoJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        return Map.of("nome", "Viagem Europa", "descricao", "Gastos da viagem");
    }

    private String criarGrupoRetornaId(String nome) throws Exception {
        Map<String, Object> body = Map.of("nome", nome);
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postGrupoValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Viagem Europa")))
                .andExpect(jsonPath("$.descricao", equalTo("Gastos da viagem")))
                .andExpect(jsonPath("$.ativo", equalTo(true)));
    }

    @Test
    void postGrupoSemDescricaoRetorna201() throws Exception {
        Map<String, Object> body = Map.of("nome", "Casa Nova");
        mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", equalTo("Casa Nova")));
    }

    @Test
    void postGrupoNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "   ");
        mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postGrupoNomeAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "a".repeat(101));
        mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postGrupoDescricaoAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "Valido", "descricao", "a".repeat(301));
        mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.descricao", notNullValue()));
    }

    @Test
    void getGruposRetornaListaDoUsuario() throws Exception {
        mockMvc.perform(comAuth(post("/api/grupos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Grupo A")))));
        mockMvc.perform(comAuth(post("/api/grupos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Grupo B")))));

        mockMvc.perform(comAuth(get("/api/grupos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getGruposRetornaListaVaziaQuandoNaoHaGrupos() throws Exception {
        mockMvc.perform(comAuth(get("/api/grupos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putGrupoExistenteRetorna200() throws Exception {
        String id = criarGrupoRetornaId("Antigo");

        Map<String, Object> update = Map.of("nome", "Novo", "descricao", "Atualizado");
        mockMvc.perform(comAuth(put("/api/grupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Novo")))
                .andExpect(jsonPath("$.descricao", equalTo("Atualizado")));
    }

    @Test
    void putGrupoInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        Map<String, Object> update = Map.of("nome", "Novo");
        mockMvc.perform(comAuth(put("/api/grupos/" + idInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void deleteGrupoExistenteRetorna204() throws Exception {
        String id = criarGrupoRetornaId("Remover");

        mockMvc.perform(comAuth(delete("/api/grupos/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteGrupoInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(delete("/api/grupos/" + idInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cicloCompletoPostGetPutDelete() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Ciclo", "descricao", "Desc")))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/grupos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", equalTo("Ciclo")));

        mockMvc.perform(comAuth(put("/api/grupos/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Ciclo Atualizado")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Ciclo Atualizado")));

        mockMvc.perform(comAuth(delete("/api/grupos/" + idStr)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/grupos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/grupos"))
                .andExpect(status().isUnauthorized());
    }
}
