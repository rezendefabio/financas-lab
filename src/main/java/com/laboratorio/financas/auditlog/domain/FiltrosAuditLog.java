package com.laboratorio.financas.auditlog.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Filtros opcionais para consulta geral da trilha de auditoria.
 *
 * <p>Todos os campos sao nullable: um campo null significa "nao filtrar
 * por esse criterio".
 */
public record FiltrosAuditLog(
        String entityType,
        UUID entityId,
        AuditAction action,
        String userEmail,
        Instant criadoApartirDe,
        Instant criadoAte
) {
}
