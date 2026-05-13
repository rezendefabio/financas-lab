package com.laboratorio.financas.meta.infrastructure.persistence;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MetaRepositoryImpl implements MetaRepository {

    private final MetaJpaRepository jpaRepository;
    private final MetaMapper mapper;

    public MetaRepositoryImpl(MetaJpaRepository jpaRepository, MetaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Meta salvar(Meta meta) {
        MetaEntity entity = mapper.toEntity(meta);
        MetaEntity salva = jpaRepository.save(entity);
        return mapper.toMeta(salva);
    }

    @Override
    public Meta atualizar(Meta meta) {
        MetaEntity entity = mapper.toEntity(meta);
        MetaEntity salva = jpaRepository.save(entity);
        return mapper.toMeta(salva);
    }

    @Override
    public Optional<Meta> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toMeta);
    }

    @Override
    public List<Meta> listar() {
        return jpaRepository.findAll().stream()
                .map(mapper::toMeta)
                .toList();
    }
}
