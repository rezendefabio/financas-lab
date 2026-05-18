package com.laboratorio.financas.anexo.infrastructure.persistence;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AnexoRepositoryImpl implements AnexoRepository {

    private final AnexoJpaRepository jpaRepository;
    private final AnexoMapper mapper;

    public AnexoRepositoryImpl(AnexoJpaRepository jpaRepository, AnexoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Anexo salvar(Anexo domain) {
        AnexoEntity entity = mapper.toEntity(domain);
        AnexoEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Anexo> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Anexo> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
