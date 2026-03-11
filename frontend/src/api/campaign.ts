/**
 * 营销活动接口
 */
import request from '@/utils/request'
import type { Campaign } from '@/types/entity'

export function getCampaignList() {
  return request.get<Campaign[]>('/campaign/campaign/list')
}

export function getCampaign(id: number) {
  return request.get<Campaign>(`/campaign/campaign/${id}`)
}

export function createCampaign(data: Partial<Campaign>) {
  return request.post<Campaign>('/campaign/campaign', data)
}

/** 全量更新活动 */
export function updateCampaign(id: number, data: Partial<Campaign>) {
  return request.put<Campaign>(`/campaign/campaign/${id}`, data)
}

/** 删除营销活动 */
export function deleteCampaign(id: number) {
  return request.delete(`/campaign/campaign/${id}`)
}
