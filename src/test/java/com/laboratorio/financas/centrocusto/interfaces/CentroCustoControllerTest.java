package com.laboratorio.financas.centrocusto.interfaces;

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
import com.laboratorio.financas.centrocusto.infrastructure.persistence.CentroCustoJpaRepository;
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
class CentroCustoControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CentroCustoJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private String criarRetornaId(String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/centros-custo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postCentroCustoValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/centros-custo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Casa", "descricao", "Despesas residenciais")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Casa")))
                .andExpect(jsonPath("$.descricao", equalTo("Despesas residenciais")))
                .andExpect(jsonPath("$.ativo", equalTo(true)))
                .andExpect(jsonPath("$.userId", notNullValue()));
    }

    @Test
    void postCentroCustoNomeBlankRetorna400() throws Exception {
        mockMvc.perform(comAuth(post("/api/centros-custo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "   ")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCentroCustoNomeAcimaMaximoRetorna400() throws Exception {
        mockMvc.perform(comAuth(post("/api/centros-custo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "a".repeat(101))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCentroCustoDescricaoAcimaMaximoRetorna400() throws Exception {
        mockMvc.perform(comAuth(post("/api/centros-custo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Casa", "descricao", "a".repeat(256))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetornaCentrosDoUsuario() throws Exception {
        criarRetornaId("Casa");
        criarRetornaId("Trabalho");

        mockMvc.perform(comAuth(get("/api/centros-custo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getListaRetornaVazioSemRegistros() throws Exception {
        mockMvc.perform(comAuth(get("/api/centros-custo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getPorIdRetornaCentroCustoExistente() throws Exception {
        String id = criarRetornaId("Casa");

        mockMvc.perform(comAuth(get("/api/centros-custo/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.nome", equalTo("Casa")));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/centros-custo/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putAtualizaNomeEDescricao() throws Exception {
        String id = criarRetornaId("Casa");

        mockMvc.perform(comAuth(put("/api/centros-custo/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Trabalho", "descricao", "Atualizada")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Trabalho")))
                .andExpect(jsonPath("$.descricao", equalTo("Atualizada")));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/centros-custo/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "X")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDesativaCentroCustoERetorna204() throws Exception {
        String id = criarRetornaId("Casa");

        mockMvc.perform(comAuth(delete("/api/centros-custo/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/centros-custo/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", equalTo(false)));
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/centros-custo/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void postSemAuthRetorna401() throws Exception {
        mockMvc.perform(post("/api/centros-custo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Casa"))))
                .andExpect(status().isUnauthorized());
    }
}
