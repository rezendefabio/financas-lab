package com.laboratorio.financas.lembrete.infrastructure.persistence;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LembreteRepositoryImpl implements LembreteRepository {

    private final LembreteJpaRepository jpaRepository;
    private final LembreteMapper mapper;

    public LembreteRepositoryImpl(LembreteJpaRepository jpaRepository,
                                  LembreteMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Lembrete salvar(Lembrete lembrete) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(lembrete)));
    }

    @Override
    public Optional<Lembrete> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Lembrete> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Lembrete atualizar(Lembrete lembrete) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(lembrete)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
