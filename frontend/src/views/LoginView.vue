<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">SmartMail</h1>
      <p class="subtitle">邮件营销系统</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      <p class="footer-link">
        <router-link to="/register">没有账号？去注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { LoginRequest } from '@/types/api'

const router = useRouter()
const route = useRoute()
const store = useStore<{ user: { accessToken: string } }>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

onMounted(() => {
  if (route.query.registered === '1') {
    successMsg.value = '注册成功，请登录'
    router.replace({ path: '/login' }).catch(() => {})
  }
})

const form = reactive<LoginRequest>({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    errorMsg.value = ''
    try {
      await store.dispatch('user/login', form)
      router.replace('/')
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { errorInfo?: string } } })?.response?.data
      errorMsg.value = res?.errorInfo ?? '登录失败，请检查用户名和密码'
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}
.login-card {
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
.login-form {
  margin-top: 24px;
}
.submit-btn {
  width: 100%;
}
.success-msg {
  margin: 12px 0 0;
  color: var(--el-color-success);
  font-size: 13px;
  text-align: center;
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
