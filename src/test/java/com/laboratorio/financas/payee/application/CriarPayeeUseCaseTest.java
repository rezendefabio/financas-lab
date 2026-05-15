package com.laboratorio.financas.payee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.payee.application.dto.CriarPayeeComando;
import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarPayeeUseCaseTest {

    private PayeeRepository repository;
    private CriarPayeeUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PayeeRepository.class);
        useCase = new CriarPayeeUseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetornaPayee() {
        Payee payeeSalvo = payee("Supermercado", null);
        when(repository.save(any(Payee.class))).thenReturn(payeeSalvo);
        CriarPayeeComando comando = new CriarPayeeComando(USER_ID, "Supermercado", null);

        Payee resultado = useCase.executar(comando);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Supermercado");
    }

    @Test
    void executarChamaRepositorioSaveUmaVez() {
        Payee payeeSalvo = payee("Farmacia", null);
        when(repository.save(any(Payee.class))).thenReturn(payeeSalvo);
        CriarPayeeComando comando = new CriarPayeeComando(USER_ID, "Farmacia", null);

        useCase.executar(comando);

        verify(repository, times(1)).save(any(Payee.class));
    }

    @Test
    void executarRetornaOQueRepositorioRetornou() {
        Payee payeeSalvo = payee("Padaria", null);
        when(repository.save(any(Payee.class))).thenReturn(payeeSalvo);
        CriarPayeeComando comando = new CriarPayeeComando(USER_ID, "Padaria", null);

        Payee resultado = useCase.executar(comando);

        assertThat(resultado).isSameAs(payeeSalvo);
    }

    @Test
    void executarComCategoriaPadraoIdPreservaCampo() {
        UUID categoriaId = UUID.randomUUID();
        Payee payeeSalvo = payee("Academia", categoriaId);
        when(repository.save(any(Payee.class))).thenReturn(payeeSalvo);
        CriarPayeeComando comando = new CriarPayeeComando(USER_ID, "Academia", categoriaId);

        Payee resultado = useCase.executar(comando);

        assertThat(resultado.getCategoriaPadraoId()).isEqualTo(categoriaId);
    }

    private Payee payee(String nome, UUID categoriaId) {
        return new Payee(USER_ID, nome, categoriaId);
    }
}
