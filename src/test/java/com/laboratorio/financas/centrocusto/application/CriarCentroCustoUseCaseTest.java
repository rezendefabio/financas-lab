package com.laboratorio.financas.centrocusto.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.centrocusto.application.dto.CriarCentroCustoComando;
import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarCentroCustoUseCaseTest {

    private CentroCustoRepository repository;
    private CriarCentroCustoUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CentroCustoRepository.class);
        useCase = new CriarCentroCustoUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaCentroCusto() {
        CentroCusto salvo = new CentroCusto(USER_ID, "Casa", null);
        when(repository.save(any(CentroCusto.class))).thenReturn(salvo);

        CentroCusto resultado = useCase.executar(new CriarCentroCustoComando(USER_ID, "Casa", null));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Casa");
    }

    @Test
    void executarChamaRepositorioSaveUmaVez() {
        CentroCusto salvo = new CentroCusto(USER_ID, "Trabalho", null);
        when(repository.save(any(CentroCusto.class))).thenReturn(salvo);

        useCase.executar(new CriarCentroCustoComando(USER_ID, "Trabalho", null));

        verify(repository, times(1)).save(any(CentroCusto.class));
    }

    @Test
    void executarComDescricaoPreservaCampo() {
        CentroCusto salvo = new CentroCusto(USER_ID, "Casa", "Despesas");
        when(repository.save(any(CentroCusto.class))).thenReturn(salvo);

        CentroCusto resultado = useCase.executar(
                new CriarCentroCustoComando(USER_ID, "Casa", "Despesas"));

        assertThat(resultado.getDescricao()).isEqualTo("Despesas");
    }
}
