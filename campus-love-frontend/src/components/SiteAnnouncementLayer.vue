<template>
  <Teleport to="body">
    <Transition name="ann-fade">
      <div v-if="visible && items.length" class="ann-overlay" role="dialog" aria-modal="true" aria-labelledby="ann-title">
        <div class="ann-backdrop" @click.self="onClose" />
        <div class="ann-shell">
          <button type="button" class="ann-close" aria-label="关闭并标记已读" @click="onClose">
            <el-icon :size="20"><Close /></el-icon>
          </button>

          <div class="ann-header">
            <div class="ann-badge">
              <el-icon class="ann-badge-icon"><Bell /></el-icon>
              <span>站点公告</span>
            </div>
            <h2 id="ann-title" class="ann-title">{{ current?.title }}</h2>
            <p class="ann-meta">
              有效期 {{ formatRange(current) }}
            </p>
          </div>

          <div class="ann-body">
            <div class="ann-content-scroll">
              <p class="ann-text">{{ current?.content }}</p>
            </div>
          </div>

          <div v-if="items.length > 1" class="ann-nav">
            <button
              type="button"
              class="ann-nav-btn"
              :disabled="slideIndex <= 0"
              aria-label="上一条"
              @click="slideIndex--"
            >
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <div class="ann-dots" role="tablist">
              <button
                v-for="(_, i) in items"
                :key="i"
                type="button"
                class="ann-dot"
                :class="{ active: i === slideIndex }"
                :aria-label="`第 ${i + 1} 条`"
                @click="slideIndex = i"
              />
            </div>
            <span class="ann-counter">{{ slideIndex + 1 }} / {{ items.length }}</span>
            <button
              type="button"
              class="ann-nav-btn"
              :disabled="slideIndex >= items.length - 1"
              aria-label="下一条"
              @click="slideIndex++"
            >
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <div class="ann-footer">
            <button type="button" class="ann-btn-primary" @click="onClose">知道了</button>
          </div>
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
  padding: $space-2xl $space-xl $space-md;
  padding-right: 52px;
  background: $primary-gradient;
  border-bottom: 1px solid $border-light;
}

.ann-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: $text-xs;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: $primary-dark;
  opacity: 0.9;
  margin-bottom: $space-sm;
}

.ann-badge-icon {
  color: $primary;
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

.ann-content-scroll {
  max-height: min(38vh, 320px);
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: rgba(215, 127, 162, 0.35) transparent;
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
  flex-wrap: wrap;
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

.ann-dots {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ann-dot {
  width: 8px;
  height: 8px;
  border-radius: $radius-full;
  border: none;
  padding: 0;
  background: rgba(215, 127, 162, 0.28);
  cursor: pointer;
  transition: transform $transition-fast, background $transition-fast;
  &.active {
    background: linear-gradient(135deg, $primary, $accent);
    transform: scale(1.25);
  }
}

.ann-counter {
  font-size: $text-sm;
  font-weight: 600;
  color: $text-secondary;
  min-width: 52px;
  text-align: center;
}

.ann-footer {
  padding: $space-md $space-xl $space-xl;
  border-top: 1px solid $border-light;
  background: rgba(255, 248, 251, 0.6);
}

.ann-btn-primary {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: $radius-md;
  font-size: $text-base;
  font-weight: 600;
  color: $text-inverse;
  cursor: pointer;
  background: linear-gradient(135deg, $primary, $primary-dark);
  box-shadow: $shadow-glow;
  transition: transform $transition-fast, filter $transition-fast;
  &:hover {
    filter: brightness(1.03);
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
