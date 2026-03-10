<template>
  <div class="dashboard">
    <h2 class="page-title">工作台</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">营销活动</div>
          <div class="stat-value">{{ campaignCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">邮件模板</div>
          <div class="stat-value">{{ templateCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">客户分组</div>
          <div class="stat-value">{{ groupCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日发送</div>
          <div class="stat-value">{{ todaySent }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="welcome-card">
      <p>欢迎使用 SmartMail 邮件营销系统。请从左侧菜单进入模板管理、客户分组、营销活动等模块。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const campaignCount = ref(0)
const templateCount = ref(0)
const groupCount = ref(0)
const todaySent = ref(0)

onMounted(async () => {
  try {
    const [campaignRes, templateRes, groupRes] = await Promise.all([
      request.get<unknown[]>('/campaign/campaign/list'),
      request.get<unknown[]>('/template/template/list'),
      request.get<unknown[]>('/contact/group/list'),
    ])
    campaignCount.value = Array.isArray(campaignRes.data) ? campaignRes.data.length : 0
    templateCount.value = Array.isArray(templateRes.data) ? templateRes.data.length : 0
    groupCount.value = Array.isArray(groupRes.data) ? groupRes.data.length : 0
    // 今日发送暂无后端统计接口，先占位
    todaySent.value = 0
  } catch {
    // 忽略错误，保持 0
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
.stat-card {
  margin-bottom: 20px;
}
.stat-label {
  font-size: 14px;
  color: #888;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a2e;
}
.welcome-card {
  margin-top: 20px;
}
</style>
