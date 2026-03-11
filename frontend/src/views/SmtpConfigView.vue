<template>
  <div class="smtp-config">
    <h2 class="page-title">发信设置（SMTP）</h2>
    <el-card>
      <p class="desc">配置本账号的 SMTP 发信服务器，创建的活动将使用您的配置发送邮件；不配置则使用系统默认发信。</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        style="max-width: 520px; margin-top: 16px"
      >
        <el-form-item label="SMTP 主机" prop="host">
          <el-input v-model="form.host" placeholder="如 smtp.example.com" clearable />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" placeholder="25 或 465" style="width: 100%" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="SMTP 登录用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="不修改请留空"
            clearable
          />
        </el-form-item>
        <el-form-item label="发件人邮箱" prop="fromEmail">
          <el-input v-model="form.fromEmail" placeholder="如 noreply@example.com" clearable />
        </el-form-item>
        <el-form-item label="发件人名称" prop="fromName">
          <el-input v-model="form.fromName" placeholder="如 智能邮件" clearable />
        </el-form-item>
        <el-form-item label="使用 SSL" prop="useSsl">
          <el-switch v-model="form.useSsl" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as deliveryApi from '@/api/delivery'
import type { SmtpConfigDto } from '@/api/delivery'

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const form = reactive<SmtpConfigDto & { password?: string }>({
  host: '',
  port: 25,
  username: '',
  password: '',
  fromEmail: '',
  fromName: '',
  useSsl: false,
})

const formRules: FormRules = {
  host: [{ required: true, message: '请输入 SMTP 主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
}

/** 拉取当前用户配置并回填表单；密码显示占位，提交时不传则后端不更新 */
async function loadConfig() {
  try {
    const data = await deliveryApi.getSmtpConfig()
    if (data && typeof data === 'object') {
      form.host = data.host ?? ''
      form.port = data.port ?? 25
      form.username = data.username ?? ''
      form.password = ''
      form.fromEmail = data.fromEmail ?? ''
      form.fromName = data.fromName ?? ''
      form.useSsl = !!data.useSsl
    }
  } catch (e: unknown) {
    const errData = (e as { response?: { data?: { errorInfo?: string } } })?.response?.data
    ElMessage.error(errData?.errorInfo || '获取配置失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate().catch(() => {})
  submitLoading.value = true
  try {
    const payload: SmtpConfigDto = {
      host: form.host?.trim() ?? '',
      port: form.port ?? 25,
      username: form.username?.trim() ?? '',
      fromEmail: form.fromEmail?.trim() ?? '',
      fromName: form.fromName?.trim() ?? '',
      useSsl: form.useSsl,
    }
    if (form.password != null && form.password !== '') {
      payload.password = form.password
    }
    await deliveryApi.saveSmtpConfig(payload)
    ElMessage.success('保存成功')
    await loadConfig()
  } catch (e: unknown) {
    const errData = (e as { response?: { data?: { errorInfo?: string } } })?.response?.data
    ElMessage.error(errData?.errorInfo || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.smtp-config {
  padding: 0;
}
.page-title {
  margin: 0 0 16px 0;
  font-size: 18px;
}
.desc {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0;
}
</style>
