package com.laboratorio.financas.grupo.infrastructure.persistence;

import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.grupo.domain.GrupoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GrupoRepositoryImpl implements GrupoRepository {

    private final GrupoJpaRepository jpaRepository;
    private final GrupoMapper mapper;

    public GrupoRepositoryImpl(GrupoJpaRepository jpaRepository, GrupoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Grupo salvar(Grupo grupo) {
        GrupoEntity entity = mapper.toEntity(grupo);
        GrupoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Grupo> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Grupo> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
