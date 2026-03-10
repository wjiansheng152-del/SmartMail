/**
 * 调度接口：创建计划、计划列表
 */
import request from '@/utils/request'
import type { ScheduleCreateRequest, ScheduleJob } from '@/types/entity'

export function createSchedule(data: ScheduleCreateRequest) {
  return request.post<number>('/scheduler/schedule', data)
}

export function getScheduleList() {
  return request.get<ScheduleJob[]>('/scheduler/schedule/list')
}
