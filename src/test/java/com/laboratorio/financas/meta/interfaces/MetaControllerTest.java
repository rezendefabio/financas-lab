package com.laboratorio.financas.meta.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.meta.infrastructure.persistence.MetaJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class MetaControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MetaJpaRepository metaJpaRepository;

    @AfterEach
    void limpar() {
        metaJpaRepository.deleteAll();
    }

    private static final String PRAZO_FUTURO = LocalDate.now().plusMonths(6).toString();

    private String requestValido() {
        return """
                {
                  "nome": "Viagem Europa",
                  "valorAlvoValor": 10000.00,
                  "valorAlvoMoeda": "BRL",
                  "prazo": "%s"
                }
                """.formatted(PRAZO_FUTURO);
    }

    private String criarMetaRetornaId() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/metas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido())))
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postMetaCriaMetaValidaRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/metas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", equalTo(authenticatedUserId.toString())))
                .andExpect(jsonPath("$.nome", equalTo("Viagem Europa")))
                .andExpect(jsonPath("$.status", equalTo("EM_ANDAMENTO")))
                .andExpect(jsonPath("$.valorAlvo.valor", equalTo(10000.0)))
                .andExpect(jsonPath("$.valorAlvo.moeda", equalTo("BRL")))
                .andExpect(jsonPath("$.valorAtual.valor", equalTo(0.0)))
                .andExpect(jsonPath("$.percentualConcluido", equalTo(0.0)));
    }

    @Test
    void postMetaComNomeBlankRetorna400() throws Exception {
        String body = """
                {
                  "nome": "   ",
                  "valorAlvoValor": 5000.00,
                  "valorAlvoMoeda": "BRL",
                  "prazo": "%s"
                }
                """.formatted(PRAZO_FUTURO);
        mockMvc.perform(comAuth(post("/api/metas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postMetaComValorAlvoNullRetorna400() throws Exception {
        String body = """
                {
                  "nome": "Reserva",
                  "valorAlvoValor": null,
                  "valorAlvoMoeda": "BRL",
                  "prazo": "%s"
                }
                """.formatted(PRAZO_FUTURO);
        mockMvc.perform(comAuth(post("/api/metas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postMetaComMoedaInvalidaRetorna400() throws Exception {
        String body = """
                {
                  "nome": "Reserva",
                  "valorAlvoValor": 1000.00,
                  "valorAlvoMoeda": "BRLX",
                  "prazo": "%s"
                }
                """.formatted(PRAZO_FUTURO);
        mockMvc.perform(comAuth(post("/api/metas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postMetaComPrazoNullRetorna400() throws Exception {
        String body = """
                {
                  "nome": "Reserva",
                  "valorAlvoValor": 1000.00,
                  "valorAlvoMoeda": "BRL",
                  "prazo": null
                }
                """;
        mockMvc.perform(comAuth(post("/api/metas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMetasRetornaListaVaziaQuandoNenhumaMeta() throws Exception {
        mockMvc.perform(comAuth(get("/api/metas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMetasRetornaTodasAsMetasCriadas() throws Exception {
        mockMvc.perform(comAuth(post("/api/metas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestValido())));
        String body2 = """
                {
                  "nome": "Fundo Emergencia",
                  "valorAlvoValor": 5000.00,
                  "valorAlvoMoeda": "BRL",
                  "prazo": "%s"
                }
                """.formatted(PRAZO_FUTURO);
        mockMvc.perform(comAuth(post("/api/metas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body2)));

        mockMvc.perform(comAuth(get("/api/metas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getMetaPorIdRetornaMetaExistente() throws Exception {
        String id = criarMetaRetornaId();

        mockMvc.perform(comAuth(get("/api/metas/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.nome", equalTo("Viagem Europa")));
    }

    @Test
    void getMetaPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/metas/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMetaCancelaMetaEmAndamentoRetorna204() throws Exception {
        String id = criarMetaRetornaId();

        mockMvc.perform(comAuth(delete("/api/metas/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/metas/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("CANCELADA")));
    }

    @Test
    void deleteMetaInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/metas/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void postDepositoIncrementaValorAtualRetorna200() throws Exception {
        String id = criarMetaRetornaId();
        String deposito = """
                {
                  "valor": 2500.00,
                  "moeda": "BRL"
                }
                """;

        mockMvc.perform(comAuth(post("/api/metas/" + id + "/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deposito)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorAtual.valor", equalTo(2500.0)))
                .andExpect(jsonPath("$.status", equalTo("EM_ANDAMENTO")))
                .andExpect(jsonPath("$.percentualConcluido", equalTo(25.0)));
    }

    @Test
    void postDepositoQueAtingeValorAlvoMarcaComoConcluida() throws Exception {
        String id = criarMetaRetornaId();
        String deposito = """
                {
                  "valor": 10000.00,
                  "moeda": "BRL"
                }
                """;

        mockMvc.perform(comAuth(post("/api/metas/" + id + "/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deposito)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("CONCLUIDA")))
                .andExpect(jsonPath("$.percentualConcluido", equalTo(100.0)));
    }

    @Test
    void postDepositoComValorNaoPositivoRetorna400() throws Exception {
        String id = criarMetaRetornaId();
        String deposito = """
                {
                  "valor": 0,
                  "moeda": "BRL"
                }
                """;

        mockMvc.perform(comAuth(post("/api/metas/" + id + "/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deposito)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postDepositoEmMetaInexistenteRetorna404() throws Exception {
        String deposito = """
                {
                  "valor": 100.00,
                  "moeda": "BRL"
                }
                """;

        mockMvc.perform(comAuth(post("/api/metas/" + UUID.randomUUID() + "/depositos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deposito)))
                .andExpect(status().isNotFound());
    }
}
