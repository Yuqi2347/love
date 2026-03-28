<template>
  <Teleport to="body">
    <Transition name="ann-fade">
      <div v-if="visible && items.length" class="ann-overlay" role="dialog" aria-modal="true">
        <div class="ann-backdrop" @click.self="onClose" />
        <div class="ann-shell glass-panel">
          <button type="button" class="ann-close" @click="onClose">
            <el-icon><Close /></el-icon>
          </button>

          <div class="ann-header">
            <h2 class="ann-title">{{ current?.title }}</h2>
            <p class="ann-meta">{{ formatRange(current) }}</p>
          </div>

          <div class="ann-body">
            <p class="ann-text">{{ current?.content }}</p>
          </div>

          <div v-if="items.length > 1" class="ann-nav">
            <button type="button" class="ann-nav-btn" :disabled="slideIndex <= 0" @click="slideIndex--">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span class="ann-counter">{{ slideIndex + 1 }} / {{ items.length }}</span>
            <button type="button" class="ann-nav-btn" :disabled="slideIndex >= items.length - 1" @click="slideIndex++">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <button type="button" class="ann-btn" @click="onClose">知道了</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Close, Bell, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getUnreadAnnouncements, dismissAnnouncements, type SiteAnnouncement } from '@/api/announcementApi'

const visible = ref(false)
const items = ref<SiteAnnouncement[]>([])
const slideIndex = ref(0)

const current = computed(() => items.value[slideIndex.value] ?? null)

function formatRange(a: SiteAnnouncement | null) {
  if (!a) return ''
  const f = formatShort(a.validFrom)
  const t = formatShort(a.validUntil)
  return `${f} — ${t}`
}

function formatShort(iso: string) {
  try {
    const d = new Date(iso)
    return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}

async function load() {
  try {
    const res = await getUnreadAnnouncements()
    const list = res.data.data ?? []
    if (list.length) {
      items.value = list
      slideIndex.value = 0
      visible.value = true
    } else {
      items.value = []
      visible.value = false
    }
  } catch {
    /* 未登录或网络错误时静默 */
  }
}

async function onClose() {
  const ids = items.value.map((x) => x.id)
  visible.value = false
  if (ids.length) {
    try {
      await dismissAnnouncements(ids)
    } catch {
      /* 已关闭 UI，失败仅影响已读状态 */
    }
  }
  items.value = []
}

watch(slideIndex, (i) => {
  if (i < 0) slideIndex.value = 0
  if (items.value.length && i >= items.value.length) slideIndex.value = items.value.length - 1
})

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && visible.value) {
    e.preventDefault()
    onClose()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))

defineExpose({ load })
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.ann-overlay {
  position: fixed;
  inset: 0;
  z-index: 5000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $space-lg;
  pointer-events: auto;
}

.ann-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(79, 57, 65, 0.42);
  backdrop-filter: blur(10px);
}

.ann-shell {
  position: relative;
  width: 100%;
  max-width: 440px;
  max-height: min(86vh, 640px);
  display: flex;
  flex-direction: column;
  border-radius: $radius-xl;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.97), rgba(255, 248, 251, 0.98));
  box-shadow:
    $shadow-lg,
    0 0 0 1px rgba(215, 127, 162, 0.18),
    0 24px 48px rgba(215, 127, 162, 0.15);
  overflow: hidden;
}

.ann-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: $radius-full;
  background: rgba(255, 255, 255, 0.85);
  color: $text-secondary;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background $transition-fast, color $transition-fast, transform $transition-fast;
  &:hover {
    background: $bg-tertiary;
    color: $text-primary;
    transform: scale(1.05);
  }
}

.ann-header {
  padding: $space-xl $space-xl $space-md;
  padding-right: 52px;
  border-bottom: 1px solid $border-light;
}

.ann-title {
  margin: 0;
  font-size: $text-xl;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.35;
}

.ann-meta {
  margin: $space-sm 0 0;
  font-size: $text-sm;
  color: $text-muted;
}

.ann-body {
  flex: 1;
  min-height: 0;
  padding: $space-lg $space-xl;
}

.ann-text {
  margin: 0;
  font-size: $text-base;
  line-height: 1.65;
  color: $text-primary;
  white-space: pre-wrap;
  word-break: break-word;
}

.ann-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $space-md;
  padding: 0 $space-xl $space-md;
}

.ann-nav-btn {
  width: 40px;
  height: 40px;
  border-radius: $radius-full;
  border: 1px solid $border-color;
  background: $bg-primary;
  color: $primary;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color $transition-fast, box-shadow $transition-fast;
  &:hover:not(:disabled) {
    border-color: $primary-light;
    box-shadow: $shadow-sm;
  }
  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

.ann-counter {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-secondary;
  min-width: 52px;
  text-align: center;
}

.ann-btn {
  width: 100%;
  height: 44px;
  margin: $space-md $space-xl $space-lg;
  border: none;
  border-radius: $radius-md;
  font-size: $text-base;
  font-weight: 600;
  color: $text-inverse;
  cursor: pointer;
  background: linear-gradient(135deg, $primary, $primary-dark);
  box-shadow: $shadow-sm;
  transition: transform $transition-fast, box-shadow $transition-fast;
  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-1px);
  }
}

.ann-fade-enter-active,
.ann-fade-leave-active {
  transition: opacity 0.28s ease;
}
.ann-fade-enter-from,
.ann-fade-leave-to {
  opacity: 0;
}
</style>
