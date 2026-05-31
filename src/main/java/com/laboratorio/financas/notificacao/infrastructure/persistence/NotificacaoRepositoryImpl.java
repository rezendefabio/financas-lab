package com.laboratorio.financas.notificacao.infrastructure.persistence;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoRepositoryImpl implements NotificacaoRepository {

    private final NotificacaoJpaRepository jpaRepository;
    private final NotificacaoMapper mapper;

    public NotificacaoRepositoryImpl(NotificacaoJpaRepository jpaRepository, NotificacaoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notificacao salvar(Notificacao notificacao) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(notificacao)));
    }

    @Override
    public Optional<Notificacao> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notificacao> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Notificacao> buscarPorChaveNatural(UUID userId, TipoNotificacao tipo, UUID referenciaId) {
        return jpaRepository.findByUserIdAndTipoAndReferenciaId(userId, tipo, referenciaId)
                .map(mapper::toDomain);
    }

    @Override
    public Notificacao atualizar(Notificacao notificacao) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(notificacao)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
