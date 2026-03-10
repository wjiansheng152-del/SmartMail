<template>
  <div class="template-list">
    <h2 class="page-title">模板管理</h2>
    <el-card>
      <el-button type="primary" @click="handleCreate">新建模板</el-button>
      <el-table :data="list" style="width: 100%; margin-top: 16px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="subject" label="主题" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑模板' : '新建模板'" width="700px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="模板名称" />
        </el-form-item>
        <el-form-item label="主题" prop="subject">
          <el-input v-model="form.subject" placeholder="邮件主题" />
        </el-form-item>
        <el-form-item label="正文" prop="bodyHtml">
          <el-input v-model="form.bodyHtml" type="textarea" :rows="10" placeholder="HTML 正文，可使用变量如 {{name}}" />
        </el-form-item>
        <el-form-item label="变量" prop="variables">
          <el-input v-model="form.variables" placeholder="可选，如 name,company" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="预览" width="800px">
      <div class="preview-body" v-html="previewHtml" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'
import type { EmailTemplate } from '@/types/entity'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<EmailTemplate[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewHtml = ref('')
const editId = ref<number | null>(null)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = ref<Partial<EmailTemplate>>({
  name: '',
  subject: '',
  bodyHtml: '',
  variables: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入主题', trigger: 'blur' }],
  bodyHtml: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await request.get<EmailTemplate[]>('/template/template/list')
    list.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  editId.value = null
  form.value = { name: '', subject: '', bodyHtml: '', variables: '' }
  dialogVisible.value = true
}

function handleEdit(row: EmailTemplate) {
  editId.value = row.id ?? null
  form.value = { ...row }
  dialogVisible.value = true
}

function handlePreview(row: EmailTemplate) {
  previewHtml.value = row.bodyHtml ?? ''
  previewVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (editId.value != null) {
        await request.put(`/template/template/${editId.value}`, form.value)
        ElMessage.success('更新成功')
      } else {
        await request.post('/template/template', form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row: EmailTemplate) {
  await ElMessageBox.confirm('确定删除该模板？', '提示', {
    type: 'warning',
  })
  await request.delete(`/template/template/${row.id}`)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.template-list {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
.preview-body {
  max-height: 60vh;
  overflow: auto;
  border: 1px solid #eee;
  padding: 12px;
}
</style>
