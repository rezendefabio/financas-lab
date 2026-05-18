package com.laboratorio.financas.anexo.application;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoNaoEncontradoException;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import com.laboratorio.financas.anexo.domain.ArmazenamentoService;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ObterUrlDownloadAnexoUseCase {

    private static final int EXPIRACAO_MINUTOS = 15;

    private final AnexoRepository anexoRepository;
    private final ArmazenamentoService armazenamentoService;

    public ObterUrlDownloadAnexoUseCase(AnexoRepository anexoRepository,
                                        ArmazenamentoService armazenamentoService) {
        this.anexoRepository = anexoRepository;
        this.armazenamentoService = armazenamentoService;
    }

    @Transactional(readOnly = true)
    public String executar(UUID id) {
        Anexo anexo = anexoRepository.buscarPorId(id)
                .orElseThrow(() -> new AnexoNaoEncontradoException(id));
        return armazenamentoService.urlTemporaria(anexo.getChaveArmazenamento(), EXPIRACAO_MINUTOS);
    }
}
