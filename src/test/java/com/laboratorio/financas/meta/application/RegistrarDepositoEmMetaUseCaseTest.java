package com.laboratorio.financas.meta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaNaoEncontradaException;
import com.laboratorio.financas.meta.domain.MetaRepository;
import com.laboratorio.financas.meta.domain.StatusMeta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RegistrarDepositoEmMetaUseCaseTest {

    private MetaRepository repository;
    private RegistrarDepositoEmMetaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(MetaRepository.class);
        useCase = new RegistrarDepositoEmMetaUseCase(repository);
    }

    @Test
    @DisplayName("executar: caminho feliz busca meta, registra deposito e chama atualizar")
    void executarCaminhoFelizBuscaRegistraEAtualiza() {
        // Given
        UUID metaId = UUID.randomUUID();
        Meta meta = new Meta(
                "Viagem",
                new Money(BigDecimal.valueOf(5000), Currency.getInstance("BRL")),
                LocalDate.now().plusMonths(6)
        );
        Meta metaAtualizada = new Meta(
                "Viagem",
                new Money(BigDecimal.valueOf(5000), Currency.getInstance("BRL")),
                LocalDate.now().plusMonths(6)
        );
        when(repository.buscarPorId(metaId)).thenReturn(Optional.of(meta));
        when(repository.atualizar(any(Meta.class))).thenReturn(metaAtualizada);

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(1000), "BRL"
        );

        // When
        Meta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isNotNull();
        verify(repository, times(1)).buscarPorId(metaId);
        verify(repository, times(1)).atualizar(any(Meta.class));
    }

    @Test
    @DisplayName("executar: lanca MetaNaoEncontradaException quando meta nao existe")
    void executarLancaMetaNaoEncontradaExceptionQuandoIdNaoExiste() {
        // Given
        UUID metaId = UUID.randomUUID();
        when(repository.buscarPorId(metaId)).thenReturn(Optional.empty());

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(500), "BRL"
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(MetaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("executar: nao chama atualizar quando meta nao encontrada")
    void executarNaoChamaAtualizarQuandoMetaNaoEncontrada() {
        // Given
        UUID metaId = UUID.randomUUID();
        when(repository.buscarPorId(metaId)).thenReturn(Optional.empty());

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(500), "BRL"
        );

        // When
        try {
            useCase.executar(comando);
        } catch (MetaNaoEncontradaException e) {
            // esperado
        }

        // Then
        verify(repository, times(0)).atualizar(any(Meta.class));
    }

    @Test
    @DisplayName("executar: retorna meta retornada pelo repositorio apos atualizacao")
    void executarRetornaMetaRetornadaPeloRepositorio() {
        // Given
        UUID metaId = UUID.randomUUID();
        Meta meta = new Meta(
                "Reserva de emergencia",
                new Money(BigDecimal.valueOf(10000), Currency.getInstance("BRL")),
                LocalDate.now().plusYears(1)
        );
        Meta metaComDeposito = new Meta(
                "Reserva de emergencia",
                new Money(BigDecimal.valueOf(10000), Currency.getInstance("BRL")),
                LocalDate.now().plusYears(1)
        );
        when(repository.buscarPorId(metaId)).thenReturn(Optional.of(meta));
        when(repository.atualizar(any(Meta.class))).thenReturn(metaComDeposito);

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(2000), "BRL"
        );

        // When
        Meta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado).isSameAs(metaComDeposito);
    }

    @Test
    @DisplayName("executar: lanca IllegalStateException quando meta nao esta em andamento")
    void executarLancaIllegalStateExceptionQuandoMetaNaoEstaEmAndamento() {
        // Given
        UUID metaId = UUID.randomUUID();
        Meta meta = new Meta(
                "Notebook",
                new Money(BigDecimal.valueOf(3000), Currency.getInstance("BRL")),
                LocalDate.now().plusMonths(3)
        );
        meta.cancelar();
        when(repository.buscarPorId(metaId)).thenReturn(Optional.of(meta));

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(500), "BRL"
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Meta nao esta em andamento");
    }

    @Test
    @DisplayName("executar: lanca IllegalArgumentException quando moeda do deposito difere da meta")
    void executarLancaIllegalArgumentExceptionQuandoMoedaDiferente() {
        // Given
        UUID metaId = UUID.randomUUID();
        Meta meta = new Meta(
                "Carro",
                new Money(BigDecimal.valueOf(50000), Currency.getInstance("BRL")),
                LocalDate.now().plusYears(2)
        );
        when(repository.buscarPorId(metaId)).thenReturn(Optional.of(meta));

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(1000), "USD"
        );

        // When / Then
        assertThatThrownBy(() -> useCase.executar(comando))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("executar: deposito que atinge valorAlvo muda status para CONCLUIDA antes de atualizar")
    void executarDepositoQueAtingeValorAlvoConcluidMeta() {
        // Given
        UUID metaId = UUID.randomUUID();
        Meta meta = new Meta(
                "Fundo de viagem",
                new Money(BigDecimal.valueOf(1000), Currency.getInstance("BRL")),
                LocalDate.now().plusMonths(1)
        );
        // Captura a meta apos registrarDeposito para verificar status
        when(repository.buscarPorId(metaId)).thenReturn(Optional.of(meta));
        when(repository.atualizar(any(Meta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                metaId, BigDecimal.valueOf(1000), "BRL"
        );

        // When
        Meta resultado = useCase.executar(comando);

        // Then
        assertThat(resultado.getStatus()).isEqualTo(StatusMeta.CONCLUIDA);
    }
}
