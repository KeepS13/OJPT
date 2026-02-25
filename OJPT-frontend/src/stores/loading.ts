import { defineStore } from 'pinia'

/**
 * 全局 Loading 状态管理
 * 用于管理页面级别或组件级别的加载状态
 */
export const useLoadingStore = defineStore('loading', {
  state: () => ({
    // 全局加载状态
    global: false,
    // 模块级别的加载状态
    modules: {} as Record<string, boolean>,
    // 操作级别的加载状态（用于按钮等）
    operations: {} as Record<string, boolean>,
  }),
  
  getters: {
    /**
     * 检查某个模块是否正在加载
     */
    isModuleLoading: (state) => (module: string) => {
      return state.modules[module] || false
    },
    
    /**
     * 检查某个操作是否正在进行
     */
    isOperationLoading: (state) => (operation: string) => {
      return state.operations[operation] || false
    },
    
    /**
     * 是否有任何加载状态
     */
    isAnyLoading: (state) => {
      if (state.global) return true
      if (Object.values(state.modules).some(Boolean)) return true
      if (Object.values(state.operations).some(Boolean)) return true
      return false
    },
  },
  
  actions: {
    /**
     * 设置全局加载状态
     */
    setGlobalLoading(loading: boolean) {
      this.global = loading
    },
    
    /**
     * 开始模块加载
     */
    startModuleLoading(module: string) {
      this.modules[module] = true
    },
    
    /**
     * 结束模块加载
     */
    stopModuleLoading(module: string) {
      this.modules[module] = false
    },
    
    /**
     * 开始操作加载
     */
    startOperation(operation: string) {
      this.operations[operation] = true
    },
    
    /**
     * 结束操作加载
     */
    stopOperation(operation: string) {
      this.operations[operation] = false
    },
    
    /**
     * 重置所有加载状态
     */
    reset() {
      this.global = false
      this.modules = {}
      this.operations = {}
    },
  },
})

/**
 * 便捷的 loading wrapper 函数
 * 用于包装异步操作并自动管理加载状态
 */
export function withLoading<T>(
  operation: string,
  fn: () => Promise<T>
): Promise<T> {
  const store = useLoadingStore()
  store.startOperation(operation)
  return fn().finally(() => {
    store.stopOperation(operation)
  })
}
