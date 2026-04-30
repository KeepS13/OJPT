import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

const normalizePath = (id: string) => id.replaceAll('\\', '/')

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    host: '127.0.0.1',
    port: 8110,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8111',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true, // Clear output dir before build.
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = normalizePath(id)

          if (!normalizedId.includes('/node_modules/')) {
            return
          }

          if (
            normalizedId.includes('/node_modules/vue/') ||
            normalizedId.includes('/node_modules/@vue/') ||
            normalizedId.includes('/node_modules/vue-router/') ||
            normalizedId.includes('/node_modules/pinia/')
          ) {
            return 'vendor-vue'
          }

          if (normalizedId.includes('/node_modules/@element-plus/icons-vue/')) {
            return 'element-plus-icons'
          }

          if (normalizedId.includes('/node_modules/element-plus/')) {
            const componentMatch = normalizedId.match(/\/node_modules\/element-plus\/es\/components\/([^/]+)/)
            if (!componentMatch) {
              return 'element-plus-core'
            }

            const componentName = componentMatch[1]
            if (
              [
                'cascader',
                'cascader-panel',
                'date-picker',
                'date-picker-panel',
                'select',
                'select-v2',
                'table',
                'table-v2',
                'time-picker',
                'tree',
                'tree-v2',
                'virtual-list',
              ].includes(componentName)
            ) {
              return 'element-plus-data'
            }

            return 'element-plus-components'
          }

          if (
            normalizedId.includes('/node_modules/vee-validate/') ||
            normalizedId.includes('/node_modules/@vee-validate/') ||
            normalizedId.includes('/node_modules/yup/')
          ) {
            return 'vendor-validation'
          }

          if (normalizedId.includes('/node_modules/axios/')) {
            return 'vendor-axios'
          }

          if (
            normalizedId.includes('/node_modules/marked/') ||
            normalizedId.includes('/node_modules/dompurify/')
          ) {
            return 'vendor-markdown'
          }

          return 'vendor'
        },
      },
    },
  },
})
