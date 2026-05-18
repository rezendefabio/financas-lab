package com.laboratorio.financas.anexo.application;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarAnexosPorEntidadeUseCase {

    private final AnexoRepository anexoRepository;

    public ListarAnexosPorEntidadeUseCase(AnexoRepository anexoRepository) {
        this.anexoRepository = anexoRepository;
    }

    @Transactional(readOnly = true)
    public List<Anexo> executar(String entidadeTipo, UUID entidadeId) {
        return anexoRepository.listarPorEntidade(entidadeTipo, entidadeId);
    }
}
