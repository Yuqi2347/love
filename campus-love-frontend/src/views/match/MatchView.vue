<template>
  <div class="match-page">
    <div class="page-header">
      <h2 class="page-title">匹配推荐</h2>
      <span class="page-subtitle">基于多维度画像精准匹配</span>
    </div>

    <div v-if="cards.length" class="card-stack">
      <div
class="swipe-card card"
        :style="currentCardStyle"
        @mousedown="startDrag"
        @touchstart.passive="startDrag">
        <div class="swipe-overlay like" :style="{ opacity: likeOpacity }">LIKE 💕</div>
        <div class="swipe-overlay nope" :style="{ opacity: nopeOpacity }">SKIP ✋</div>

        <img :src="currentCard.avatarUrl || defaultAvatar" class="swipe-avatar" />
        <div class="swipe-info">
          <div class="swipe-name">{{ currentCard.nickname }}</div>
          <div class="swipe-tags">
            <span v-if="currentCard.zodiac" class="swipe-tag">{{ currentCard.zodiac }}</span>
            <span v-if="currentCard.mbti" class="swipe-tag accent">{{ currentCard.mbti }}</span>
            <span v-if="currentCard.major" class="swipe-tag">{{ currentCard.major }}</span>
          </div>
          <p class="swipe-bio">{{ currentCard.bio || '这个人很神秘~' }}</p>

          <div class="match-score-bar">
            <div class="score-label">匹配度</div>
            <div class="score-track">
              <div class="score-fill" :style="{ width: currentCard.matchScore + '%' }"></div>
            </div>
            <div class="score-value">{{ currentCard.matchScore }}%</div>
          </div>

          <div class="dimension-grid">
            <div v-for="(val, key) in currentCard.detail" :key="key" class="dimension-item">
              <div class="dim-score">{{ val }}</div>
              <div class="dim-label">{{ dimensionLabels[key as string] || key }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <button class="action-btn skip" @click="handleSkip">
          <el-icon :size="28"><Close /></el-icon>
        </button>
        <button class="action-btn like" @click="handleLike">
          <el-icon :size="28"><Check /></el-icon>
        </button>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">💫</div>
      <p>今日推荐已看完，明天再来吧</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getRecommendations, type MatchResult } from '@/api/matchApi'
import { followUser } from '@/api/followApi'
import { ElMessage } from 'element-plus'
import { MATCH_DIMENSION_LABELS } from '@/constants/matchConst'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 500"><rect fill="%23f0f2f5" width="400" height="500" rx="24"/><text x="50%" y="45%" text-anchor="middle" fill="%23adb5bd" font-size="80">👤</text></svg>'
const dimensionLabels = MATCH_DIMENSION_LABELS

const cards = ref<MatchResult[]>([])
const currentIndex = ref(0)
const dragX = ref(0)
const isDragging = ref(false)

const currentCard = computed(() => cards.value[currentIndex.value] || {} as MatchResult)
const likeOpacity = computed(() => Math.max(0, dragX.value / 150))
const nopeOpacity = computed(() => Math.max(0, -dragX.value / 150))

const currentCardStyle = computed(() => ({
  transform: `translateX(${dragX.value}px) rotate(${dragX.value * 0.05}deg)`,
  transition: isDragging.value ? 'none' : 'transform 0.4s ease',
}))

onMounted(async () => {
  try {
    const res = await getRecommendations(0, 20)
    cards.value = res.data.data || []
  } catch { /* empty */ }
})

let startX = 0
function startDrag(e: MouseEvent | TouchEvent) {
  isDragging.value = true
  startX = 'touches' in e ? e.touches[0]!.clientX : e.clientX
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', endDrag)
  document.addEventListener('touchmove', onDrag, { passive: true })
  document.addEventListener('touchend', endDrag)
}

function onDrag(e: MouseEvent | TouchEvent) {
  const x = 'touches' in e ? e.touches[0]!.clientX : e.clientX
  dragX.value = x - startX
}

function endDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', endDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', endDrag)

  if (dragX.value > 100) handleLike()
  else if (dragX.value < -100) handleSkip()
  else dragX.value = 0
}

function nextCard() {
  dragX.value = 0
  if (currentIndex.value < cards.value.length - 1) {
    currentIndex.value++
  } else {
    cards.value = []
  }
}

async function handleLike() {
  const card = currentCard.value
  dragX.value = 400
  setTimeout(() => {
    nextCard()
    followUser(card.userId).then(() => {
      ElMessage.success(`已关注 ${card.nickname}`)
    }).catch(() => {})
  }, 300)
}

function handleSkip() {
  dragX.value = -400
  setTimeout(nextCard, 300)
}
</script>

<style lang="scss" scoped>
.match-page { padding: 0; }

.page-header {
  padding: 20px 24px;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;

  .page-title { font-size: 20px; font-weight: 700; }
  .page-subtitle { font-size: 13px; color: $text-muted; margin-top: 2px; display: block; }
}

.card-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
}

.swipe-card {
  width: 100%;
  max-width: 400px;
  border-radius: $radius-xl;
  overflow: hidden;
  user-select: none;
  cursor: grab;
  position: relative;

  &:active { cursor: grabbing; }
}

.swipe-overlay {
  position: absolute;
  top: 20px;
  z-index: 5;
  font-size: 32px;
  font-weight: 900;
  padding: 8px 20px;
  border-radius: $radius-md;
  border: 4px solid;

  &.like { left: 20px; color: $success; border-color: $success; transform: rotate(-15deg); }
  &.nope { right: 20px; color: $danger; border-color: $danger; transform: rotate(15deg); }
}

.swipe-avatar {
  width: 100%;
  height: 300px;
  object-fit: cover;
}

.swipe-info { padding: 20px; }

.swipe-name { font-size: 22px; font-weight: 700; margin-bottom: 8px; }

.swipe-tags { display: flex; gap: 6px; margin-bottom: 10px; }

.swipe-tag {
  padding: 3px 10px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;

  &.accent { background: rgba($primary, 0.1); color: $primary; }
}

.swipe-bio { font-size: 14px; color: $text-secondary; margin-bottom: 16px; }

.match-score-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;

  .score-label { font-size: 13px; font-weight: 600; color: $text-secondary; white-space: nowrap; }
  .score-track { flex: 1; height: 8px; background: $bg-tertiary; border-radius: $radius-full; overflow: hidden; }
  .score-fill { height: 100%; background: $primary-gradient; border-radius: $radius-full; transition: width 0.6s ease; }
  .score-value { font-size: 18px; font-weight: 800; color: $primary; }
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.dimension-item {
  text-align: center;
  padding: 8px 4px;
  background: $bg-tertiary;
  border-radius: $radius-md;

  .dim-score { font-size: 18px; font-weight: 700; color: $primary; }
  .dim-label { font-size: 11px; color: $text-muted; margin-top: 2px; }
}

.action-buttons {
  display: flex;
  gap: 24px;
  margin-top: 24px;
}

.action-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid;
  transition: all $transition-base;
  cursor: pointer;

  &.skip { border-color: $danger; color: $danger; background: white; &:hover { background: $danger; color: white; } }
  &.like { border-color: $success; color: $success; background: white; &:hover { background: $success; color: white; } }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  gap: 16px;
  .empty-icon { font-size: 64px; }
  p { color: $text-muted; font-size: 15px; }
}
</style>
