package com.laboratorio.financas.auditlog.infrastructure.persistence;

import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import com.laboratorio.financas.auditlog.domain.FiltrosAuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;
    private final AuditLogMapper mapper;

    public AuditLogRepositoryImpl(AuditLogJpaRepository jpaRepository, AuditLogMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Page<AuditLog> listarPorEntidade(String entityType, UUID entityId, Pageable pageable) {
        return jpaRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> listar(FiltrosAuditLog filtros, Pageable pageable) {
        return jpaRepository.findComFiltros(
                filtros.entityType(),
                filtros.entityId(),
                filtros.action(),
                filtros.userEmail(),
                filtros.criadoApartirDe(),
                filtros.criadoAte(),
                pageable
        ).map(mapper::toDomain);
    }

    @Override
    public AuditLog salvar(AuditLog log) {
        AuditLogEntity entity = mapper.toEntity(log);
        AuditLogEntity salvo = jpaRepository.save(entity);
        return mapper.toDomain(salvo);
    }
}
