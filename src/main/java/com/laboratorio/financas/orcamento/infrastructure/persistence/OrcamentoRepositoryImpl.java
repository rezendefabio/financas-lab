package com.laboratorio.financas.orcamento.infrastructure.persistence;

import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoRepositoryImpl implements OrcamentoRepository {

    private final OrcamentoJpaRepository jpaRepository;
    private final OrcamentoMapper mapper;

    public OrcamentoRepositoryImpl(OrcamentoJpaRepository jpaRepository, OrcamentoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Orcamento salvar(Orcamento domain) {
        OrcamentoEntity entity = mapper.toEntity(domain);
        OrcamentoEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Orcamento> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Orcamento> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
