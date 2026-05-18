package com.laboratorio.financas.anexo.application;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoNaoEncontradoException;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import com.laboratorio.financas.anexo.domain.ArmazenamentoService;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RemoverAnexoUseCase {

    private final AnexoRepository anexoRepository;
    private final ArmazenamentoService armazenamentoService;

    public RemoverAnexoUseCase(AnexoRepository anexoRepository,
                               ArmazenamentoService armazenamentoService) {
        this.anexoRepository = anexoRepository;
        this.armazenamentoService = armazenamentoService;
    }

    @Transactional
    public void executar(UUID id) {
        Anexo anexo = anexoRepository.buscarPorId(id)
                .orElseThrow(() -> new AnexoNaoEncontradoException(id));
        armazenamentoService.remover(anexo.getChaveArmazenamento());
        anexoRepository.remover(id);
    }
}
