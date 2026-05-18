package com.laboratorio.financas.incidente.application;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import com.laboratorio.financas.incidente.domain.IncidenteNaoEncontradoException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarIncidenteUseCase {

    private final ErroRegistradoRepository erroRegistradoRepository;

    public BuscarIncidenteUseCase(ErroRegistradoRepository erroRegistradoRepository) {
        this.erroRegistradoRepository = erroRegistradoRepository;
    }

    @Transactional(readOnly = true)
    public ErroRegistrado executar(String codigo) {
        return erroRegistradoRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new IncidenteNaoEncontradoException(codigo));
    }
}
