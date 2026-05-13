package com.laboratorio.financas.importacao.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaRepositoryImpl;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.infrastructure.persistence.TransacaoJpaRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ImportacaoControllerTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String CABECALHO = "tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransacaoJpaRepository transacaoJpaRepository;

    @Autowired
    private ContaJpaRepository contaJpaRepository;

    @Autowired
    private ContaRepositoryImpl contaRepositoryImpl;

    @AfterEach
    void limpar() {
        transacaoJpaRepository.deleteAll();
        contaJpaRepository.deleteAll();
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

        mockMvc.perform(multipart("/api/importacoes/csv").file(csvFile(csv)))
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

        mockMvc.perform(multipart("/api/importacoes/csv").file(csvFile(csv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLinhas", equalTo(2)))
                .andExpect(jsonPath("$.importadas", equalTo(1)))
                .andExpect(jsonPath("$.falhas", equalTo(1)))
                .andExpect(jsonPath("$.erros[0].linha", equalTo(2)));
    }

    @Test
    void postCsvComCabecalhoInvalidoRetorna400() throws Exception {
        String csv = "cabecalho,errado\nDESPESA,100.00\n";

        mockMvc.perform(multipart("/api/importacoes/csv").file(csvFile(csv)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCsvSomenteHeaderRetorna201ComZeroLinhas() throws Exception {
        String csv = CABECALHO + "\n";

        mockMvc.perform(multipart("/api/importacoes/csv").file(csvFile(csv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalLinhas", equalTo(0)))
                .andExpect(jsonPath("$.importadas", equalTo(0)))
                .andExpect(jsonPath("$.falhas", equalTo(0)));
    }
}
