package com.laboratorio.financas.importacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AnalisarCsvUseCaseTest {

    private TransacaoRepository transacaoRepository;
    private AnalisarCsvUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        useCase = new AnalisarCsvUseCase(transacaoRepository);
    }

    private byte[] csv(String... linhas) {
        StringBuilder sb = new StringBuilder();
        sb.append("tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId\n");
        for (String linha : linhas) {
            sb.append(linha).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void csvValidoSemDuplicatasMarcaPossivelDuplicatasZero() {
        when(transacaoRepository.listarComFiltros(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        String conta1 = "11111111-0000-0000-0000-000000000001";
        byte[] conteudo = csv(
                "DESPESA,150.00,BRL,2026-05-01,Supermercado," + conta1 + ",,",
                "RECEITA,3000.00,BRL,2026-05-01,Salario," + conta1 + ",,"
        );

        AnalisarCsvUseCase.Resultado r = useCase.analisar(conteudo);

        assertThat(r.totalLinhas()).isEqualTo(2);
        assertThat(r.linhasValidas()).isEqualTo(2);
        assertThat(r.possivelDuplicatas()).isZero();
        assertThat(r.errosParsing()).isZero();
        assertThat(r.itens()).hasSize(2);
        assertThat(r.itens()).allMatch(i -> !i.possivelDuplicata());
        assertThat(r.itens().get(0).transacaoExistenteId()).isNull();
        assertThat(r.itens().get(0).linhaCsvOriginal())
                .isEqualTo("DESPESA,150.00,BRL,2026-05-01,Supermercado," + conta1 + ",,");
    }

    @Test
    void csvComLinhaCorrespondenteMarcaPossivelDuplicata() {
        UUID contaId = UUID.fromString("11111111-0000-0000-0000-000000000001");
        UUID existenteId = UUID.randomUUID();
        Transacao existente = new Transacao(
                TipoTransacao.DESPESA,
                new Money(new BigDecimal("150.00"), Currency.getInstance("BRL")),
                LocalDate.of(2026, 5, 1),
                "Supermercado anterior",
                contaId,
                null,
                null,
                StatusTransacao.CLEARED,
                null,
                List.of()
        );
        // injetar id conhecido via reconstrucao
        Transacao existenteComId = new Transacao(
                existenteId,
                existente.getTipo(),
                existente.getValor(),
                existente.getData(),
                existente.getDescricao(),
                existente.getContaId(),
                null,
                existente.getCriadoEm(),
                existente.getAtualizadoEm(),
                null,
                StatusTransacao.CLEARED,
                null,
                null,
                null,
                null,
                List.of()
        );
        when(transacaoRepository.listarComFiltros(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existenteComId)));

        byte[] conteudo = csv(
                "DESPESA,150.00,BRL,2026-05-01,Supermercado," + contaId + ",,"
        );

        AnalisarCsvUseCase.Resultado r = useCase.analisar(conteudo);

        assertThat(r.linhasValidas()).isEqualTo(1);
        assertThat(r.possivelDuplicatas()).isEqualTo(1);
        assertThat(r.itens()).hasSize(1);
        assertThat(r.itens().get(0).possivelDuplicata()).isTrue();
        assertThat(r.itens().get(0).transacaoExistenteId()).isEqualTo(existenteId);
    }

    @Test
    void csvComMesmoValorMasMoedaDiferenteNaoMarcaDuplicata() {
        UUID contaId = UUID.fromString("11111111-0000-0000-0000-000000000001");
        Transacao existenteUsd = new Transacao(
                UUID.randomUUID(),
                TipoTransacao.DESPESA,
                new Money(new BigDecimal("150.00"), Currency.getInstance("USD")),
                LocalDate.of(2026, 5, 1),
                "Compra em dolar",
                contaId,
                null,
                java.time.Instant.now(),
                null,
                null,
                StatusTransacao.CLEARED,
                null,
                null,
                null,
                null,
                List.of()
        );
        when(transacaoRepository.listarComFiltros(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existenteUsd)));

        byte[] conteudo = csv(
                "DESPESA,150.00,BRL,2026-05-01,Supermercado," + contaId + ",,"
        );

        AnalisarCsvUseCase.Resultado r = useCase.analisar(conteudo);

        assertThat(r.linhasValidas()).isEqualTo(1);
        assertThat(r.possivelDuplicatas()).isZero();
        assertThat(r.itens().get(0).possivelDuplicata()).isFalse();
        assertThat(r.itens().get(0).transacaoExistenteId()).isNull();
    }

    @Test
    void csvComCabecalhoInvalidoLancaIllegalArgumentException() {
        byte[] conteudo = "coluna1,coluna2\nvalor1,valor2\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> useCase.analisar(conteudo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cabecalho CSV invalido");
    }

    @Test
    void csvComLinhaMalFormadaRegistraErroParsing() {
        when(transacaoRepository.listarComFiltros(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        String conta1 = "11111111-0000-0000-0000-000000000001";
        byte[] conteudo = csv(
                "DESPESA,abc,BRL,2026-05-01,Mercado," + conta1 + ",,"
        );

        AnalisarCsvUseCase.Resultado r = useCase.analisar(conteudo);

        assertThat(r.errosParsing()).isEqualTo(1);
        assertThat(r.linhasValidas()).isZero();
        assertThat(r.totalLinhas()).isEqualTo(1);
        assertThat(r.erros()).hasSize(1);
        assertThat(r.erros().get(0).motivo()).contains("Valor invalido");
    }
}
