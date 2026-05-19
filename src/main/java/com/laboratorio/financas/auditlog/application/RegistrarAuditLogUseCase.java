package com.laboratorio.financas.auditlog.application;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Registra um novo evento na trilha de auditoria.
 *
 * <p>Invocado pelo {@code AuditEventListener}, nunca diretamente pelos
 * controllers de negocio.
 */
@Component
public class RegistrarAuditLogUseCase {

    private final AuditLogRepository auditLogRepository;

    public RegistrarAuditLogUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog executar(
            String entityType,
            UUID entityId,
            AuditAction action,
            String userEmail,
            String screenCode,
            String before,
            String after
    ) {
        AuditLog log = new AuditLog(entityType, entityId, action, userEmail, screenCode, before, after);
        return auditLogRepository.salvar(log);
    }
}
