/**
 * 应用入口：Vue 3 + Vuex + Vue Router + Element Plus，并注入请求层鉴权
 */
import './assets/main.css'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { store } from './store'
import { setAuthProvider } from '@/utils/request'

const app = createApp(App)
app.use(store)
app.use(router)
app.use(ElementPlus)

// 注入鉴权提供者，供 request 拦截器获取 Token、刷新、登出
setAuthProvider({
  getAccessToken: () => store.state.user.accessToken,
  getTenantId: () => store.state.user.tenantId,
  refreshToken: () => store.dispatch('user/refreshToken'),
  logout: () => store.dispatch('user/logout'),
})

app.mount('#app')
