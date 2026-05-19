package com.laboratorio.financas.transacao.interfaces;

import static org.hamcrest.Matchers.equalTo;
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
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
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
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class TransacaoControllerTest extends AbstractAuthenticatedIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String DATA = "2025-01-15";

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

    @Test
    void postTransacaoReceitaValidaRetorna201() throws Exception {
        UUID contaId = criarContaPersistida();

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipo", equalTo("RECEITA")))
                .andExpect(jsonPath("$.contaId", equalTo(contaId.toString())))
                .andExpect(jsonPath("$.status", equalTo("CLEARED")));
    }

    @Test
    void postTransacaoTransferenciaValidaRetorna201ECriaPar() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestTransferencia(contaId, contaDestinoId)))))
                .andExpect(status().isCreated())
                // Retorna a despesa (conta origem)
                .andExpect(jsonPath("$.tipo", equalTo("DESPESA")))
                .andExpect(jsonPath("$.contaId", equalTo(contaId.toString())))
                .andExpect(jsonPath("$.transferGroupId", notNullValue()));

        // Par criado: 2 transacoes no banco
        org.assertj.core.api.Assertions.assertThat(transacaoJpaRepository.count()).isEqualTo(2L);
    }

    @Test
    void postTransacaoComDescricaoBlankRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestReceita(contaId);
        body.put("descricao", "   ");

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransacaoComValorZeroRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestReceita(contaId);
        body.put("valor", BigDecimal.ZERO);

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransacaoComContaIdInexistenteRetorna400ComRecurso() throws Exception {
        Map<String, Object> body = requestReceita(UUID.randomUUID());

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("conta")));
    }

    @Test
    void postTransferenciaComContaDestinoInexistenteRetorna400ComRecurso() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestTransferencia(contaId, UUID.randomUUID());

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("contaDestino")));
    }

    @Test
    void postTransacaoComCategoriaIdInexistenteRetorna400ComRecurso() throws Exception {
        UUID contaId = criarContaPersistida();
        Map<String, Object> body = requestReceita(contaId);
        body.put("categoriaId", UUID.randomUUID().toString());

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
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

        mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesSemFiltrosRetorna200Paginado() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComSizeMaiorQueMaxRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes").param("size", "200")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComSizeZeroRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes").param("size", "0")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComFiltroContaIdFiltraOrigem() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID outraContaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(outraContaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("contaId", contaId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].contaId", equalTo(contaId.toString())));
    }

    @Test
    void getTransacoesComFiltroPeriodoFiltraData() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes")
                        .param("dataInicio", "2025-01-01")
                        .param("dataFim", "2025-01-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesSemFiltroDeDateRetornaTodasIndependenteDaData() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        Map<String, Object> outraData = requestReceita(contaId);
        outraData.put("data", "2024-06-01");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outraData))));

        mockMvc.perform(comAuth(get("/api/transacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(2)));
    }

    @Test
    void getTransacoesSemDataInicioPegaTransacoesAnterioresAoFim() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        Map<String, Object> outraData = requestReceita(contaId);
        outraData.put("data", "2024-06-01");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outraData))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("dataFim", "2025-01-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(2)));
    }

    @Test
    void getTransacoesSemDataFimPegaTransacoesAposInicio() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        Map<String, Object> outraData = requestReceita(contaId);
        outraData.put("data", "2024-06-01");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outraData))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("dataInicio", "2025-01-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltroTipoFiltraTipo() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        Map<String, Object> despesa = requestReceita(contaId);
        despesa.put("tipo", "DESPESA");
        despesa.put("descricao", "Compra");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(despesa))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("tipo", "RECEITA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltroCategoriaIdFiltraCategoria() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID categoriaId = criarCategoriaPersistida(TipoCategoria.RECEITA);

        Map<String, Object> body = requestReceita(contaId);
        body.put("categoriaId", categoriaId.toString());
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))));

        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("categoriaId", categoriaId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComFiltrosCombinados() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes")
                        .param("contaId", contaId.toString())
                        .param("tipo", "RECEITA")
                        .param("dataInicio", "2025-01-01")
                        .param("dataFim", "2025-01-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComTipoInvalidoRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes").param("tipo", "INVALIDO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComFiltroStatusFiltraStatus() throws Exception {
        UUID contaId = criarContaPersistida();

        Map<String, Object> compensada = requestReceita(contaId);
        compensada.put("status", "CLEARED");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(compensada))));

        Map<String, Object> pendente = requestReceita(contaId);
        pendente.put("status", "PENDING");
        pendente.put("descricao", "Pendente");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pendente))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("status", "PENDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].status", equalTo("PENDING")));
    }

    @Test
    void getTransacoesComStatusInvalidoRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes").param("status", "INVALIDO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComSortValidoOrdenaPorCampo() throws Exception {
        UUID contaId = criarContaPersistida();

        Map<String, Object> menor = requestReceita(contaId);
        menor.put("valor", BigDecimal.valueOf(10));
        menor.put("descricao", "Menor");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(menor))));

        Map<String, Object> maior = requestReceita(contaId);
        maior.put("valor", BigDecimal.valueOf(900));
        maior.put("descricao", "Maior");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maior))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("sort", "valor:asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao", equalTo("Menor")))
                .andExpect(jsonPath("$.content[1].descricao", equalTo("Maior")));

        mockMvc.perform(comAuth(get("/api/transacoes").param("sort", "valor:desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao", equalTo("Maior")))
                .andExpect(jsonPath("$.content[1].descricao", equalTo("Menor")));
    }

    @Test
    void getTransacoesSemSortUsaOrdenacaoPadrao() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacoesComSortCampoForaDaWhitelistRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes").param("sort", "contaId:asc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComSortDirecaoInvalidaRetorna400() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes").param("sort", "data:cima")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransacoesComSortSemDirecaoUsaDescPadrao() throws Exception {
        UUID contaId = criarContaPersistida();
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestReceita(contaId)))));

        mockMvc.perform(comAuth(get("/api/transacoes").param("sort", "data")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void getTransacaoPorIdExistenteRetorna200() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(comAuth(get("/api/transacoes/" + idStr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(idStr)));
    }

    @Test
    void getTransacaoPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/transacoes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void putAtualizaTransacaoExistenteRetorna200() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        Map<String, Object> update = requestReceita(contaId);
        update.put("descricao", "Salario atualizado");
        update.put("valor", BigDecimal.valueOf(200));

        mockMvc.perform(comAuth(put("/api/transacoes/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario atualizado")))
                .andExpect(jsonPath("$.criadoEm", notNullValue()))
                .andExpect(jsonPath("$.atualizadoEm", notNullValue()));
    }

    @Test
    void putEmIdInexistenteRetorna404() throws Exception {
        UUID contaId = criarContaPersistida();

        mockMvc.perform(comAuth(put("/api/transacoes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void putComFkInvalidaRetorna400() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        Map<String, Object> update = requestReceita(UUID.randomUUID());

        mockMvc.perform(comAuth(put("/api/transacoes/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.recurso", equalTo("conta")));
    }

    @Test
    void deleteSoftDeleteRetorna204ETransacaoSomeNaBusca() throws Exception {
        UUID contaId = criarContaPersistida();
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asText();

        // Soft delete retorna 204
        mockMvc.perform(comAuth(delete("/api/transacoes/" + idStr)))
                .andExpect(status().isNoContent());

        // Transacao some da busca
        mockMvc.perform(comAuth(get("/api/transacoes/" + idStr)))
                .andExpect(status().isNotFound());

        // Registro ainda existe no banco (soft delete)
        org.assertj.core.api.Assertions.assertThat(transacaoJpaRepository.count()).isEqualTo(1L);
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/transacoes/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSoftDeleteTransacaoNaoAparecaNaListagem() throws Exception {
        UUID contaId = criarContaPersistida();

        // Cria 2 transacoes
        MvcResult r1 = mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andReturn();
        Map<String, Object> outra = requestReceita(contaId);
        outra.put("descricao", "Outra");
        mockMvc.perform(comAuth(post("/api/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outra))));

        String idStr = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asText();

        // Soft delete na primeira
        mockMvc.perform(comAuth(delete("/api/transacoes/" + idStr)))
                .andExpect(status().isNoContent());

        // Listagem retorna apenas 1
        mockMvc.perform(comAuth(get("/api/transacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void cicloCompletoPostGetPutGetDeleteGet() throws Exception {
        UUID contaId = criarContaPersistida();

        MvcResult postResult = mockMvc.perform(comAuth(post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestReceita(contaId)))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(comAuth(get("/api/transacoes/" + idStr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario")));

        Map<String, Object> update = requestReceita(contaId);
        update.put("descricao", "Salario revisado");
        mockMvc.perform(comAuth(put("/api/transacoes/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario revisado")));

        mockMvc.perform(comAuth(get("/api/transacoes/" + idStr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao", equalTo("Salario revisado")));

        // Soft delete
        mockMvc.perform(comAuth(delete("/api/transacoes/" + idStr)))
                .andExpect(status().isNoContent());

        // GET retorna 404 apos soft delete
        mockMvc.perform(comAuth(get("/api/transacoes/" + idStr)))
                .andExpect(status().isNotFound());
    }
}
