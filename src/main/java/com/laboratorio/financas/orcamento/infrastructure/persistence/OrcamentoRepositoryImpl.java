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
    public Orcamento salvar(Orcamento orcamento) {
        OrcamentoEntity entity = mapper.toEntity(orcamento);
        OrcamentoEntity salvo = jpaRepository.save(entity);
        return mapper.toOrcamento(salvo);
    }

    @Override
    public Optional<Orcamento> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toOrcamento);
    }

    @Override
    public List<Orcamento> listar() {
        return jpaRepository.findAll().stream().map(mapper::toOrcamento).toList();
    }

    @Override
    public Orcamento atualizar(Orcamento orcamento) {
        OrcamentoEntity entity = mapper.toEntity(orcamento);
        OrcamentoEntity salvo = jpaRepository.save(entity);
        return mapper.toOrcamento(salvo);
    }
}
