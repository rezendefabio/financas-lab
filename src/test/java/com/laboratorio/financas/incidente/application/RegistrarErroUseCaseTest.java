package com.laboratorio.financas.incidente.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RegistrarErroUseCaseTest {

    private ErroRegistradoRepository erroRegistradoRepository;
    private RegistrarErroUseCase useCase;

    @BeforeEach
    void setUp() {
        erroRegistradoRepository = Mockito.mock(ErroRegistradoRepository.class);
        useCase = new RegistrarErroUseCase(erroRegistradoRepository);
    }

    @Test
    void executarSalvaErroERetornaCodigoGerado() {
        // Given
        when(erroRegistradoRepository.salvar(any(ErroRegistrado.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        RegistrarErroUseCase.Comando comando = new RegistrarErroUseCase.Comando(
                "POST /api/transacoes", "NullPointerException", "valor nulo", "stack...");

        // When
        String codigo = useCase.executar(comando);

        // Then
        assertThat(codigo).matches("ERR-[0-9A-F]{8}");
    }

    @Test
    void executarConstroiErroComOsDadosDoComando() {
        // Given
        when(erroRegistradoRepository.salvar(any(ErroRegistrado.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        ArgumentCaptor<ErroRegistrado> captor = ArgumentCaptor.forClass(ErroRegistrado.class);
        RegistrarErroUseCase.Comando comando = new RegistrarErroUseCase.Comando(
                "GET /api/contas", "IllegalStateException", "estado invalido", "stack completo");

        // When
        useCase.executar(comando);

        // Then
        Mockito.verify(erroRegistradoRepository).salvar(captor.capture());
        ErroRegistrado salvo = captor.getValue();
        assertThat(salvo.getOperacao()).isEqualTo("GET /api/contas");
        assertThat(salvo.getClasseErro()).isEqualTo("IllegalStateException");
        assertThat(salvo.getMensagem()).isEqualTo("estado invalido");
        assertThat(salvo.getStackTrace()).isEqualTo("stack completo");
    }

    @Test
    void executarComMensagemNulaConstroiErroComTextoPadrao() {
        // Given
        when(erroRegistradoRepository.salvar(any(ErroRegistrado.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        ArgumentCaptor<ErroRegistrado> captor = ArgumentCaptor.forClass(ErroRegistrado.class);
        RegistrarErroUseCase.Comando comando = new RegistrarErroUseCase.Comando(
                "op", "Classe", null, "stack");

        // When
        useCase.executar(comando);

        // Then
        Mockito.verify(erroRegistradoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getMensagem()).isEqualTo("(sem mensagem)");
    }

    @Test
    void executarRetornaCodigoDoErroPersistido() {
        // Given — o codigo retornado e o da instancia devolvida pelo repositorio.
        ErroRegistrado persistido = new ErroRegistrado(
                "op", "Classe", "msg", "stack");
        when(erroRegistradoRepository.salvar(any(ErroRegistrado.class))).thenReturn(persistido);
        RegistrarErroUseCase.Comando comando = new RegistrarErroUseCase.Comando(
                "op", "Classe", "msg", "stack");

        // When
        String codigo = useCase.executar(comando);

        // Then
        assertThat(codigo).isEqualTo(persistido.getCodigo());
    }
}
