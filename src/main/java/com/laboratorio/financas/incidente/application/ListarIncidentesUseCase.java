package com.laboratorio.financas.incidente.application;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import com.laboratorio.financas.incidente.domain.FiltrosIncidente;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListarIncidentesUseCase {

    private final ErroRegistradoRepository repository;

    public ListarIncidentesUseCase(ErroRegistradoRepository repository) {
        this.repository = repository;
    }

    public List<ErroRegistrado> executar(FiltrosIncidente filtros) {
        return repository.listarComFiltros(filtros);
    }
}
