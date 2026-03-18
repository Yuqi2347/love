<template>
  <div class="app-empty-state">
    <div v-if="icon" class="empty-icon">{{ icon }}</div>
    <slot name="icon" v-else />
    <p class="empty-text">{{ text }}</p>
    <p v-if="hint" class="empty-hint">{{ hint }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  text: string
  hint?: string
}>()
</script>

<style lang="scss" scoped>
.app-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  gap: 8px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.empty-text {
  color: $text-muted;
  font-size: 15px;
}

.empty-hint {
  color: $text-muted;
  font-size: 13px;
}
</style>
