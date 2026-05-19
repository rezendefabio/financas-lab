package com.laboratorio.financas.auditlog.infrastructure;

import com.laboratorio.financas.auditlog.application.RegistrarAuditLogUseCase;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Consome {@link AuditEvent} de forma assincrona e persiste o registro de
 * auditoria.
 *
 * <p>A combinacao {@code @Async} + try/catch garante que uma falha na
 * escrita do audit log nunca propague para a thread que disparou a operacao
 * de negocio: o evento e apenas logado e descartado.
 */
@Component
public class AuditEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuditEventListener.class);

    private final RegistrarAuditLogUseCase registrarUseCase;

    public AuditEventListener(RegistrarAuditLogUseCase registrarUseCase) {
        this.registrarUseCase = registrarUseCase;
    }

    @Async
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        try {
            registrarUseCase.executar(
                    event.entityType(),
                    event.entityId(),
                    event.action(),
                    event.userEmail(),
                    event.screenCode(),
                    event.before(),
                    event.after()
            );
        } catch (RuntimeException ex) {
            LOG.error(
                    "Falha ao registrar audit log para {} {} ({})",
                    event.entityType(), event.entityId(), event.action(), ex);
        }
    }
}
