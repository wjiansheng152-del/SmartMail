<template>
  <div class="schedule-list">
    <h2 class="page-title">定时发送</h2>
    <el-card>
      <el-button type="primary" @click="showCreate">创建计划</el-button>
      <el-alert
        v-if="scheduleList.length === 0 && !loading"
        title="当前无发送计划，请点击「创建计划」添加"
        type="info"
        show-icon
        style="margin-top: 16px"
      />
      <el-table :data="scheduleList" v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="id" label="计划ID" width="90" />
        <el-table-column prop="campaignId" label="活动ID" width="90" />
        <el-table-column prop="cronExpr" label="Cron" width="140" />
        <el-table-column prop="runAt" label="执行时间" width="180" />
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" title="创建发送计划" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="活动" prop="campaignId">
          <el-select v-model="form.campaignId" placeholder="选择活动" style="width: 100%">
            <el-option
              v-for="c in campaignList"
              :key="c.id"
              :label="c.name"
              :value="c.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron 表达式" prop="cronExpr">
          <el-input v-model="form.cronExpr" placeholder="如 0 9 * * * 表示每天9点" />
        </el-form-item>
        <el-form-item label="或执行时间" prop="runAt">
          <el-input v-model="form.runAt" placeholder="一次性执行时间 yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as scheduleApi from '@/api/schedule'
import * as campaignApi from '@/api/campaign'
import type { Campaign, ScheduleJob } from '@/types/entity'

const scheduleList = ref<ScheduleJob[]>([])
const campaignList = ref<Campaign[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const store = useStore<{ user: { userId: number | null } }>()

const form = ref({ campaignId: 0, cronExpr: '', runAt: '' })
const formRules: FormRules = {
  campaignId: [{ required: true, message: '请选择活动', trigger: 'change' }],
}

async function loadSchedules() {
  loading.value = true
  try {
    const res = await scheduleApi.getScheduleList()
    scheduleList.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

async function loadCampaigns() {
  const res = await campaignApi.getCampaignList()
  campaignList.value = Array.isArray(res.data) ? res.data : []
}

function showCreate() {
  form.value = { campaignId: campaignList.value[0]?.id ?? 0, cronExpr: '', runAt: '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const campaign = campaignList.value.find((c) => c.id === form.value.campaignId)
      await scheduleApi.createSchedule({
        campaignId: form.value.campaignId,
        createdBy: campaign?.createdBy ?? store.state.user.userId ?? undefined,
        cronExpr: form.value.cronExpr || undefined,
        runAt: form.value.runAt || undefined,
      })
      ElMessage.success('计划已创建')
      dialogVisible.value = false
      loadSchedules()
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => {
  loadSchedules()
  loadCampaigns()
})
</script>

<style scoped>
.schedule-list {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
</style>
