package com.laboratorio.financas.importacao.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoJpaRepository;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoRepositoryImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;

@AutoConfigureMockMvc
class ImportacaoControllerTest extends AbstractAuthenticatedIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String CABECALHO = "tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId";

    @Autowired
    private TransacaoJpaRepository transacaoJpaRepository;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    @Autowired
    private ContaRepositoryImpl contaRepositoryImpl;

    @Autowired
    private TransacaoRepositoryImpl transacaoRepositoryImpl;

    @AfterEach
    void limpar() {
        transacaoJpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
    }

    private UUID criarContaPersistida() {
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

    private MockMultipartFile csvFile(String conteudo) {
        return new MockMultipartFile("arquivo", "test.csv", "text/csv",
                conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void postCsvValidoRetorna201ComImportadasEZeroFalhas() throws Exception {
        UUID contaId = criarContaPersistida();
        UUID contaDestinoId = criarContaPersistida();

        String csv = CABECALHO + "\n"
                + "RECEITA,1000.00,BRL,2026-05-01,Salario," + contaId + ",,\n"
                + "DESPESA,150.00,BRL,2026-05-01,Supermercado," + contaId + ",,\n"
                + "TRANSFERENCIA,500.00,BRL,2026-05-01,Reserva," + contaId + "," + contaDestinoId + ",\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/csv").file(csvFile(csv))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLinhas", equalTo(3)))
                .andExpect(jsonPath("$.importadas", equalTo(3)))
                .andExpect(jsonPath("$.falhas", equalTo(0)));
    }

    @Test
    void postCsvComLinhaInvalidaRetorna201ComFalha() throws Exception {
        UUID contaId = criarContaPersistida();

        String csv = CABECALHO + "\n"
                + "TIPO_INVALIDO,100.00,BRL,2026-05-01,Erro," + contaId + ",,\n"
                + "RECEITA,200.00,BRL,2026-05-01,Salario," + contaId + ",,\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/csv").file(csvFile(csv))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLinhas", equalTo(2)))
                .andExpect(jsonPath("$.importadas", equalTo(1)))
                .andExpect(jsonPath("$.falhas", equalTo(1)))
                .andExpect(jsonPath("$.erros[0].linha", equalTo(2)));
    }

    @Test
    void postCsvComCabecalhoInvalidoRetorna400() throws Exception {
        String csv = "cabecalho,errado\nDESPESA,100.00\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/csv").file(csvFile(csv))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCsvSomenteHeaderRetorna201ComZeroLinhas() throws Exception {
        String csv = CABECALHO + "\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/csv").file(csvFile(csv))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLinhas", equalTo(0)))
                .andExpect(jsonPath("$.importadas", equalTo(0)))
                .andExpect(jsonPath("$.falhas", equalTo(0)));
    }

    @Test
    void postAnalisarSemDuplicatasRetorna200ComItensValidos() throws Exception {
        UUID contaId = criarContaPersistida();

        String csv = CABECALHO + "\n"
                + "RECEITA,1000.00,BRL,2026-05-01,Salario," + contaId + ",,\n"
                + "DESPESA,150.00,BRL,2026-05-01,Supermercado," + contaId + ",,\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/analisar").file(csvFile(csv))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLinhas", equalTo(2)))
                .andExpect(jsonPath("$.linhasValidas", equalTo(2)))
                .andExpect(jsonPath("$.possivelDuplicatas", equalTo(0)))
                .andExpect(jsonPath("$.errosParsing", equalTo(0)))
                .andExpect(jsonPath("$.itens[0].possivelDuplicata", equalTo(false)))
                .andExpect(jsonPath("$.itens[1].possivelDuplicata", equalTo(false)));
    }

    @Test
    void postAnalisarComLinhaJaExistenteMarcaPossivelDuplicata() throws Exception {
        UUID contaId = criarContaPersistida();
        Transacao existente = transacaoRepositoryImpl.salvar(new Transacao(
                TipoTransacao.DESPESA,
                new Money(new BigDecimal("150.00"), BRL),
                LocalDate.of(2026, 5, 1),
                "Supermercado",
                contaId,
                null,
                authenticatedUserId,
                StatusTransacao.CLEARED,
                null,
                List.of()
        ));

        String csv = CABECALHO + "\n"
                + "RECEITA,1000.00,BRL,2026-05-01,Salario," + contaId + ",,\n"
                + "DESPESA,150.00,BRL,2026-05-01,Supermercado," + contaId + ",,\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/analisar").file(csvFile(csv))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLinhas", equalTo(2)))
                .andExpect(jsonPath("$.linhasValidas", equalTo(2)))
                .andExpect(jsonPath("$.possivelDuplicatas", equalTo(1)))
                .andExpect(jsonPath("$.itens[0].possivelDuplicata", equalTo(false)))
                .andExpect(jsonPath("$.itens[1].possivelDuplicata", equalTo(true)))
                .andExpect(jsonPath("$.itens[1].transacaoExistenteId",
                        equalTo(existente.getId().toString())));
    }

    @Test
    void postAnalisarComCabecalhoInvalidoRetorna400() throws Exception {
        String csv = "cabecalho,errado\nDESPESA,100.00\n";

        mockMvc.perform(comAuth(multipart("/api/importacoes/analisar").file(csvFile(csv))))
                .andExpect(status().isBadRequest());
    }
}
