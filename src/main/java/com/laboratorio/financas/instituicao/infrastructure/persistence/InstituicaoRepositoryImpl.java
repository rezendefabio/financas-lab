package com.laboratorio.financas.instituicao.infrastructure.persistence;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.domain.InstituicaoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InstituicaoRepositoryImpl implements InstituicaoRepository {

    private final InstituicaoJpaRepository jpaRepository;
    private final InstituicaoMapper mapper;

    public InstituicaoRepositoryImpl(InstituicaoJpaRepository jpaRepository, InstituicaoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Instituicao> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Instituicao> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Instituicao> findAllAtivas() {
        return jpaRepository.findByAtivaTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Instituicao save(Instituicao instituicao) {
        InstituicaoEntity entity = mapper.toEntity(instituicao);
        InstituicaoEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }
}
