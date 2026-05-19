package com.laboratorio.financas.auditlog.interfaces.dto;

import com.laboratorio.financas.auditlog.domain.AuditLog;
import java.util.UUID;

/**
 * Representacao de leitura de um registro da trilha de auditoria.
 *
 * <p>{@code criadoEm} e exposto como string ISO-8601.
 */
public record AuditLogResponse(
        UUID id,
        String entityType,
        UUID entityId,
        String action,
        String userEmail,
        String screenCode,
        String before,
        String after,
        String criadoEm
) {

    public static AuditLogResponse fromDomain(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction().name(),
                log.getUserEmail(),
                log.getScreenCode(),
                log.getBefore(),
                log.getAfter(),
                log.getCriadoEm().toString()
        );
    }
}
