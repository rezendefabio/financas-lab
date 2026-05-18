package com.laboratorio.financas.anexo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import com.laboratorio.financas.anexo.domain.ArmazenamentoService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FazerUploadAnexoUseCaseTest {

    private static final long DEZ_MB = 10L * 1024 * 1024;

    private AnexoRepository anexoRepository;
    private ArmazenamentoService armazenamentoService;
    private FazerUploadAnexoUseCase useCase;

    @BeforeEach
    void setUp() {
        anexoRepository = Mockito.mock(AnexoRepository.class);
        armazenamentoService = Mockito.mock(ArmazenamentoService.class);
        useCase = new FazerUploadAnexoUseCase(anexoRepository, armazenamentoService);
        when(anexoRepository.salvar(any(Anexo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private FazerUploadAnexoUseCase.Comando comandoComTamanho(long tamanho) {
        InputStream conteudo = new ByteArrayInputStream(new byte[] {1, 2, 3});
        return new FazerUploadAnexoUseCase.Comando(
                "doc.pdf", "application/pdf", tamanho, "TRANSACAO", UUID.randomUUID(), conteudo);
    }

    @Test
    void executarFazUploadESalvaAnexo() {
        FazerUploadAnexoUseCase.Comando comando = comandoComTamanho(1024);

        Anexo resultado = useCase.executar(comando);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("doc.pdf");
        verify(armazenamentoService).upload(
                eq(resultado.getChaveArmazenamento()), any(InputStream.class), eq(1024L),
                eq("application/pdf"));
        verify(anexoRepository).salvar(resultado);
    }

    @Test
    void executarUsaChaveArmazenamentoDoAnexoNoUpload() {
        FazerUploadAnexoUseCase.Comando comando = comandoComTamanho(2048);

        Anexo resultado = useCase.executar(comando);

        ArgumentCaptor<String> chaveCaptor = ArgumentCaptor.forClass(String.class);
        verify(armazenamentoService).upload(
                chaveCaptor.capture(), any(InputStream.class), anyLong(), anyString());
        assertThat(chaveCaptor.getValue()).isEqualTo(resultado.getChaveArmazenamento());
    }

    @Test
    void executarAceitaArquivoNoLimiteDeDezMb() {
        FazerUploadAnexoUseCase.Comando comando = comandoComTamanho(DEZ_MB);

        Anexo resultado = useCase.executar(comando);

        assertThat(resultado).isNotNull();
        verify(anexoRepository).salvar(any(Anexo.class));
    }

    @Test
    void executarRejeitaArquivoAcimaDeDezMb() {
        FazerUploadAnexoUseCase.Comando comando = comandoComTamanho(DEZ_MB + 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.executar(comando))
                .withMessageContaining("10MB");

        verify(armazenamentoService, never()).upload(anyString(), any(), anyLong(), anyString());
        verify(anexoRepository, never()).salvar(any());
    }
}
