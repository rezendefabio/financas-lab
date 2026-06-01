package com.laboratorio.financas.assinatura.infrastructure.persistence;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AssinaturaRepositoryImpl implements AssinaturaRepository {

    private final AssinaturaJpaRepository jpaRepository;
    private final AssinaturaMapper mapper;

    public AssinaturaRepositoryImpl(AssinaturaJpaRepository jpaRepository, AssinaturaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Assinatura salvar(Assinatura entidade) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(entidade)));
    }

    @Override
    public Optional<Assinatura> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Assinatura> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Assinatura atualizar(Assinatura entidade) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(entidade)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
