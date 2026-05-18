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
    public Anexo salvar(Anexo anexo) {
        AnexoEntity entity = mapper.toEntity(anexo);
        AnexoEntity salvo = jpaRepository.save(entity);
        return mapper.toDomain(salvo);
    }

    @Override
    public Optional<Anexo> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Anexo> listarPorEntidade(String entidadeTipo, UUID entidadeId) {
        return jpaRepository.findByEntidadeTipoAndEntidadeId(entidadeTipo, entidadeId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void remover(UUID id) {
        jpaRepository.deleteById(id);
    }
}
