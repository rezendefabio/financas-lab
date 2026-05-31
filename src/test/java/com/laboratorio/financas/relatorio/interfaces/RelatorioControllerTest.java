package com.laboratorio.financas.relatorio.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

@AutoConfigureMockMvc
class RelatorioControllerTest extends AbstractAuthenticatedIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

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

    private UUID criarConta() {
        Money saldo = new Money(BigDecimal.ZERO, BRL);
        Conta conta = new Conta(
                UUID.randomUUID(),
                authenticatedUserId,
                "Conta " + UUID.randomUUID().toString().substring(0, 8),
                TipoConta.CORRENTE,
                saldo,
                saldo,
                null,
                null,
                null,
                true,
                java.time.Instant.now(),
                null
        );
        contaRepositoryImpl.salvar(conta);
        return conta.getId();
    }

    private UUID criarCategoria(String nome) {
        Categoria categoria = new Categoria(nome, TipoCategoria.DESPESA);
        categoriaRepositoryImpl.salvar(categoria);
        return categoria.getId();
    }

    private void criarTransacao(UUID contaId, UUID categoriaId, String tipo,
                                String valor, String data) throws Exception {
        String body = """
                {
                  "tipo": "%s",
                  "valor": %s,
                  "moeda": "BRL",
                  "data": "%s",
                  "descricao": "transacao teste",
                  "contaId": "%s"
                  %s
                }
                """.formatted(
                tipo, valor, data, contaId,
                categoriaId != null ? ", \"categoriaId\": \"" + categoriaId + "\"" : ""
        );

        mockMvc.perform(comAuth(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/transacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        )).andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("GET /api/relatorios/gastos-por-categoria")
    class GastosPorCategoria {

        @Test
        @DisplayName("retorna 200 com lista vazia quando nao ha transacoes")
        void semTransacoesRetorna200ComListaVazia() throws Exception {
            mockMvc.perform(comAuth(get("/api/relatorios/gastos-por-categoria")
                            .param("dataInicio", "2026-01-01")
                            .param("dataFim", "2026-01-31")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itensPorCategoria").isArray())
                    .andExpect(jsonPath("$.itensPorCategoria.length()").value(0))
                    .andExpect(jsonPath("$.totalGeral.valor").value(0))
                    .andExpect(jsonPath("$.totalGeral.moeda").value("BRL"));
        }

        @Test
        @DisplayName("retorna 200 com gastos agrupados por categoria")
        void comTransacoesRetorna200ComGastosAgrupados() throws Exception {
            UUID contaId = criarConta();
            UUID categoriaId = criarCategoria("Alimentacao");
            criarTransacao(contaId, categoriaId, "DESPESA", "100.00", "2026-01-10");
            criarTransacao(contaId, categoriaId, "DESPESA", "50.00", "2026-01-15");

            mockMvc.perform(comAuth(get("/api/relatorios/gastos-por-categoria")
                            .param("dataInicio", "2026-01-01")
                            .param("dataFim", "2026-01-31")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itensPorCategoria.length()").value(1))
                    .andExpect(jsonPath("$.itensPorCategoria[0].nomeCategoria").value("Alimentacao"))
                    .andExpect(jsonPath("$.itensPorCategoria[0].totalGasto.valor").value(150.00))
                    .andExpect(jsonPath("$.totalGeral.valor").value(150.00));
        }

        @Test
        @DisplayName("retorna 200 filtrando por contaId")
        void comContaIdRetorna200SomenteTransacoesDaConta() throws Exception {
            UUID contaId1 = criarConta();
            UUID contaId2 = criarConta();
            UUID categoriaId = criarCategoria("Transporte");
            criarTransacao(contaId1, categoriaId, "DESPESA", "200.00", "2026-01-05");
            criarTransacao(contaId2, categoriaId, "DESPESA", "100.00", "2026-01-10");

            mockMvc.perform(comAuth(get("/api/relatorios/gastos-por-categoria")
                            .param("dataInicio", "2026-01-01")
                            .param("dataFim", "2026-01-31")
                            .param("contaId", contaId1.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGeral.valor").value(200.00));
        }
    }

    @Nested
    @DisplayName("GET /api/relatorios/dashboard/fluxo-caixa")
    class FluxoCaixa {

        @Test
        @DisplayName("retorna 200 com zeros quando nao ha transacoes no mes")
        void semTransacoesRetorna200ComZeros() throws Exception {
            mockMvc.perform(comAuth(get("/api/relatorios/dashboard/fluxo-caixa")
                            .param("ano", "2026")
                            .param("mes", "1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ano").value(2026))
                    .andExpect(jsonPath("$.mes").value(1))
                    .andExpect(jsonPath("$.totalReceitas").value(0))
                    .andExpect(jsonPath("$.totalDespesas").value(0))
                    .andExpect(jsonPath("$.saldo").value(0))
                    .andExpect(jsonPath("$.moeda").value("BRL"));
        }

        @Test
        @DisplayName("retorna 200 com fluxo calculado quando ha transacoes no mes")
        void comTransacoesRetorna200ComFluxoCalculado() throws Exception {
            UUID contaId = criarConta();
            criarTransacao(contaId, null, "RECEITA", "1000.00", "2026-01-10");
            criarTransacao(contaId, null, "DESPESA", "300.00", "2026-01-15");

            mockMvc.perform(comAuth(get("/api/relatorios/dashboard/fluxo-caixa")
                            .param("ano", "2026")
                            .param("mes", "1")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalReceitas").value(1000.00))
                    .andExpect(jsonPath("$.totalDespesas").value(300.00))
                    .andExpect(jsonPath("$.saldo").value(700.00));
        }

        @Test
        @DisplayName("retorna 400 quando mes invalido (0)")
        void mesInvalidoRetorna400() throws Exception {
            mockMvc.perform(comAuth(get("/api/relatorios/dashboard/fluxo-caixa")
                            .param("ano", "2026")
                            .param("mes", "0")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 400 quando mes invalido (13)")
        void mesAcimaDoLimiteRetorna400() throws Exception {
            mockMvc.perform(comAuth(get("/api/relatorios/dashboard/fluxo-caixa")
                            .param("ano", "2026")
                            .param("mes", "13")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/relatorios/evolucao-saldo")
    class EvolucaoSaldo {

        @Test
        @DisplayName("retorna 200 com zeros quando nao ha transacoes")
        void semTransacoesRetorna200ComZeros() throws Exception {
            mockMvc.perform(comAuth(get("/api/relatorios/evolucao-saldo")
                            .param("dataInicio", "2026-01-01")
                            .param("dataFim", "2026-01-31")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalReceitas.valor").value(0))
                    .andExpect(jsonPath("$.totalDespesas.valor").value(0))
                    .andExpect(jsonPath("$.saldoLiquido.valor").value(0))
                    .andExpect(jsonPath("$.evolucaoPorMes.length()").value(1));
        }

        @Test
        @DisplayName("retorna 200 com evolucao por mes")
        void comTransacoesRetorna200ComEvolucaoPorMes() throws Exception {
            UUID contaId = criarConta();
            criarTransacao(contaId, null, "RECEITA", "1000.00", "2026-01-05");
            criarTransacao(contaId, null, "RECEITA", "500.00", "2026-02-05");
            criarTransacao(contaId, null, "DESPESA", "200.00", "2026-01-10");

            mockMvc.perform(comAuth(get("/api/relatorios/evolucao-saldo")
                            .param("dataInicio", "2026-01-01")
                            .param("dataFim", "2026-02-28")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalReceitas.valor").value(1500.00))
                    .andExpect(jsonPath("$.totalDespesas.valor").value(200.00))
                    .andExpect(jsonPath("$.saldoLiquido.valor").value(1300.00))
                    .andExpect(jsonPath("$.evolucaoPorMes.length()").value(2))
                    .andExpect(jsonPath("$.evolucaoPorMes[0].mes").value("2026-01-01"))
                    .andExpect(jsonPath("$.evolucaoPorMes[0].totalReceitas.valor").value(1000.00))
                    .andExpect(jsonPath("$.evolucaoPorMes[1].mes").value("2026-02-01"))
                    .andExpect(jsonPath("$.evolucaoPorMes[1].totalReceitas.valor").value(500.00));
        }
    }
}
