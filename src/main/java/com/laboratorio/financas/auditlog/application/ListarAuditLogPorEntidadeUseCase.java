package com.laboratorio.financas.auditlog.application;

import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Lista a trilha de auditoria de uma entidade especifica, paginada e
 * ordenada do evento mais recente para o mais antigo.
 */
@Component
public class ListarAuditLogPorEntidadeUseCase {

    private final AuditLogRepository auditLogRepository;

    public ListarAuditLogPorEntidadeUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLog> executar(String entityType, UUID entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.listarPorEntidade(entityType, entityId, pageable);
    }
}
