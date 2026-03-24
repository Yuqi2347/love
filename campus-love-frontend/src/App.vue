<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page" mode="out-in">
      <keep-alive v-if="route.meta.keepAlive">
        <component :is="Component" :key="String(route.name || route.path)" />
      </keep-alive>
      <component v-else :is="Component" :key="route.fullPath" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/store/userStore'
import { useChatStore } from '@/store/chatStore'

const userStore = useUserStore()
const chatStore = useChatStore()

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.fetchProfile()
    chatStore.connectWebSocket()
  }
})
</script>

<style lang="scss">
.page-enter-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-active {
  transition: opacity 0.15s ease;
}
.page-leave-to {
  opacity: 0;
}
</style>
