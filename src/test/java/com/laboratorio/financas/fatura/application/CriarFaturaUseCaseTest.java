package com.laboratorio.financas.fatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CriarFaturaUseCaseTest {

    private FaturaRepository repository;
    private CriarFaturaUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONTA_ID = UUID.randomUUID();
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 6, 10);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FaturaRepository.class);
        useCase = new CriarFaturaUseCase(repository);
    }

    @Test
    void executarSemValorTotalCriaFaturaComValorNulo() {
        when(repository.salvar(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarFaturaUseCase.Comando comando = new CriarFaturaUseCase.Comando(
                USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null, null);
        Fatura resultado = useCase.executar(comando);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Cartao");
        assertThat(resultado.getValorTotal()).isNull();
    }

    @Test
    void executarComValorTotalConstroiMoney() {
        when(repository.salvar(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarFaturaUseCase.Comando comando = new CriarFaturaUseCase.Comando(
                USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, new BigDecimal("1500.00"), "BRL");
        Fatura resultado = useCase.executar(comando);

        assertThat(resultado.getValorTotal()).isNotNull();
        assertThat(resultado.getValorTotal().valor()).isEqualByComparingTo("1500.00");
        assertThat(resultado.getValorTotal().moeda().getCurrencyCode()).isEqualTo("BRL");
    }

    @Test
    void executarChamaRepositorioSalvarUmaVez() {
        when(repository.salvar(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(new CriarFaturaUseCase.Comando(
                USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null, null));

        verify(repository, times(1)).salvar(any(Fatura.class));
    }

    @Test
    void executarPassaUserIdEContaIdParaDominio() {
        ArgumentCaptor<Fatura> captor = ArgumentCaptor.forClass(Fatura.class);
        when(repository.salvar(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.executar(new CriarFaturaUseCase.Comando(
                USER_ID, CONTA_ID, "Cartao", VENCIMENTO, null, null, null));

        verify(repository).salvar(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getContaId()).isEqualTo(CONTA_ID);
    }
}
