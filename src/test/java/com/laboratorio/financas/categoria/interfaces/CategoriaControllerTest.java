package com.laboratorio.financas.categoria.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class CategoriaControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoriaJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        return Map.of(
                "nome", "Salario",
                "tipo", "RECEITA"
        );
    }

    @Test
    void postCategoriaValidaRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Salario")))
                .andExpect(jsonPath("$.tipo", equalTo("RECEITA")));
    }

    @Test
    void postCategoriaNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "   ", "tipo", "RECEITA");
        mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postCategoriaTipoNullRetorna400() throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("nome", "Salario");
        body.put("tipo", null);
        mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCategoriaTipoInvalidoRetorna400() throws Exception {
        String body = "{\"nome\":\"Salario\",\"tipo\":\"XYZ\"}";
        mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategoriasRetornaTodasSemParam() throws Exception {
        mockMvc.perform(comAuth(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestValido()))));
        mockMvc.perform(comAuth(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Aluguel", "tipo", "DESPESA")))));

        mockMvc.perform(comAuth(get("/api/categorias")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getCategoriasComParamTipoFiltra() throws Exception {
        mockMvc.perform(comAuth(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestValido()))));
        mockMvc.perform(comAuth(post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Aluguel", "tipo", "DESPESA")))));

        mockMvc.perform(comAuth(get("/api/categorias").param("tipo", "RECEITA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo", equalTo("RECEITA")));
    }

    @Test
    void getCategoriasComTipoInvalidoRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/categorias").param("tipo", "XYZ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategoriaPorIdExistenteRetorna200() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/categorias/" + idStr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(idStr)));
    }

    @Test
    void getCategoriaPorIdInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(get("/api/categorias/" + idInexistente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void deleteCategoriaExistenteRetorna204() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/categorias/" + idStr)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategoriaInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(delete("/api/categorias/" + idInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cicloCompletoPostGetDeleteGet404() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/categorias/" + idStr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Salario")));

        mockMvc.perform(comAuth(delete("/api/categorias/" + idStr)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/categorias/" + idStr)))
                .andExpect(status().isNotFound());
    }
}
