package com.laboratorio.financas.incidente.domain;

import java.util.List;
import java.util.Optional;

public interface ErroRegistradoRepository {

    ErroRegistrado salvar(ErroRegistrado erro);

    Optional<ErroRegistrado> buscarPorCodigo(String codigo);

    List<ErroRegistrado> listarComFiltros(FiltrosIncidente filtros);
}
