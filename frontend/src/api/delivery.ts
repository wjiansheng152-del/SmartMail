/**
 * 投递状态接口
 */
import request from '@/utils/request'
import type { DeliveryStatus } from '@/types/entity'

export function getDeliveryStatus(campaignId: number) {
  return request.get<DeliveryStatus>(`/delivery/delivery/status/${campaignId}`)
}
