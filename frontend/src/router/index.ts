/**
 * 路由配置：登录、主布局及各业务页面；未登录访问需鉴权页时重定向到登录
 */
import { createRouter, createWebHistory } from 'vue-router'
import { store } from '@/store'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/views/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        {
          path: 'templates',
          name: 'templates',
          component: () => import('@/views/TemplateListView.vue'),
        },
        {
          path: 'contacts',
          name: 'contacts',
          component: () => import('@/views/ContactListView.vue'),
        },
        {
          path: 'campaigns',
          name: 'campaigns',
          component: () => import('@/views/CampaignListView.vue'),
        },
        {
          path: 'schedules',
          name: 'schedules',
          component: () => import('@/views/ScheduleListView.vue'),
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/StatsView.vue'),
        },
        {
          path: 'audit',
          name: 'audit',
          component: () => import('@/views/AuditListView.vue'),
        },
        {
          path: 'settings/smtp',
          name: 'smtp-settings',
          component: () => import('@/views/SmtpConfigView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const loggedIn = !!(store.state.user?.accessToken)
  if (to.meta.requiresAuth && !loggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.guest && loggedIn && (to.path === '/login' || to.path === '/register')) {
    next('/')
    return
  }
  next()
})

export default router
