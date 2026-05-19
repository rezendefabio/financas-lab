import { apiFetch } from '@/services/api-client'
import type { AuditLogPage } from '../types/auditlog'

export const auditlogService = {
  listarPorEntidade: (
    entityType: string,
    entityId: string,
    page = 0,
    size = 20,
  ): Promise<AuditLogPage> =>
    apiFetch<AuditLogPage>(
      `/api/audit-log?entityType=${encodeURIComponent(entityType)}` +
        `&entityId=${encodeURIComponent(entityId)}&page=${page}&size=${size}`,
    ),
}
