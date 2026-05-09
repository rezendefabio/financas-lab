package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class TransacaoRepositoryImpl implements TransacaoRepository {

    private final TransacaoJpaRepository jpaRepository;
    private final TransacaoMapper mapper;

    public TransacaoRepositoryImpl(TransacaoJpaRepository jpaRepository, TransacaoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Transacao salvar(Transacao transacao) {
        TransacaoEntity entity = mapper.toEntity(transacao);
        TransacaoEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Transacao> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable) {
        LocalDate inicio = filtros.dataInicio() != null ? filtros.dataInicio() : LocalDate.of(1900, 1, 1);
        LocalDate fim = filtros.dataFim() != null ? filtros.dataFim() : LocalDate.of(9999, 12, 31);
        return jpaRepository.findComFiltros(
                filtros.contaId(),
                inicio,
                fim,
                filtros.tipo(),
                filtros.categoriaId(),
                pageable
        ).map(mapper::toDomain);
    }
}
