<template>
  <div class="stats-view">
    <h2 class="page-title">数据统计</h2>
    <el-card>
      <el-form inline>
        <el-form-item label="选择活动">
          <el-select v-model="campaignId" placeholder="选择活动" style="width: 280px" @change="loadStats">
            <el-option
              v-for="c in campaignList"
              :key="c.id"
              :label="c.name"
              :value="c.id!"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template v-if="campaignId">
        <h4>投递状态</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="总数">{{ deliveryStatus?.total ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="已发送">{{ deliveryStatus?.sent ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="失败">{{ deliveryStatus?.failed ?? 0 }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top: 24px">追踪统计</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="打开数">{{ trackingStats?.openCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="点击数">{{ trackingStats?.clickCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="打开率">
            {{ openRate }}
          </el-descriptions-item>
          <el-descriptions-item label="点击率">
            {{ clickRate }}
          </el-descriptions-item>
        </el-descriptions>
        <p class="tip">投递状态来自 campaign_batch 汇总，打开/点击统计来自追踪埋点。</p>
      </template>
      <p v-else class="tip">请先选择活动查看统计。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as campaignApi from '@/api/campaign'
import * as deliveryApi from '@/api/delivery'
import * as trackingApi from '@/api/tracking'
import type { Campaign } from '@/types/entity'
import type { DeliveryStatus, TrackingStats } from '@/types/entity'

const campaignList = ref<Campaign[]>([])
const campaignId = ref<number | null>(null)
const deliveryStatus = ref<DeliveryStatus | null>(null)
const trackingStats = ref<TrackingStats | null>(null)

const openRate = computed(() => {
  const total = deliveryStatus.value?.sent ?? 0
  const open = trackingStats.value?.openCount ?? 0
  if (total <= 0) return '-'
  return `${((open / total) * 100).toFixed(2)}%`
})

const clickRate = computed(() => {
  const total = deliveryStatus.value?.sent ?? 0
  const click = trackingStats.value?.clickCount ?? 0
  if (total <= 0) return '-'
  return `${((click / total) * 100).toFixed(2)}%`
})

async function loadCampaigns() {
  const res = await campaignApi.getCampaignList()
  campaignList.value = Array.isArray(res.data) ? res.data : []
  if (campaignList.value.length && campaignId.value == null) {
    const first = campaignList.value[0]
    campaignId.value = first?.id ?? null
  }
}

async function loadStats() {
  if (campaignId.value == null) return
  try {
    const [dRes, tRes] = await Promise.all([
      deliveryApi.getDeliveryStatus(campaignId.value),
      trackingApi.getTrackingStats(campaignId.value),
    ])
    deliveryStatus.value = dRes.data
    trackingStats.value = tRes.data
  } catch {
    deliveryStatus.value = null
    trackingStats.value = null
  }
}

onMounted(async () => {
  await loadCampaigns()
  if (campaignId.value != null) loadStats()
})
</script>

<style scoped>
.stats-view {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
.tip {
  margin-top: 16px;
  color: #888;
  font-size: 13px;
}
</style>
