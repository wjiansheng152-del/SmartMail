/**
 * 客户、分组、退订、黑名单接口
 */
import request from '@/utils/request'
import type { IPage } from '@/types/api'
import type {
  Contact,
  ContactCreateRequest,
  ContactGroup,
  Unsubscribe,
  Blacklist,
} from '@/types/entity'

/** 客户分页 */
export function getContactPage(params: {
  page: number
  size: number
  groupId?: number
}) {
  return request.get<IPage<Contact>>('/contact/contact/page', { params })
}

/** 创建客户 */
export function createContact(data: ContactCreateRequest) {
  return request.post<Contact>('/contact/contact', data)
}

/** 删除客户 */
export function deleteContact(id: number) {
  return request.delete(`/contact/contact/${id}`)
}

/** 分组列表 */
export function getGroupList() {
  return request.get<ContactGroup[]>('/contact/group/list')
}

/** 创建分组 */
export function createGroup(data: Partial<ContactGroup>) {
  return request.post<ContactGroup>('/contact/group', data)
}

/** 删除分组 */
export function deleteGroup(id: number) {
  return request.delete(`/contact/group/${id}`)
}

/** 退订检查 */
export function checkUnsubscribe(email: string) {
  return request.get<boolean>('/contact/unsubscribe/check', { params: { email } })
}

/** 添加退订 */
export function addUnsubscribe(email: string, reason?: string) {
  return request.post<Unsubscribe>('/contact/unsubscribe', null, {
    params: { email, reason },
  })
}

/** 黑名单检查 */
export function checkBlacklist(email: string) {
  return request.get<boolean>('/contact/blacklist/check', { params: { email } })
}

/** 黑名单列表 */
export function getBlacklistList() {
  return request.get<Blacklist[]>('/contact/blacklist/list')
}

/** 添加黑名单 */
export function addBlacklist(email: string, source?: string) {
  return request.post<Blacklist>('/contact/blacklist', null, {
    params: { email, source },
  })
}
