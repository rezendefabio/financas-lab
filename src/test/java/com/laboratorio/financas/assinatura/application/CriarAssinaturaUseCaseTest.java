package com.laboratorio.financas.assinatura.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarAssinaturaUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final Money VALOR = new Money(new BigDecimal("29.90"), Currency.getInstance("BRL"));
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    private AssinaturaRepository repository;
    private CriarAssinaturaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AssinaturaRepository.class);
        useCase = new CriarAssinaturaUseCase(repository);
    }

    @Test
    void executarCaminhoFelizSalvaERetornaEntidade() {
        Assinatura salva = new Assinatura(USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        when(repository.salvar(any(Assinatura.class))).thenReturn(salva);

        CriarAssinaturaUseCase.Comando cmd = new CriarAssinaturaUseCase.Comando(
                USER_ID, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        Assinatura resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Netflix");
        assertThat(resultado.getTipo()).isEqualTo(TipoAssinatura.STREAMING);
        verify(repository, times(1)).salvar(any(Assinatura.class));
    }
}
