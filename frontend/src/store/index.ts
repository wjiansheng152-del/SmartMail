/**
 * Vuex 根 store：登录态、Token、租户、用户信息
 */
import { createStore } from 'vuex'
import { user, type UserState } from './modules/user'

export interface RootState {
  user: UserState
}

export const store = createStore<RootState>({
  modules: {
    user,
  },
})
