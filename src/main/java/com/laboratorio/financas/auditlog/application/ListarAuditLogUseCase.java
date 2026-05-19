package com.laboratorio.financas.auditlog.application;

import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import com.laboratorio.financas.auditlog.domain.FiltrosAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Lista a trilha de auditoria do sistema com filtros opcionais, paginada e
 * ordenada do evento mais recente para o mais antigo.
 */
@Component
public class ListarAuditLogUseCase {

    private final AuditLogRepository auditLogRepository;

    public ListarAuditLogUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLog> executar(FiltrosAuditLog filtros, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.listar(filtros, pageable);
    }
}
