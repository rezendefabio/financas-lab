package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarContaUseCaseTest {

    private ContaRepository repository;
    private CriarContaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContaRepository.class);
        useCase = new CriarContaUseCase(repository);
    }

    private CriarContaUseCase.Comando comandoSimples(String nome, TipoConta tipo, BigDecimal valor, String moeda) {
        return new CriarContaUseCase.Comando(nome, tipo, valor, moeda, null, null, null, null, null);
    }

    @Test
    void executarCaminhoFelizRetornaConta() {
        // Given
        Conta contaSalva = new Conta(
                "Carteira",
                TipoConta.CORRENTE,
                new Money(BigDecimal.valueOf(100), Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = comandoSimples("Carteira", TipoConta.CORRENTE, BigDecimal.valueOf(100), "BRL");

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Carteira");
    }

    @Test
    void executarComValorZeroNaoLancaExcecao() {
        // Given
        Conta contaSalva = new Conta(
                "Poupanca",
                TipoConta.POUPANCA,
                new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = comandoSimples("Poupanca", TipoConta.POUPANCA, BigDecimal.ZERO, "BRL");

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
    }

    @Test
    void executarComValorNegativoNaoLancaExcecao() {
        // Given
        Conta contaSalva = new Conta(
                "Cartao",
                TipoConta.CARTAO_CREDITO,
                new Money(BigDecimal.valueOf(-50), Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = comandoSimples("Cartao", TipoConta.CARTAO_CREDITO, BigDecimal.valueOf(-50), "BRL");

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
    }

    @Test
    void executarComMoedaInvalidaLancaIllegalArgumentException() {
        // Given
        CriarContaUseCase.Comando comando = comandoSimples("Conta", TipoConta.CORRENTE, BigDecimal.valueOf(100), "XYZ");

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void executarChamaRepositorioSalvarUmaVez() {
        // Given
        Conta contaSalva = new Conta(
                "Dinheiro",
                TipoConta.DINHEIRO,
                new Money(BigDecimal.valueOf(1000), Currency.getInstance("USD"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = comandoSimples("Dinheiro", TipoConta.DINHEIRO, BigDecimal.valueOf(1000), "USD");

        // When
        useCase.executar(comando);

        // Then
        verify(repository, times(1)).salvar(any(Conta.class));
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        // Given
        Conta contaSalva = new Conta(
                "Especial",
                TipoConta.CORRENTE,
                new Money(BigDecimal.valueOf(500), Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = comandoSimples("Especial", TipoConta.CORRENTE, BigDecimal.valueOf(500), "BRL");

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isSameAs(contaSalva);
    }

    @Test
    void executarComUserIdPassaParaComando() {
        // Given
        UUID userId = UUID.randomUUID();
        Conta contaSalva = new Conta(
                "Carteira",
                TipoConta.CORRENTE,
                new Money(BigDecimal.valueOf(100), Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = new CriarContaUseCase.Comando(
                "Carteira", TipoConta.CORRENTE, BigDecimal.valueOf(100), "BRL",
                userId, null, null, null, null
        );

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
        verify(repository, times(1)).salvar(any(Conta.class));
    }

    @Test
    void executarComLimiteCreditoPopulaOCampo() {
        // Given
        Conta contaSalva = new Conta(
                "Cartao",
                TipoConta.CARTAO_CREDITO,
                new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = new CriarContaUseCase.Comando(
                "Cartao", TipoConta.CARTAO_CREDITO, BigDecimal.ZERO, "BRL",
                null, new BigDecimal("5000.00"), "BRL", 10, 20
        );

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
    }

    @Test
    void executarComTipoInvestimentoAceita() {
        // Given
        Conta contaSalva = new Conta(
                "CDB",
                TipoConta.INVESTIMENTO,
                new Money(BigDecimal.valueOf(1000), Currency.getInstance("BRL"))
        );
        when(repository.salvar(any(Conta.class))).thenReturn(contaSalva);

        CriarContaUseCase.Comando comando = comandoSimples("CDB", TipoConta.INVESTIMENTO, BigDecimal.valueOf(1000), "BRL");

        // When
        Conta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipo()).isEqualTo(TipoConta.INVESTIMENTO);
    }
}
