package com.laboratorio.financas.notificacao.infrastructure.persistence;

import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoJpaRepository extends JpaRepository<NotificacaoEntity, UUID> {

    List<NotificacaoEntity> findByUserId(UUID userId);

    Optional<NotificacaoEntity> findByUserIdAndTipoAndReferenciaId(
            UUID userId, TipoNotificacao tipo, UUID referenciaId);
}
