<template>
  <div class="app-tab-bar">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      :class="['tab-btn', { active: modelValue === tab.value }]"
      @click="$emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
export interface TabItem {
  label: string
  value: string
}

defineProps<{
  tabs: TabItem[]
  modelValue: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<style lang="scss" scoped>
.app-tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-radius: $radius-full;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.08);
    color: $primary;
  }

  &.active {
    background: $primary;
    color: white;
    box-shadow: 0 4px 12px rgba($primary, 0.3);
  }
}
</style>
