package com.laboratorio.financas.anotacao.infrastructure.persistence;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.AnotacaoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AnotacaoRepositoryImpl implements AnotacaoRepository {

    private final AnotacaoJpaRepository jpaRepository;
    private final AnotacaoMapper mapper;

    public AnotacaoRepositoryImpl(AnotacaoJpaRepository jpaRepository, AnotacaoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Anotacao salvar(Anotacao anotacao) {
        AnotacaoEntity entity = mapper.toEntity(anotacao);
        AnotacaoEntity salva = jpaRepository.save(entity);
        return mapper.toAnotacao(salva);
    }

    @Override
    public Optional<Anotacao> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toAnotacao);
    }

    @Override
    public List<Anotacao> listarPorUsuario(UUID usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::toAnotacao)
                .toList();
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
