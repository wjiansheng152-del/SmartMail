<template>
  <el-container class="main-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">SmartMail</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1a1a2e"
        text-color="#e0e0e0"
        active-text-color="#fff"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/templates">
          <el-icon><Document /></el-icon>
          <span>模板管理</span>
        </el-menu-item>
        <el-menu-item index="/contacts">
          <el-icon><UserFilled /></el-icon>
          <span>客户与分组</span>
        </el-menu-item>
        <el-menu-item index="/campaigns">
          <el-icon><Promotion /></el-icon>
          <span>营销活动</span>
        </el-menu-item>
        <el-menu-item index="/schedules">
          <el-icon><Clock /></el-icon>
          <span>定时发送</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/audit">
          <el-icon><List /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/settings/smtp">
          <el-icon><Setting /></el-icon>
          <span>发信设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container direction="vertical">
      <el-header class="header">
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-right">
          <span class="tenant">租户: {{ tenantId }}</span>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-name">
              {{ username }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import {
  Odometer,
  Document,
  UserFilled,
  Promotion,
  Clock,
  DataAnalysis,
  List,
  ArrowDown,
  Setting,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const store = useStore<{
  user: { username: string; tenantId: string }
}>()

const activeMenu = computed(() => route.path)
const username = computed(() => store.state.user.username || '用户')
const tenantId = computed(() => store.state.user.tenantId || 'default')

const titleMap: Record<string, string> = {
  '/': '工作台',
  '/templates': '模板管理',
  '/contacts': '客户与分组',
  '/campaigns': '营销活动',
  '/schedules': '定时发送',
  '/stats': '数据统计',
  '/audit': '审计日志',
  '/settings/smtp': '发信设置',
}
const currentTitle = computed(() => titleMap[route.path] ?? 'SmartMail')

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    store.dispatch('user/logout')
    router.replace('/login')
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.aside {
  background-color: #1a1a2e;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
  padding: 0 24px;
}
.header-title {
  font-size: 18px;
  font-weight: 500;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.tenant {
  font-size: 12px;
  color: #888;
}
.user-name {
  cursor: pointer;
  font-size: 14px;
}
.main {
  background: #f5f6fa;
  padding: 20px;
  overflow: auto;
}
</style>
