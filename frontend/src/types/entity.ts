/**
 * 与后端实体对齐的类型定义
 * 日期时间格式：yyyy-MM-dd HH:mm:ss（北京标准时间）
 */

/** 客户/联系人 */
export interface Contact {
  id: number
  email: string
  name?: string
  mobile?: string
  createTime: string
  updateTime: string
}

/** 新建客户请求 */
export interface ContactCreateRequest {
  email: string
  name?: string
  mobile?: string
}

/** 客户分组 */
export interface ContactGroup {
  id: number
  name: string
  ruleType?: string
  ruleExpr?: string
  createTime: string
  updateTime: string
}

/** 邮件模板 */
export interface EmailTemplate {
  id?: number
  name: string
  subject: string
  bodyHtml: string
  variables?: string
  version?: number
  createTime?: string
  updateTime?: string
}

/** 营销活动 */
export interface Campaign {
  id?: number
  name: string
  templateId: number
  groupId: number
  status?: string
  abConfig?: string
  scheduledAt?: string
  createTime?: string
  updateTime?: string
}

/** 调度创建请求 */
export interface ScheduleCreateRequest {
  campaignId: number
  cronExpr?: string
  runAt?: string
}

/** 调度计划（与后端 ScheduleJob 对齐） */
export interface ScheduleJob {
  id: number
  campaignId: number
  cronExpr?: string
  runAt?: string
  status?: string
  createTime?: string
  updateTime?: string
}

/** 退订记录 */
export interface Unsubscribe {
  id?: number
  email: string
  reason?: string
  createTime?: string
}

/** 黑名单 */
export interface Blacklist {
  id?: number
  email: string
  source?: string
  createTime?: string
}

/** 审计日志 */
export interface AuditLog {
  id?: number
  userId?: number
  action: string
  resource: string
  resourceId?: string
  detail?: string
  createTime?: string
}

/** 投递状态（delivery status） */
export interface DeliveryStatus {
  campaignId: number
  total: number
  sent: number
  failed: number
}

/** 追踪统计（tracking stats） */
export interface TrackingStats {
  campaignId?: number
  openCount?: number
  clickCount?: number
}
