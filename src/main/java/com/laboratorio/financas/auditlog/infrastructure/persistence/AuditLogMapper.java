package com.laboratorio.financas.auditlog.infrastructure.persistence;

import com.laboratorio.financas.auditlog.domain.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    default AuditLogEntity toEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new AuditLogEntity(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getUserEmail(),
                log.getScreenCode(),
                log.getBefore(),
                log.getAfter(),
                log.getCriadoEm()
        );
    }

    default AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuditLog(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getUserEmail(),
                entity.getScreenCode(),
                entity.getBeforeState(),
                entity.getAfterState(),
                entity.getCriadoEm()
        );
    }
}
