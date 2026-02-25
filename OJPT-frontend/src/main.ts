import './assets/main.scss'

// 导入 Vue 的核心函数
import { createApp } from 'vue'
// 导入应用主组件
import App from './App.vue'
// 导入前端路由配置，实现页面跳转
import router from './router'

// Pinia：Vue 推荐的状态管理库，用于全局状态管理
import { createPinia } from 'pinia'

// Element Plus：基于 Vue 3 的组件库（UI 框架），用于快速构建美观的页面；引入其 JS 和样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 创建 Vue 应用实例
const app = createApp(App)

// 创建 pinia 实例，并注册到当前应用，实现全局状态管理
const pinia = createPinia()
app.use(pinia)

// 应用路由，为 SPA 提供页面导航能力
app.use(router)

// 应用 Element Plus，使组件库可用于全局
app.use(ElementPlus)

// 挂载 Vue 应用到页面上
app.mount('#app')
