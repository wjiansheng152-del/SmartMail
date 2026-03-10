/**
 * 审计日志接口
 */
import request from '@/utils/request'
import type { AuditLog } from '@/types/entity'

export function getAuditLogList(params: {
  page?: number
  size?: number
  userId?: number
}) {
  return request.get<AuditLog[] | { records: AuditLog[]; total: number }>(
    '/audit/log/list',
    { params }
  )
}
