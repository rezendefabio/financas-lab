package com.laboratorio.financas.auditlog.infrastructure.persistence;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Query("SELECT a FROM AuditLogEntity a "
            + "WHERE a.entityType = :entityType AND a.entityId = :entityId "
            + "ORDER BY a.criadoEm DESC")
    Page<AuditLogEntity> findByEntityTypeAndEntityId(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            Pageable pageable);

    @Query("""
            SELECT a FROM AuditLogEntity a WHERE
            (:entityType IS NULL OR a.entityType = :entityType) AND
            (:entityId IS NULL OR a.entityId = :entityId) AND
            (:action IS NULL OR a.action = :action) AND
            (:userEmail IS NULL OR a.userEmail = :userEmail) AND
            (CAST(:criadoApartirDe AS timestamp) IS NULL OR a.criadoEm >= :criadoApartirDe) AND
            (CAST(:criadoAte AS timestamp) IS NULL OR a.criadoEm <= :criadoAte)
            ORDER BY a.criadoEm DESC
            """)
    Page<AuditLogEntity> findComFiltros(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("action") AuditAction action,
            @Param("userEmail") String userEmail,
            @Param("criadoApartirDe") Instant criadoApartirDe,
            @Param("criadoAte") Instant criadoAte,
            Pageable pageable);
}
