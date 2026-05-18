package com.laboratorio.financas.anexo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarAnexosPorEntidadeUseCaseTest {

    private AnexoRepository anexoRepository;
    private ListarAnexosPorEntidadeUseCase useCase;

    @BeforeEach
    void setUp() {
        anexoRepository = Mockito.mock(AnexoRepository.class);
        useCase = new ListarAnexosPorEntidadeUseCase(anexoRepository);
    }

    @Test
    void executarRetornaAnexosDaEntidade() {
        UUID entidadeId = UUID.randomUUID();
        Anexo anexo = new Anexo("doc.pdf", "application/pdf", 1024, "TRANSACAO", entidadeId);
        when(anexoRepository.listarPorEntidade("TRANSACAO", entidadeId))
                .thenReturn(List.of(anexo));

        List<Anexo> resultado = useCase.executar("TRANSACAO", entidadeId);

        assertThat(resultado).containsExactly(anexo);
    }

    @Test
    void executarRetornaListaVaziaQuandoEntidadeNaoTemAnexos() {
        UUID entidadeId = UUID.randomUUID();
        when(anexoRepository.listarPorEntidade("CONTA", entidadeId)).thenReturn(List.of());

        List<Anexo> resultado = useCase.executar("CONTA", entidadeId);

        assertThat(resultado).isEmpty();
    }
}
