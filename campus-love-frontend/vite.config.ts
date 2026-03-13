import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5179,
    strictPort: true, // 端口被占用时直接报错，不自动换端口
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        ws: true,
        timeout: 60000, // 外网隧道延迟高，代理超时放宽
      },
      '/uploads': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/uploads/, '/api/uploads'),
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables" as *;`,
      },
    },
  },
})
