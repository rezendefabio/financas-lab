package com.laboratorio.financas.fatura.infrastructure.persistence;

import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.domain.FaturaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FaturaRepositoryImpl implements FaturaRepository {

    private final FaturaJpaRepository jpaRepository;
    private final FaturaMapper mapper;

    public FaturaRepositoryImpl(FaturaJpaRepository jpaRepository, FaturaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Fatura salvar(Fatura fatura) {
        FaturaEntity entity = mapper.toEntity(fatura);
        FaturaEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Fatura> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Fatura> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Fatura atualizar(Fatura fatura) {
        FaturaEntity entity = mapper.toEntity(fatura);
        FaturaEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
