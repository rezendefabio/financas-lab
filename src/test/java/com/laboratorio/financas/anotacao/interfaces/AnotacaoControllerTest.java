package com.laboratorio.financas.anotacao.interfaces;

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
import com.laboratorio.financas.anotacao.infrastructure.persistence.AnotacaoJpaRepository;
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
class AnotacaoControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnotacaoJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        return Map.of(
                "titulo", "Lembrar de pagar fatura",
                "tipo", "LEMBRETE",
                "prioridade", "MEDIA"
        );
    }

    private String criarAnotacaoRetornaId() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/anotacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postAnotacaoValidaRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/anotacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.titulo", equalTo("Lembrar de pagar fatura")))
                .andExpect(jsonPath("$.tipo", equalTo("LEMBRETE")))
                .andExpect(jsonPath("$.prioridade", equalTo("MEDIA")));
    }

    @Test
    void postAnotacaoTituloBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("titulo", "  ", "tipo", "LEMBRETE", "prioridade", "MEDIA");
        mockMvc.perform(comAuth(post("/api/anotacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postAnotacaoSemTipoRetorna400() throws Exception {
        Map<String, Object> body = Map.of("titulo", "Titulo", "prioridade", "MEDIA");
        mockMvc.perform(comAuth(post("/api/anotacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postAnotacaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/anotacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAnotacoesRetornaListaDoUsuario() throws Exception {
        criarAnotacaoRetornaId();
        criarAnotacaoRetornaId();

        mockMvc.perform(comAuth(get("/api/anotacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAnotacoesSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/anotacoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAnotacaoPorIdExistenteRetorna200() throws Exception {
        String id = criarAnotacaoRetornaId();

        mockMvc.perform(comAuth(get("/api/anotacoes/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.titulo", equalTo("Lembrar de pagar fatura")));
    }

    @Test
    void getAnotacaoPorIdInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(get("/api/anotacoes/" + idInexistente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void getAnotacaoPorIdSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/anotacoes/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putAnotacaoExistenteRetorna200() throws Exception {
        String id = criarAnotacaoRetornaId();

        Map<String, Object> atualizacao = Map.of(
                "titulo", "Titulo atualizado",
                "tipo", "ALERTA",
                "prioridade", "ALTA"
        );

        mockMvc.perform(comAuth(put("/api/anotacoes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizacao))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", equalTo("Titulo atualizado")))
                .andExpect(jsonPath("$.tipo", equalTo("ALERTA")))
                .andExpect(jsonPath("$.prioridade", equalTo("ALTA")));
    }

    @Test
    void putAnotacaoInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        Map<String, Object> atualizacao = Map.of(
                "titulo", "Titulo",
                "tipo", "LEMBRETE",
                "prioridade", "MEDIA"
        );

        mockMvc.perform(comAuth(put("/api/anotacoes/" + idInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizacao))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAnotacaoExistenteRetorna204() throws Exception {
        String id = criarAnotacaoRetornaId();

        mockMvc.perform(comAuth(delete("/api/anotacoes/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAnotacaoInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(delete("/api/anotacoes/" + idInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAnotacaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(delete("/api/anotacoes/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cicloCompletoPostGetPutDeleteGet404() throws Exception {
        String id = criarAnotacaoRetornaId();

        mockMvc.perform(comAuth(get("/api/anotacoes/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", equalTo("Lembrar de pagar fatura")));

        Map<String, Object> atualizacao = Map.of(
                "titulo", "Titulo atualizado",
                "tipo", "OBSERVACAO",
                "prioridade", "BAIXA"
        );

        mockMvc.perform(comAuth(put("/api/anotacoes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizacao))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", equalTo("Titulo atualizado")));

        mockMvc.perform(comAuth(delete("/api/anotacoes/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/anotacoes/" + id)))
                .andExpect(status().isNotFound());
    }
}
