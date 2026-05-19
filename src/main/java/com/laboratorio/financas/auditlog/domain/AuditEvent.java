package com.laboratorio.financas.auditlog.domain;

import java.util.UUID;

/**
 * Evento de aplicacao publicado pelos controllers de negocio apos uma
 * mutacao bem-sucedida.
 *
 * <p>O processamento e desacoplado via Spring Application Events: o
 * {@code AuditEventListener} consome o evento de forma assincrona, de modo
 * que falha na escrita do audit log nao afeta a operacao de negocio.
 */
public record AuditEvent(
        String entityType,
        UUID entityId,
        AuditAction action,
        String userEmail,
        String screenCode,
        String before,
        String after
) {
}
