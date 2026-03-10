/// <reference types="vite/client" />

/** 环境变量类型 */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}
interface ImportMeta {
  readonly env: ImportMetaEnv
}

/** Vuex 4 模块解析补充（包内 types 未通过 exports 暴露时的兼容） */
declare module 'vuex' {
  import type { App, InjectionKey } from 'vue'
  export interface Store<S = any> {
    state: S
    getters: any
    dispatch(type: string, payload?: any): Promise<any>
    commit(type: string, payload?: any): void
    install(app: App): void
  }
  export function createStore<S>(options: any): Store<S>
  export function useStore<S = any>(injectKey?: InjectionKey<Store<S>> | string): Store<S>
}
