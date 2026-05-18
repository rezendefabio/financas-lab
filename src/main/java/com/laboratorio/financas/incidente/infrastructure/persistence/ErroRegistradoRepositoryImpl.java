package com.laboratorio.financas.incidente.infrastructure.persistence;

import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.ErroRegistradoRepository;
import com.laboratorio.financas.incidente.domain.FiltrosIncidente;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ErroRegistradoRepositoryImpl implements ErroRegistradoRepository {

    private final ErroRegistradoJpaRepository jpaRepository;
    private final ErroRegistradoMapper mapper;

    public ErroRegistradoRepositoryImpl(ErroRegistradoJpaRepository jpaRepository,
                                        ErroRegistradoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ErroRegistrado salvar(ErroRegistrado erro) {
        ErroRegistradoEntity entity = mapper.toEntity(erro);
        ErroRegistradoEntity salvo = jpaRepository.save(entity);
        return mapper.toErroRegistrado(salvo);
    }

    @Override
    public Optional<ErroRegistrado> buscarPorCodigo(String codigo) {
        return jpaRepository.findByCodigo(codigo).map(mapper::toErroRegistrado);
    }

    @Override
    public List<ErroRegistrado> listarComFiltros(FiltrosIncidente filtros) {
        return jpaRepository.findComFiltros(
                filtros.criadoApartirDe(),
                filtros.criadoAte(),
                filtros.classeErro(),
                filtros.operacao()
        ).stream().map(mapper::toErroRegistrado).toList();
    }
}
