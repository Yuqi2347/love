import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') },
  },
  server: {
    port: 5174,
    allowedHosts: ['campal.social', 'www.campal.social', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
        secure: false,
      },
      '/uploads': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/uploads/, '/api/uploads'),
      },
    },
  },
  css: {
    preprocessorOptions: {
      scss: { additionalData: `@use "@/styles/variables" as *;` },
    },
  },
})
