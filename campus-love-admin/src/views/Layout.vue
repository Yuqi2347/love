<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">管理后台</div>
      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="#1a1a2e"
        text-color="#a0aec0"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/invites">
          <el-icon><Calendar /></el-icon>
          <span>邀约管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">{{ currentTitle }}</span>
        <div class="header-right">
          <span class="user-name">{{ userStore.user?.nickname || userStore.user?.email }}</span>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
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
import { useUserStore } from '@/store/userStore'
import { DataAnalysis, User, Calendar } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentTitle = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '仪表盘',
    '/users': '用户管理',
    '/invites': '邀约管理',
  }
  return map[route.path] || '管理后台'
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.layout { min-height: 100vh; }

.aside {
  background: #1a1a2e;
  .logo {
    height: 56px;
    line-height: 56px;
    padding-left: 20px;
    color: #fff;
    font-weight: 700;
    font-size: 16px;
  }
  .menu { border-right: none; }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: $bg-primary;
  border-bottom: 1px solid $border-light;

  .header-title { font-size: 18px; font-weight: 600; }
  .header-right { display: flex; align-items: center; gap: 16px; }
  .user-name { font-size: 14px; color: $text-secondary; }
}

.main {
  padding: 24px;
  background: $bg-secondary;
  min-height: calc(100vh - 56px);
}
</style>
