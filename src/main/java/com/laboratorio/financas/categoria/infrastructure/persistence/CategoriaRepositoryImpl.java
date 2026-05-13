package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CategoriaRepositoryImpl implements CategoriaRepository {

    private final CategoriaJpaRepository jpaRepository;
    private final CategoriaMapper mapper;

    public CategoriaRepositoryImpl(CategoriaJpaRepository jpaRepository, CategoriaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        CategoriaEntity entity = mapper.toEntity(categoria);
        CategoriaEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> listarTodas() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Categoria> listarPorTipo(TipoCategoria tipo) {
        return jpaRepository.findByTipo(tipo).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Categoria> listarRaiz() {
        return jpaRepository.findByCategoriaPaiIdIsNull().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Categoria> listarFilhosDe(UUID categoriaPaiId) {
        return jpaRepository.findByCategoriaPaiId(categoriaPaiId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
