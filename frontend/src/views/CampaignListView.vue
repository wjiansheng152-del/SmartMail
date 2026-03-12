<template>
  <div class="campaign-list">
    <h2 class="page-title">营销活动</h2>
    <el-card>
      <el-button type="primary" @click="handleCreate">新建活动</el-button>
      <el-table :data="list" v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="templateId" label="模板ID" width="90" />
        <el-table-column prop="groupId" label="分组ID" width="90" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleSendNow(row)">立即发送</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 新建/编辑活动 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑活动' : '新建活动'" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="活动名称" />
        </el-form-item>
        <el-form-item label="模板" prop="templateId">
          <el-select v-model="form.templateId" placeholder="选择模板" style="width: 100%">
            <el-option
              v-for="t in templateList"
              :key="t.id"
              :label="t.name"
              :value="t.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分组" prop="groupId">
          <el-select v-model="form.groupId" placeholder="选择分组" style="width: 100%">
            <el-option
              v-for="g in groupList"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="状态" style="width: 100%">
            <el-option label="草稿" value="draft" />
            <el-option label="已计划" value="scheduled" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="活动详情" width="560px">
      <template v-if="currentCampaign">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ currentCampaign.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentCampaign.name }}</el-descriptions-item>
          <el-descriptions-item label="模板ID">{{ currentCampaign.templateId }}</el-descriptions-item>
          <el-descriptions-item label="分组ID">{{ currentCampaign.groupId }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentCampaign.status }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentCampaign.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as campaignApi from '@/api/campaign'
import * as scheduleApi from '@/api/schedule'
import * as templateApi from '@/api/template'
import * as contactApi from '@/api/contact'
import type { Campaign } from '@/types/entity'
import type { EmailTemplate } from '@/types/entity'
import type { ContactGroup } from '@/types/entity'
import { formatUtcRunAtFromLocalDate } from '@/utils/datetime'

const list = ref<Campaign[]>([])
const templateList = ref<EmailTemplate[]>([])
const groupList = ref<ContactGroup[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const currentCampaign = ref<Campaign | null>(null)
const submitLoading = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const store = useStore<{ user: { userId: number | null } }>()

const form = ref<Partial<Campaign>>({
  name: '',
  templateId: 0,
  groupId: 0,
  status: 'draft',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  templateId: [{ required: true, message: '请选择模板', trigger: 'change' }],
  groupId: [{ required: true, message: '请选择分组', trigger: 'change' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await campaignApi.getCampaignList()
    list.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [tRes, gRes] = await Promise.all([
    templateApi.getTemplateList(),
    contactApi.getGroupList(),
  ])
  templateList.value = Array.isArray(tRes.data) ? tRes.data : []
  groupList.value = Array.isArray(gRes.data) ? gRes.data : []
}

function handleCreate() {
  editId.value = null
  form.value = {
    name: '',
    templateId: templateList.value[0]?.id ?? 0,
    groupId: groupList.value[0]?.id ?? 0,
    status: 'draft',
  }
  dialogVisible.value = true
}

function handleEdit(row: Campaign) {
  editId.value = row.id ?? null
  form.value = {
    name: row.name,
    templateId: row.templateId,
    groupId: row.groupId,
    status: row.status ?? 'draft',
  }
  dialogVisible.value = true
}

/**
 * 处理「立即发送」：
 * - 使用浏览器本地时间（例如北京时间）计算「当前时间 + 1 分钟」；
 * - 提交给后端 scheduler 前，将本地时间转换为 UTC 字符串，
 *   避免 Docker 中按 UTC 运行的调度服务出现 8 小时时差。
 */
async function handleSendNow(row: Campaign) {
  await ElMessageBox.confirm(
    '确定立即发送该活动？将创建一条约 1 分钟后执行的计划，由调度服务自动触发发送。',
    '立即发送',
    { type: 'warning' }
  )
  const runAt = new Date()
  runAt.setMinutes(runAt.getMinutes() + 1)
  await scheduleApi.createSchedule({
    campaignId: row.id!,
    createdBy: row.createdBy ?? store.state.user.userId ?? undefined,
    // 关键：将本地时间转换为 UTC 字符串再提交给后端
    runAt: formatUtcRunAtFromLocalDate(runAt),
  })
  ElMessage.success('已创建发送计划，约 1 分钟后执行')
}

async function handleDelete(row: Campaign) {
  await ElMessageBox.confirm('确定删除该营销活动？', '提示', { type: 'warning' })
  await campaignApi.deleteCampaign(row.id!)
  ElMessage.success('已删除')
  loadList()
}

async function handleSubmit() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (editId.value != null) {
        await campaignApi.updateCampaign(editId.value, form.value)
        ElMessage.success('更新成功')
      } else {
        await campaignApi.createCampaign(form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitLoading.value = false
    }
  })
}

function handleDetail(row: Campaign) {
  currentCampaign.value = { ...row }
  detailVisible.value = true
}

onMounted(() => {
  loadList()
  loadOptions()
})
</script>

<style scoped>
.campaign-list {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
</style>
