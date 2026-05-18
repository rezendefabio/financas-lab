package com.laboratorio.financas.anexo.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.laboratorio.financas.anexo.infrastructure.persistence.AnexoJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;

@AutoConfigureMockMvc
class AnexoControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private AnexoJpaRepository anexoJpaRepository;

    @AfterEach
    void limpar() {
        anexoJpaRepository.deleteAll();
    }

    private MockMultipartFile arquivo(String nome, String tipo, String conteudo) {
        return new MockMultipartFile("arquivo", nome, tipo,
                conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private String fazerUpload(String entidadeTipo, UUID entidadeId) throws Exception {
        return mockMvc.perform(comAuth(multipart("/api/anexos")
                        .file(arquivo("comprovante.pdf", "application/pdf", "conteudo do arquivo"))
                        .param("entidadeTipo", entidadeTipo)
                        .param("entidadeId", entidadeId.toString())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Nested
    @DisplayName("POST /api/anexos")
    class Upload {

        @Test
        @DisplayName("retorna 201 com anexo criado")
        void uploadDeveRetornar201ComAnexoCriado() throws Exception {
            UUID entidadeId = UUID.randomUUID();

            mockMvc.perform(comAuth(multipart("/api/anexos")
                            .file(arquivo("nota.pdf", "application/pdf", "abc"))
                            .param("entidadeTipo", "TRANSACAO")
                            .param("entidadeId", entidadeId.toString())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.nome", equalTo("nota.pdf")))
                    .andExpect(jsonPath("$.tipoConteudo", equalTo("application/pdf")))
                    .andExpect(jsonPath("$.tamanho", equalTo(3)))
                    .andExpect(jsonPath("$.entidadeTipo", equalTo("TRANSACAO")))
                    .andExpect(jsonPath("$.entidadeId", equalTo(entidadeId.toString())));
        }

        @Test
        @DisplayName("persiste o anexo de forma recuperavel via listagem")
        void uploadDevePersistirAnexoRecuperavel() throws Exception {
            UUID entidadeId = UUID.randomUUID();
            fazerUpload("CONTA", entidadeId);

            mockMvc.perform(comAuth(get("/api/anexos")
                            .param("entidadeTipo", "CONTA")
                            .param("entidadeId", entidadeId.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", equalTo(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/anexos")
    class Listar {

        @Test
        @DisplayName("retorna 200 com lista vazia quando nao ha anexos")
        void listarDeveRetornar200ComListaVazia() throws Exception {
            mockMvc.perform(comAuth(get("/api/anexos")
                            .param("entidadeTipo", "META")
                            .param("entidadeId", UUID.randomUUID().toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()", equalTo(0)));
        }

        @Test
        @DisplayName("retorna apenas anexos da entidade informada")
        void listarDeveIsolarPorEntidade() throws Exception {
            UUID transacaoId = UUID.randomUUID();
            fazerUpload("TRANSACAO", transacaoId);
            fazerUpload("TRANSACAO", UUID.randomUUID());

            mockMvc.perform(comAuth(get("/api/anexos")
                            .param("entidadeTipo", "TRANSACAO")
                            .param("entidadeId", transacaoId.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", equalTo(1)))
                    .andExpect(jsonPath("$[0].entidadeId", equalTo(transacaoId.toString())));
        }
    }

    @Nested
    @DisplayName("GET /api/anexos/{id}/download")
    class Download {

        @Test
        @DisplayName("retorna 302 com header Location para anexo existente")
        void downloadDeveRetornar302ComLocation() throws Exception {
            String corpo = fazerUpload("TRANSACAO", UUID.randomUUID());
            String id = JsonPath.read(corpo, "$.id");

            mockMvc.perform(comAuth(get("/api/anexos/{id}/download", id)))
                    .andExpect(status().isFound())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("retorna 404 ao baixar anexo inexistente")
        void downloadDeveRetornar404QuandoNaoEncontrado() throws Exception {
            mockMvc.perform(comAuth(get("/api/anexos/{id}/download", UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/anexos/{id}")
    class Remover {

        @Test
        @DisplayName("retorna 204 ao remover anexo existente")
        void removerDeveRetornar204() throws Exception {
            UUID entidadeId = UUID.randomUUID();
            String corpo = fazerUpload("META", entidadeId);
            String id = JsonPath.read(corpo, "$.id");

            mockMvc.perform(comAuth(delete("/api/anexos/{id}", id)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(comAuth(get("/api/anexos")
                            .param("entidadeTipo", "META")
                            .param("entidadeId", entidadeId.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", equalTo(0)));
        }

        @Test
        @DisplayName("retorna 404 ao remover anexo inexistente")
        void removerDeveRetornar404QuandoNaoEncontrado() throws Exception {
            mockMvc.perform(comAuth(delete("/api/anexos/{id}", UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }
    }
}
