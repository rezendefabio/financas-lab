package com.laboratorio.financas.anexo.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoNaoEncontradoException;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import com.laboratorio.financas.anexo.domain.ArmazenamentoService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RemoverAnexoUseCaseTest {

    private AnexoRepository anexoRepository;
    private ArmazenamentoService armazenamentoService;
    private RemoverAnexoUseCase useCase;

    @BeforeEach
    void setUp() {
        anexoRepository = Mockito.mock(AnexoRepository.class);
        armazenamentoService = Mockito.mock(ArmazenamentoService.class);
        useCase = new RemoverAnexoUseCase(anexoRepository, armazenamentoService);
    }

    @Test
    void executarRemoveDoArmazenamentoEDoRepositorio() {
        UUID id = UUID.randomUUID();
        Anexo anexo = new Anexo("doc.pdf", "application/pdf", 1024, "TRANSACAO", UUID.randomUUID());
        when(anexoRepository.buscarPorId(id)).thenReturn(Optional.of(anexo));

        useCase.executar(id);

        verify(armazenamentoService).remover(anexo.getChaveArmazenamento());
        verify(anexoRepository).remover(id);
    }

    @Test
    void executarLancaExcecaoQuandoAnexoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(anexoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(AnexoNaoEncontradoException.class);

        verify(armazenamentoService, never()).remover(Mockito.anyString());
        verify(anexoRepository, never()).remover(Mockito.any());
    }
}
