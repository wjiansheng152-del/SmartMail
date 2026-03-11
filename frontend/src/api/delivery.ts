/**
 * 投递状态与用户 SMTP 配置接口
 */
import request from '@/utils/request'
import type { DeliveryStatus } from '@/types/entity'

/** 用户 SMTP 配置 DTO，与后端 SmtpConfigDto 一致；password 仅 PUT 时可选，GET 返回占位 **** */
export interface SmtpConfigDto {
  host?: string
  port?: number
  username?: string
  password?: string
  fromEmail?: string
  fromName?: string
  useSsl?: boolean
}

export function getDeliveryStatus(campaignId: number) {
  return request.get<DeliveryStatus>(`/delivery/delivery/status/${campaignId}`)
}

/** 获取当前用户 SMTP 配置（密码为占位） */
export function getSmtpConfig() {
  return request.get<SmtpConfigDto | null>('/delivery/smtp-config')
}

/** 保存当前用户 SMTP 配置；password 留空表示不修改密码 */
export function saveSmtpConfig(data: SmtpConfigDto) {
  return request.put<SmtpConfigDto>('/delivery/smtp-config', data)
}
