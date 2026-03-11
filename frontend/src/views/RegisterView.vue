<template>
  <div class="register-page">
    <div class="register-card">
      <h1 class="title">SmartMail</h1>
      <p class="subtitle">注册新账号（注册即创建新租户）</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名（1~64 字符）"
            :prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（至少 6 位）"
            :prefix-icon="Lock"
            size="large"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            clearable
            @keyup.enter="handleRegister"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit-btn"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      <p class="footer-link">
        <router-link to="/login">已有账号？去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { register as registerApi } from '@/api/auth'
import type { RegisterRequest } from '@/types/api'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')

const form = reactive<RegisterRequest & { confirmPassword: string }>({
  username: '',
  password: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 64, message: '用户名长度 1~64 字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    errorMsg.value = ''
    try {
      await registerApi({ username: form.username, password: form.password })
      router.replace({ path: '/login', query: { registered: '1' } })
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { errorInfo?: string } } })?.response?.data
      errorMsg.value = res?.errorInfo ?? '注册失败，请稍后重试'
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}
.register-card {
  width: 360px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
.title {
  margin: 0 0 8px;
  font-size: 28px;
  text-align: center;
  color: #1a1a2e;
}
.subtitle {
  margin: 0 0 32px;
  text-align: center;
  color: #666;
  font-size: 14px;
}
.register-form {
  margin-top: 24px;
}
.submit-btn {
  width: 100%;
}
.error-msg {
  margin: 12px 0 0;
  color: var(--el-color-danger);
  font-size: 13px;
  text-align: center;
}
.footer-link {
  margin: 20px 0 0;
  text-align: center;
  font-size: 14px;
}
.footer-link a {
  color: var(--el-color-primary);
  text-decoration: none;
}
.footer-link a:hover {
  text-decoration: underline;
}
</style>
