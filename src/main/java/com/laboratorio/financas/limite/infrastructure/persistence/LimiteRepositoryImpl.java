package com.laboratorio.financas.limite.infrastructure.persistence;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LimiteRepositoryImpl implements LimiteRepository {

    private final LimiteJpaRepository jpaRepository;
    private final LimiteMapper mapper;

    public LimiteRepositoryImpl(LimiteJpaRepository jpaRepository, LimiteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Limite salvar(Limite limite) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(limite)));
    }

    @Override
    public Optional<Limite> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Limite> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Limite atualizar(Limite limite) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(limite)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
