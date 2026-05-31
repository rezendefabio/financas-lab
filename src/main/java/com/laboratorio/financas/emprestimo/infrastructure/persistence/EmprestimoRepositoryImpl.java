package com.laboratorio.financas.emprestimo.infrastructure.persistence;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.EmprestimoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EmprestimoRepositoryImpl implements EmprestimoRepository {

    private final EmprestimoJpaRepository jpaRepository;
    private final EmprestimoMapper mapper;

    public EmprestimoRepositoryImpl(EmprestimoJpaRepository jpaRepository,
                                    EmprestimoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Emprestimo salvar(Emprestimo emprestimo) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(emprestimo)));
    }

    @Override
    public Optional<Emprestimo> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Emprestimo> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Emprestimo atualizar(Emprestimo emprestimo) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(emprestimo)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
