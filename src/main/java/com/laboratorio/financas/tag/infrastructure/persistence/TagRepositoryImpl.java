package com.laboratorio.financas.tag.infrastructure.persistence;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TagRepositoryImpl implements TagRepository {

    private final TagJpaRepository jpaRepository;
    private final TagMapper mapper;

    public TagRepositoryImpl(TagJpaRepository jpaRepository, TagMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Tag> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Tag> buscarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Tag> buscarPorIdEUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public Tag salvar(Tag tag) {
        TagEntity entity = mapper.toEntity(tag);
        TagEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
