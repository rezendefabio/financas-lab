package com.laboratorio.financas.auditlog.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository {

    Page<AuditLog> listarPorEntidade(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> listar(FiltrosAuditLog filtros, Pageable pageable);

    AuditLog salvar(AuditLog log);
}
