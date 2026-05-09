package com.laboratorio.financas.conta.infrastructure.persistence;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContaRepositoryImpl implements ContaRepository {

    private final ContaJpaRepository jpaRepository;
    private final ContaMapper mapper;

    public ContaRepositoryImpl(ContaJpaRepository jpaRepository, ContaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Conta salvar(Conta conta) {
        ContaEntity entity = mapper.toEntity(conta);
        ContaEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Conta> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Conta> listarTodas() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Conta> listarAtivas() {
        return jpaRepository.findByAtivaTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
