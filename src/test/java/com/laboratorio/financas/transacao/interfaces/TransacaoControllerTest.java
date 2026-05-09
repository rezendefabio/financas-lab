package com.laboratorio.financas.transacao.interfaces;

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
import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaJpaRepository;
import com.laboratorio.financas.categoria.infrastructure.persistence.CategoriaRepositoryImpl;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoJpaRepository;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class TransacaoControllerTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String DATA = "2025-01-15";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransacaoJpaRepository transacaoJpaRepository;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    @Autowired
    private CategoriaJpaRepository categoriaJpaRepository;

    @Autowired
    private ContaRepositoryImpl contaRepositoryImpl;

    @Autowired
    private CategoriaRepositoryImpl categoriaRepositoryImpl;

    @AfterEach
    void limpar() {
        transacaoJpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
        categoriaJpaRepository.deleteAll();
    }

    private UUID criarContaPersistida() {
        Conta conta = new Conta(
                "Conta " + UUID.randomUUID().toString().substring(0, 8),
                TipoConta.CORRENTE,
                new Money(BigDecimal.ZERO, BRL)
        );
        contaRepositoryImpl.salvar(conta);
        return conta.getId();
    }

    private UUID criarCategoriaPersistida(TipoCategoria tipo) {
        Categoria cat = new Categoria("Categoria " + tipo, tipo);
        categoriaRepositoryImpl.salvar(cat);
        return cat.getId();
    }

    private Map<String, Object> requestReceita(UUID contaId) {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", "RECEITA");
        body.put("valor", BigDecimal.valueOf(100));
        body.put("moeda", "BRL");
        body.put("data", DATA);
        body.put("descricao", "Salario");
        body.put("contaId", contaId.toString());
        return body;
    }

    private Map<String, Object> requestTransferencia(UUID contaId, UUID contaDestinoId) {
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", "TRANSFERENCIA");
        body.put("valor", BigDecimal.valueOf(50));
        body.put("moeda", "BRL");
        body.put("data", DATA);
        body.put("descricao", "Transferencia");
        body.put("contaId", contaId.toString());
        body.put("contaDestinoId", contaDestinoId.toString());
        return body;
    }

    // POST tests

    @Test
    void postTransacaoReceitaValidaRetorna201() throws Exception {
        UUID contaId = criarContaPersistida();

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipo", equalTo("RECEITA")))
                .andExpect(jsonPath("$.contaId", equalTo(contaId.toString())));
    }

    @Test
    void postTransacaoTransferenciaValidaRetorna201() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTransferencia(contaId, contaDestinoId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo", equalTo("TRANSFERENCIA")));
    }

    @Test
    void postTransacaoComDescricaoBlankRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestReceita(contaId);
        body.put("descricao", "   ");

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransacaoComValorZeroRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestReceita(contaId);
        body.put("valor", BigDecimal.ZERO);

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransacaoComContaIdInexistenteRetorna400ComRecurso() throws Exception {
        Map<String, Object> body = requestReceita(UUID.randomUUID());

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("conta")));
    }

    @Test
    void postTransferenciaComContaDestinoInexistenteRetorna400ComRecurso() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestTransferencia(contaId, UUID.randomUUID());

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("contaDestino")));
    }

    @Test
    void postTransacaoComCategoriaIdInexistenteRetorna400ComRecurso() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestReceita(contaId);
        body.put("categoriaId", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("categoria")));
    }

    @Test
    void postTransferenciaSemContaDestinoRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = new HashMap<>();
        body.put("tipo", "TRANSFERENCIA");
        body.put("valor", BigDecimal.valueOf(100));
        body.put("moeda", "BRL");
        body.put("data", DATA);
        body.put("descricao", "Transferencia invalida");
        body.put("contaId", contaId.toString());

        mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // GET tests

    @Test
    void getTransacoesSemFiltrosRetorna200Paginado() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId))));

        mockMvc.perform(get("/api/transacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComSizeMaiorQueMaxRetorna400() throws Exception {
        mockMvc.perform(get("/api/transacoes").param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComSizeZeroRetorna400() throws Exception {
        mockMvc.perform(get("/api/transacoes").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComFiltroContaIdFiltraOrigem() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID outraContaId = criarContaPersistida();
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId))));
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(outraContaId))));

        mockMvc.perform(get("/api/transacoes").param("contaId", contaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].contaId", equalTo(contaId.toString())));
    }

    @Test
    void getTransacoesComFiltroContaIdFiltraDestinoEmTransferencia() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestTransferencia(contaId, contaDestinoId))));

        mockMvc.perform(get("/api/transacoes").param("contaId", contaDestinoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltroPeriodoFiltraData() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId))));

        mockMvc.perform(get("/api/transacoes")
                        .param("dataInicio", "2025-01-01")
                        .param("dataFim", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltroTipoFiltraTipo() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId))));

        Map<String, Object> despesa = requestReceita(contaId);
        despesa.put("tipo", "DESPESA");
        despesa.put("descricao", "Compra");
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(despesa)));

        mockMvc.perform(get("/api/transacoes").param("tipo", "RECEITA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltroCategoriaIdFiltraCategoria() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        Map<String, Object> body = requestReceita(contaId);
        body.put("categoriaId", categoriaId.toString());
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId))));

        mockMvc.perform(get("/api/transacoes").param("categoriaId", categoriaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltrosCombinados() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId))));

        mockMvc.perform(get("/api/transacoes")
                        .param("contaId", contaId.toString())
                        .param("tipo", "RECEITA")
                        .param("dataInicio", "2025-01-01")
                        .param("dataFim", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComTipoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/transacoes").param("tipo", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacaoPorIdExistenteRetorna200() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/transacoes/" + idStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(idStr)));
    }

    @Test
    void getTransacaoPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/transacoes/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    // PUT tests

    @Test
    void putAtualizaTransacaoExistenteRetorna200() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();
        String criadoEmStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("criadoEm").asText();

        Map<String, Object> update = requestReceita(contaId);
        update.put("descricao", "Salario atualizado");
        update.put("valor", BigDecimal.valueOf(200));

        MvcResult putResult = mockMvc.perform(put("/api/transacoes/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario atualizado")))
                .andReturn();

        String criadoEmAtualizado = objectMapper.readTree(putResult.getResponse().getContentAsString())
                .get("criadoEm").asText();
        org.assertj.core.api.Assertions.assertThat(criadoEmAtualizado).isEqualTo(criadoEmStr);
    }

    @Test
    void putEmIdInexistenteRetorna404() throws Exception {
        UUID contaId = criarContaPersistida();

        mockMvc.perform(put("/api/transacoes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void putComFkInvalidaRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        Map<String, Object> update = requestReceita(UUID.randomUUID());

        mockMvc.perform(put("/api/transacoes/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("conta")));
    }

    // DELETE tests

    @Test
    void deleteExistenteRetorna204() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/transacoes/" + idStr))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/transacoes/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // Ciclo completo

    @Test
    void cicloCompletoPostGetPutGetDeleteGet() throws Exception {
        UUID contaId = criarContaPersistida();

        // POST
        MvcResult postResult = mockMvc.perform(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .get("id").asText();

        // GET por id
        mockMvc.perform(get("/api/transacoes/" + idStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario")));

        // PUT
        Map<String, Object> update = requestReceita(contaId);
        update.put("descricao", "Salario revisado");
        mockMvc.perform(put("/api/transacoes/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario revisado")));

        // GET mostra atualizacao
        mockMvc.perform(get("/api/transacoes/" + idStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario revisado")));

        // DELETE
        mockMvc.perform(delete("/api/transacoes/" + idStr))
                .andExpect(status().isNoContent());

        // GET retorna 404
        mockMvc.perform(get("/api/transacoes/" + idStr))
                .andExpect(status().isNotFound());
    }
}
