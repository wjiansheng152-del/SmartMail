/**
 * 邮件模板接口
 */
import request from '@/utils/request'
import type { EmailTemplate } from '@/types/entity'

export function getTemplateList() {
  return request.get<EmailTemplate[]>('/template/template/list')
}

export function getTemplate(id: number) {
  return request.get<EmailTemplate>(`/template/template/${id}`)
}

export function createTemplate(data: Partial<EmailTemplate>) {
  return request.post<EmailTemplate>('/template/template', data)
}

export function updateTemplate(id: number, data: Partial<EmailTemplate>) {
  return request.put<EmailTemplate>(`/template/template/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete(`/template/template/${id}`)
}
