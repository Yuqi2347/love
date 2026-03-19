<template>
  <Teleport to="body">
    <transition name="base-modal-fade">
      <div v-if="modelValue" class="base-modal-root" role="presentation">
        <div class="base-modal-backdrop" @click="handleBackdropClick" />
        <div
          class="base-modal-panel"
          :style="panelStyle"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
        >
          <div class="base-modal-header">
            <div class="base-modal-title">{{ title }}</div>
            <button type="button" class="base-modal-close" aria-label="关闭" @click="close">
              <span aria-hidden="true">×</span>
            </button>
          </div>
          <div class="base-modal-body" :style="bodyStyle">
            <slot />
          </div>
          <div v-if="$slots.footer" class="base-modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  width?: string
  maxBodyHeight?: string
  closeOnBackdrop?: boolean
  lockScroll?: boolean
}>(), {
  width: '520px',
  maxBodyHeight: '70vh',
  closeOnBackdrop: true,
  lockScroll: true,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

let lockCount = 0
let previousOverflow = ''
let previousPaddingRight = ''

const panelStyle = computed(() => ({
  width: `min(calc(100vw - 32px), ${props.width})`,
}))

const bodyStyle = computed(() => ({
  maxHeight: props.maxBodyHeight,
}))

function close() {
  emit('update:modelValue', false)
}

function handleBackdropClick() {
  if (props.closeOnBackdrop) {
    close()
  }
}

function lockBodyScroll() {
  if (!props.lockScroll || typeof document === 'undefined') return
  if (lockCount === 0) {
    previousOverflow = document.body.style.overflow
    previousPaddingRight = document.body.style.paddingRight
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth
    document.body.style.overflow = 'hidden'
    if (scrollbarWidth > 0) {
      document.body.style.paddingRight = `${scrollbarWidth}px`
    }
  }
  lockCount += 1
}

function unlockBodyScroll() {
  if (!props.lockScroll || typeof document === 'undefined' || lockCount === 0) return
  lockCount -= 1
  if (lockCount === 0) {
    document.body.style.overflow = previousOverflow
    document.body.style.paddingRight = previousPaddingRight
  }
}

watch(
  () => props.modelValue,
  (visible, previousVisible) => {
    if (visible === previousVisible) return
    if (visible) {
      lockBodyScroll()
      return
    }
    unlockBodyScroll()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (props.modelValue) {
    unlockBodyScroll()
  }
})
</script>

<style scoped lang="scss">
.base-modal-fade-enter-active,
.base-modal-fade-leave-active {
  transition: opacity 0.18s ease;
}

.base-modal-fade-enter-from,
.base-modal-fade-leave-to {
  opacity: 0;
}

.base-modal-root {
  position: fixed;
  inset: 0;
  z-index: 1800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.base-modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
}

.base-modal-panel {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 32px);
  border-radius: $radius-xl;
  background: $bg-primary;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
  overflow: hidden;
}

.base-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid $border-light;
}

.base-modal-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.base-modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: $text-secondary;
  transition: background 0.18s ease, color 0.18s ease;

  &:hover {
    background: $bg-secondary;
    color: $text-primary;
  }
}

.base-modal-body {
  overflow-y: auto;
  padding: 18px 20px;
}

.base-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 20px 18px;
  border-top: 1px solid $border-light;
  background: rgba($bg-secondary, 0.7);
}

@media (max-width: $bp-mobile) {
  .base-modal-root {
    align-items: flex-end;
    padding: 0;
  }

  .base-modal-panel {
    width: 100% !important;
    max-height: 82vh;
    border-bottom-left-radius: 0;
    border-bottom-right-radius: 0;
  }

  .base-modal-body {
    padding: 16px;
  }

  .base-modal-footer {
    padding: 12px 16px 16px;
  }
}
</style>
