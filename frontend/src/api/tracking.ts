/**
 * 追踪统计接口
 */
import request from '@/utils/request'
import type { TrackingStats } from '@/types/entity'

export function getTrackingStats(campaignId: number) {
  return request.get<TrackingStats>(`/tracking/stats/${campaignId}`)
}
