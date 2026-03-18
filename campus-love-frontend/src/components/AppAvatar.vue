<template>
  <img
    :src="resolvedSrc"
    :class="['app-avatar', sizeClass]"
    :width="size"
    :height="size"
    :alt="alt"
    @error="onError"
    @click="$emit('click', $event)"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { buildDefaultAvatar, getMediaUrl } from '@/utils/shared'

const props = withDefaults(defineProps<{
  src?: string | null
  name?: string | null
  size?: number
  alt?: string
}>(), {
  src: null,
  name: null,
  size: 40,
  alt: '',
})

defineEmits<{
  click: [e: MouseEvent]
}>()

const hasFailed = ref(false)

const resolvedSrc = computed(() => {
  const fallback = buildDefaultAvatar(props.name)
  if (hasFailed.value) return fallback
  const url = getMediaUrl(props.src ?? null)
  return url || fallback
})

const sizeClass = computed(() => {
  if (props.size <= 32) return 'avatar-sm'
  if (props.size >= 80) return 'avatar-lg'
  return ''
})

function onError() {
  hasFailed.value = true
}
</script>

<style lang="scss" scoped>
.app-avatar {
  border-radius: $radius-full;
  object-fit: cover;
  background: $bg-tertiary;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}
</style>
