package com.laboratorio.financas.auditlog.interfaces;

import com.laboratorio.financas.auditlog.application.ListarAuditLogPorEntidadeUseCase;
import com.laboratorio.financas.auditlog.application.ListarAuditLogUseCase;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.FiltrosAuditLog;
import com.laboratorio.financas.auditlog.interfaces.dto.AuditLogResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Endpoint de consulta da trilha de auditoria.
 *
 * <p>Quando {@code entityId} e informado a consulta e delegada para
 * {@link ListarAuditLogPorEntidadeUseCase}; caso contrario, para
 * {@link ListarAuditLogUseCase} com os filtros opcionais.
 */
@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final ListarAuditLogPorEntidadeUseCase listarPorEntidadeUseCase;
    private final ListarAuditLogUseCase listarUseCase;

    public AuditLogController(
            ListarAuditLogPorEntidadeUseCase listarPorEntidadeUseCase,
            ListarAuditLogUseCase listarUseCase
    ) {
        this.listarPorEntidadeUseCase = listarPorEntidadeUseCase;
        this.listarUseCase = listarUseCase;
    }

    @GetMapping
    public Page<AuditLogResponse> listar(
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "entityId", required = false) UUID entityId,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "userEmail", required = false) String userEmail,
            @RequestParam(name = "criadoApartirDe", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant criadoApartirDe,
            @RequestParam(name = "criadoAte", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant criadoAte,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        if (entityId != null) {
            if (entityType == null || entityType.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "entityType e obrigatorio quando entityId e informado");
            }
            return listarPorEntidadeUseCase.executar(entityType, entityId, page, size)
                    .map(AuditLogResponse::fromDomain);
        }
        FiltrosAuditLog filtros = new FiltrosAuditLog(
                entityType, null, action, userEmail, criadoApartirDe, criadoAte);
        return listarUseCase.executar(filtros, page, size)
                .map(AuditLogResponse::fromDomain);
    }
}
