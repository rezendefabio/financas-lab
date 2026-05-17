package com.laboratorio.financas.incidente.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.laboratorio.financas.incidente.infrastructure.persistence.ErroRegistradoJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@AutoConfigureMockMvc
class IncidenteControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ErroRegistradoJpaRepository erroRegistradoJpaRepository;

    @AfterEach
    void limpar() {
        erroRegistradoJpaRepository.deleteAll();
    }

    private static final String REQUEST_JSON = """
            {
              "operacao": "CLIENT /dashboard",
              "classeErro": "TypeError",
              "mensagem": "undefined is not a function",
              "stackTrace": "at Component (app.tsx:42)"
            }
            """;

    @Nested
    @DisplayName("POST /api/incidentes")
    class Registrar {

        @Test
        @DisplayName("retorna 201 com codigo gerado mesmo sem autenticacao")
        void registrarDeveRetornar201ComCodigoSemAuth() throws Exception {
            mockMvc.perform(post("/api/incidentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REQUEST_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.matchesPattern(
                            "ERR-[0-9A-F]{8}")));
        }

        @Test
        @DisplayName("retorna 201 quando campos sao nulos")
        void registrarDeveRetornar201ComCamposNulos() throws Exception {
            mockMvc.perform(post("/api/incidentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/incidentes/{codigo}")
    class Buscar {

        @Test
        @DisplayName("retorna 200 com incidente quando encontrado")
        void buscarDeveRetornar200QuandoEncontrado() throws Exception {
            String responseBody = mockMvc.perform(post("/api/incidentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REQUEST_JSON))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String codigo = JsonPath.read(responseBody, "$.codigo");

            mockMvc.perform(comAuth(get("/api/incidentes/{codigo}", codigo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo").value(codigo))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.operacao").value("CLIENT /dashboard"))
                    .andExpect(jsonPath("$.classeErro").value("TypeError"))
                    .andExpect(jsonPath("$.mensagem").value("undefined is not a function"))
                    .andExpect(jsonPath("$.stackTrace").value("at Component (app.tsx:42)"))
                    .andExpect(jsonPath("$.criadoEm").isNotEmpty());
        }

        @Test
        @DisplayName("retorna 404 quando codigo nao existe")
        void buscarDeveRetornar404QuandoNaoEncontrado() throws Exception {
            mockMvc.perform(comAuth(get("/api/incidentes/{codigo}", "ERR-00000000")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("retorna 401 quando consultado sem autenticacao")
        void buscarDeveRetornar401SemAuth() throws Exception {
            mockMvc.perform(get("/api/incidentes/{codigo}", "ERR-00000000"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
