export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE'

export interface AuditLogEntry {
  id: string
  entityType: string
  entityId: string
  action: AuditAction
  userEmail: string | null
  screenCode: string | null
  before: string | null
  after: string | null
  criadoEm: string
}

export interface AuditLogPage {
  content: AuditLogEntry[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
