<template>
  <router-view />
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
