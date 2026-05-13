package com.laboratorio.financas.orcamento.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaJpaRepository;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaRepositoryImpl;
import com.laboratorio.financas.orcamento.infrastructure.persistence.OrcamentoJpaRepository;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class OrcamentoControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrcamentoJpaRepository orcamentoJpaRepository;

    @Autowired
    private CategoriaJpaRepository categoriaJpaRepository;

    @Autowired
    private CategoriaRepositoryImpl categoriaRepositoryImpl;

    @AfterEach
    void limpar() {
        orcamentoJpaRepository.deleteAll();
        categoriaJpaRepository.deleteAll();
    }

    private UUID criarCategoria() {
        Categoria categoria = categoriaRepositoryImpl.salvar(new Categoria("Alimentacao", TipoCategoria.DESPESA));
        return categoria.getId();
    }

    private String criarOrcamentoJson(UUID categoriaId, String mesAno) {
        return """
                {
                  "categoriaId": "%s",
                  "valorLimiteValor": 500.00,
                  "valorLimiteMoeda": "BRL",
                  "mesAno": "%s"
                }
                """.formatted(categoriaId, mesAno);
    }

    @Nested
    @DisplayName("POST /api/orcamentos")
    class Criar {

        @Test
        @DisplayName("retorna 201 com orcamento criado")
        void criarDeveRetornar201ComOrcamentoCriado() throws Exception {
            UUID categoriaId = criarCategoria();

            mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarOrcamentoJson(categoriaId, "2024-06-01")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.categoriaId").value(categoriaId.toString()))
                    .andExpect(jsonPath("$.valorLimite.valor").value(500.00))
                    .andExpect(jsonPath("$.valorLimite.moeda").value("BRL"))
                    .andExpect(jsonPath("$.mesAno").value("2024-06-01"))
                    .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @DisplayName("retorna 400 quando categoriaId e nulo")
        void criarDeveRetornar400QuandoCategoriaIdNulo() throws Exception {
            mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "valorLimiteValor": 500.00,
                                      "valorLimiteMoeda": "BRL",
                                      "mesAno": "2024-06-01"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 400 quando valorLimiteValor e nulo")
        void criarDeveRetornar400QuandoValorLimiteNulo() throws Exception {
            UUID categoriaId = criarCategoria();

            mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoriaId": "%s",
                                      "valorLimiteMoeda": "BRL",
                                      "mesAno": "2024-06-01"
                                    }
                                    """.formatted(categoriaId)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 400 quando moeda tem tamanho invalido")
        void criarDeveRetornar400QuandoMoedaComTamanhoInvalido() throws Exception {
            UUID categoriaId = criarCategoria();

            mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoriaId": "%s",
                                      "valorLimiteValor": 500.00,
                                      "valorLimiteMoeda": "BR",
                                      "mesAno": "2024-06-01"
                                    }
                                    """.formatted(categoriaId)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/orcamentos")
    class Listar {

        @Test
        @DisplayName("retorna 200 com lista vazia quando nao ha orcamentos")
        void listarDeveRetornar200ComListaVazia() throws Exception {
            mockMvc.perform(get("/api/orcamentos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("retorna 200 com orcamentos existentes")
        void listarDeveRetornar200ComOrcamentosExistentes() throws Exception {
            UUID categoriaId = criarCategoria();

            mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarOrcamentoJson(categoriaId, "2024-07-01")))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/orcamentos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].categoriaId").value(categoriaId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/orcamentos/{id}")
    class Buscar {

        @Test
        @DisplayName("retorna 200 com orcamento quando encontrado")
        void buscarDeveRetornar200QuandoEncontrado() throws Exception {
            UUID categoriaId = criarCategoria();

            String responseBody = mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarOrcamentoJson(categoriaId, "2024-08-01")))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String id = JsonPath.read(responseBody, "$.id");

            mockMvc.perform(get("/api/orcamentos/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.categoriaId").value(categoriaId.toString()))
                    .andExpect(jsonPath("$.ativo").value(true));
        }

        @Test
        @DisplayName("retorna 404 quando nao encontrado")
        void buscarDeveRetornar404QuandoNaoEncontrado() throws Exception {
            mockMvc.perform(get("/api/orcamentos/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/orcamentos/{id}")
    class Desativar {

        @Test
        @DisplayName("retorna 204 ao desativar orcamento existente")
        void desativarDeveRetornar204EDesativaOrcamento() throws Exception {
            UUID categoriaId = criarCategoria();

            String responseBody = mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarOrcamentoJson(categoriaId, "2024-09-01")))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String id = JsonPath.read(responseBody, "$.id");

            mockMvc.perform(delete("/api/orcamentos/{id}", id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/orcamentos/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ativo").value(false));
        }

        @Test
        @DisplayName("retorna 404 ao desativar orcamento inexistente")
        void desativarDeveRetornar404QuandoNaoEncontrado() throws Exception {
            mockMvc.perform(delete("/api/orcamentos/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/orcamentos/{id}/progresso")
    class Progresso {

        @Test
        @DisplayName("retorna 200 com progresso zerado quando nao ha transacoes")
        void progressoDeveRetornar200ComProgressoZeradoSemTransacoes() throws Exception {
            UUID categoriaId = criarCategoria();

            String responseBody = mockMvc.perform(post("/api/orcamentos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(criarOrcamentoJson(categoriaId, "2024-10-01")))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String id = JsonPath.read(responseBody, "$.id");

            mockMvc.perform(get("/api/orcamentos/{id}/progresso", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orcamentoId").value(id))
                    .andExpect(jsonPath("$.categoriaId").value(categoriaId.toString()))
                    .andExpect(jsonPath("$.valorLimite.valor").value(500.00))
                    .andExpect(jsonPath("$.totalGasto.valor").value(0))
                    .andExpect(jsonPath("$.percentualUtilizado").value(0))
                    .andExpect(jsonPath("$.status").value("ABAIXO"));
        }

        @Test
        @DisplayName("retorna 404 ao consultar progresso de orcamento inexistente")
        void progressoDeveRetornar404QuandoNaoEncontrado() throws Exception {
            mockMvc.perform(get("/api/orcamentos/{id}/progresso", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
