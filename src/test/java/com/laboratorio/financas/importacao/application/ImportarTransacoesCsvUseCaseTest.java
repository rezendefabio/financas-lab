package com.laboratorio.financas.importacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ImportarTransacoesCsvUseCaseTest {

    private TransacaoRepository transacaoRepository;
    private ImportarTransacoesCsvUseCase useCase;

    @BeforeEach
    void setUp() {
        transacaoRepository = Mockito.mock(TransacaoRepository.class);
        when(transacaoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        useCase = new ImportarTransacoesCsvUseCase(transacaoRepository);
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
    void csvValidoComTresLinhasImportaTodasSemErros() {
        String conta1 = "11111111-0000-0000-0000-000000000001";
        String conta2 = "33333333-0000-0000-0000-000000000003";
        String cat1 = "22222222-0000-0000-0000-000000000002";
        byte[] conteudo = csv(
                "DESPESA,150.00,BRL,2026-05-01,Supermercado," + conta1 + ",," + cat1,
                "RECEITA,3000.00,BRL,2026-05-01,Salario," + conta1 + ",,",
                "TRANSFERENCIA,500.00,BRL,2026-05-01,Reserva mensal," + conta1 + "," + conta2 + ","
        );

        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);

        assertThat(resultado.totalLinhas()).isEqualTo(3);
        assertThat(resultado.importadas()).isEqualTo(3);
        assertThat(resultado.falhas()).isEqualTo(0);
        assertThat(resultado.erros()).isEmpty();
        // TRANSFERENCIA gera par (2 chamadas); DESPESA e RECEITA geram 1 cada -- total 4
        verify(transacaoRepository, times(4)).salvar(any(Transacao.class));
    }

    @Test
    void linhaTipoInvalidoRegistraErroSemAbortar() {
        byte[] conteudo = csv(
                "INVALIDO,100.00,BRL,2026-05-01,Teste,11111111-0000-0000-0000-000000000001,,",
                "RECEITA,200.00,BRL,2026-05-01,Salario,11111111-0000-0000-0000-000000000001,,"
        );

        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);

        assertThat(resultado.totalLinhas()).isEqualTo(2);
        assertThat(resultado.importadas()).isEqualTo(1);
        assertThat(resultado.falhas()).isEqualTo(1);
        assertThat(resultado.erros()).hasSize(1);
        assertThat(resultado.erros().get(0).linha()).isEqualTo(2);
        assertThat(resultado.erros().get(0).motivo()).contains("Tipo invalido");
        verify(transacaoRepository, times(1)).salvar(any(Transacao.class));
    }

    @Test
    void linhaDataInvalidaRegistraErroSemAbortar() {
        byte[] conteudo = csv(
                "DESPESA,100.00,BRL,2026-13-99,Erro data,11111111-0000-0000-0000-000000000001,,",
                "RECEITA,200.00,BRL,2026-05-01,Salario,11111111-0000-0000-0000-000000000001,,"
        );

        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);

        assertThat(resultado.totalLinhas()).isEqualTo(2);
        assertThat(resultado.importadas()).isEqualTo(1);
        assertThat(resultado.falhas()).isEqualTo(1);
        assertThat(resultado.erros().get(0).motivo()).contains("Data invalida");
    }

    @Test
    void cabecalhoInvalidoLancaIllegalArgumentException() {
        byte[] conteudo = "cabecalho,errado\nDESPESA,100.00\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> useCase.importar(conteudo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cabecalho CSV invalido");

        verify(transacaoRepository, never()).salvar(any());
    }

    @Test
    void csvSomenteHeaderRetornaZeroLinhas() {
        byte[] conteudo = "tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId\n"
                .getBytes(StandardCharsets.UTF_8);

        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);

        assertThat(resultado.totalLinhas()).isEqualTo(0);
        assertThat(resultado.importadas()).isEqualTo(0);
        assertThat(resultado.falhas()).isEqualTo(0);
        verify(transacaoRepository, never()).salvar(any());
    }

    @Test
    void transferenciaSemContaDestinoIdRegistraErro() {
        byte[] conteudo = csv(
                "TRANSFERENCIA,500.00,BRL,2026-05-01,Sem destino,11111111-0000-0000-0000-000000000001,,"
        );

        ImportarTransacoesCsvUseCase.Resultado resultado = useCase.importar(conteudo);

        assertThat(resultado.totalLinhas()).isEqualTo(1);
        assertThat(resultado.importadas()).isEqualTo(0);
        assertThat(resultado.falhas()).isEqualTo(1);
        assertThat(resultado.erros().get(0).motivo()).contains("TRANSFERENCIA exige contaDestinoId");
        verify(transacaoRepository, never()).salvar(any());
    }
}
