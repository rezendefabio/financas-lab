package com.laboratorio.financas.lancamentorecorrente.infrastructure.persistence;

import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrenteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class LancamentoRecorrenteRepositoryImpl implements LancamentoRecorrenteRepository {

    private final LancamentoRecorrenteJpaRepository jpaRepository;
    private final LancamentoRecorrenteMapper mapper;

    public LancamentoRecorrenteRepositoryImpl(
            LancamentoRecorrenteJpaRepository jpaRepository,
            LancamentoRecorrenteMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public LancamentoRecorrente salvar(LancamentoRecorrente lancamento) {
        LancamentoRecorrenteEntity entity = mapper.toEntity(lancamento);
        LancamentoRecorrenteEntity salvo = jpaRepository.save(entity);
        return mapper.toDomain(salvo);
    }

    @Override
    public Optional<LancamentoRecorrente> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<LancamentoRecorrente> listar() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public LancamentoRecorrente atualizar(LancamentoRecorrente lancamento) {
        LancamentoRecorrenteEntity entity = mapper.toEntity(lancamento);
        LancamentoRecorrenteEntity atualizado = jpaRepository.save(entity);
        return mapper.toDomain(atualizado);
    }
}
