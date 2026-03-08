<template>
  <div class="match-page">
    <div class="page-header">
      <h2 class="page-title">匹配推荐</h2>
      <span class="page-subtitle">基于多维度画像精准匹配</span>
      <div class="filter-tabs">
        <button :class="['tab-btn', { active: genderFilter === 'all' }]" @click="setGenderFilter('all')">全部</button>
        <button :class="['tab-btn', { active: genderFilter === 'opposite' }]" @click="setGenderFilter('opposite')">异性</button>
        <button :class="['tab-btn', { active: genderFilter === 'same' }]" @click="setGenderFilter('same')">同性</button>
      </div>
    </div>

    <div v-if="sortedCards.length" class="card-container">
      <!-- 竖直卡片列表 -->
      <div
        class="cards-wrapper"
        @wheel="handleWheel">
        <div
          v-for="(card, index) in visibleCards"
          :key="card.userId"
          class="match-card"
          :class="{ 'is-current': index === centerCardOffset, 'is-removing': card.isRemoving }"
          :style="getCardStyle(card, index)"
          @mousedown="handleCardDown"
          @touchstart.passive="handleCardDown"
          @dblclick="handleViewProfile(card.userId)">
          <div class="swipe-overlay like" :style="{ opacity: index === centerCardOffset ? likeOpacity : 0 }">LIKE</div>
          <div class="swipe-overlay nope" :style="{ opacity: index === centerCardOffset ? nopeOpacity : 0 }">NOPE</div>

          <div class="card-rank">#{{ card.globalIndex }}</div>
          <img :src="card.avatarUrl || defaultAvatar" class="card-avatar" />
          <div class="card-info">
            <div class="card-header">
              <div class="card-name">{{ card.nickname }}</div>
              <div v-if="card.gender" class="card-gender" :class="card.gender === 1 ? 'male' : 'female'">
                {{ card.gender === 1 ? '♂' : '♀' }}
              </div>
            </div>
            <div class="card-tags">
              <span v-if="card.zodiac" class="card-tag">{{ card.zodiac }}</span>
              <span v-if="card.mbti" class="card-tag accent">{{ card.mbti }}</span>
              <span v-if="card.major" class="card-tag">{{ card.major }}</span>
            </div>
            <p class="card-bio">{{ card.bio || '这个人很神秘~' }}</p>

            <div class="match-score-bar">
              <div class="score-label">匹配度</div>
              <div class="score-track">
                <div class="score-fill" :style="{ width: card.matchScore + '%' }"></div>
              </div>
              <div class="score-value">{{ card.matchScore }}%</div>
            </div>

            <div class="dimension-grid">
              <div v-for="(val, key) in card.detail" :key="key" class="dimension-item">
                <div class="dim-score">{{ val }}</div>
                <div class="dim-label">{{ dimensionLabels[key as string] || key }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button class="action-btn skip" @click="handleSkip">
          <el-icon :size="36"><Close /></el-icon>
        </button>
        <button class="action-btn like" @click="handleLike">
          <el-icon :size="36"><Check /></el-icon>
        </button>
      </div>

      <div class="card-counter">{{ currentIndex + 1 }} / {{ remainingCount }}</div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">💫</div>
      <p>今日推荐已看完</p>
      <button class="reset-filter-btn" @click="setGenderFilter('all')">重新筛选</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getRecommendations, reportUserAction, type MatchResult } from '@/api/matchApi'
import { followUser } from '@/api/followApi'
import { ElMessage } from 'element-plus'
import { MATCH_DIMENSION_LABELS } from '@/constants/matchConst'
import { useRouter } from 'vue-router'

const router = useRouter()

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 500"><rect fill="%23f0f2f5" width="400" height="500" rx="24"/><text x="50%" y="45%" text-anchor="middle" fill="%23adb5bd" font-size="80">👤</text></svg>'
const dimensionLabels = MATCH_DIMENSION_LABELS

interface CardWithState extends MatchResult {
  globalIndex: number
  isRemoving: boolean
}

const cards = ref<CardWithState[]>([])
const genderFilter = ref<'all' | 'same' | 'opposite'>('all')
const currentUserGender = ref<number | null>(null)
const currentIndex = ref(0)

// 拖拽状态
const dragX = ref(0)
const isDragging = ref(false)
const isActionTriggered = ref(false) // 标记动作是否已触发

// 按匹配度排序
const sortedCards = computed(() => {
  return [...cards.value]
    .sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
    .map((card, index) => ({ ...card, globalIndex: index + 1 }))
})

// 根据性别筛选
const filteredCards = computed(() => {
  if (genderFilter.value === 'all') return sortedCards.value
  if (!currentUserGender.value) return sortedCards.value

  return sortedCards.value.filter(card => {
    if (!card.gender || card.isRemoving) return false
    const isSame = card.gender === currentUserGender.value
    return genderFilter.value === 'same' ? isSame : !isSame
  })
})

// 可见的卡片（竖直列表，当前卡片在中间）
const visibleCards = computed(() => {
  const result: CardWithState[] = []
  const len = filteredCards.value.length

  if (len === 0) return result

  // 显示当前卡片及其上下各1张
  for (let i = -1; i <= 1; i++) {
    const index = currentIndex.value + i
    if (index >= 0 && index < len) {
      result.push(filteredCards.value[index])
    }
  }

  return result
})

// 当前卡片在 visibleCards 中的索引位置
const centerCardOffset = computed(() => {
  // visibleCards 总是从 currentIndex-1 开始（如果存在）
  // 所以当前卡片总是在 visibleCards[1]，除非是第一张卡片
  return currentIndex.value === 0 ? 0 : 1
})

// 剩余卡片数
const remainingCount = computed(() => filteredCards.value.filter(c => !c.isRemoving).length)

const currentCard = computed(() => {
  const card = filteredCards.value[currentIndex.value]
  return card && !card.isRemoving ? card : null
})

const likeOpacity = computed(() => Math.max(0, Math.min(dragX.value / 100, 1)))
const nopeOpacity = computed(() => Math.max(0, Math.min(-dragX.value / 100, 1)))

async function loadCards() {
  try {
    const res = await getRecommendations(0, 100, genderFilter.value)
    cards.value = (res.data.data || []).map(card => ({
      ...card,
      globalIndex: 0,
      isRemoving: false
    }))
    currentIndex.value = 0
  } catch { /* empty */ }
}

onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    currentUserGender.value = user.gender || null
  }
  await loadCards()
})

async function setGenderFilter(filter: 'all' | 'same' | 'opposite') {
  genderFilter.value = filter
  await loadCards()
}

function getCardStyle(card: CardWithState, index: number) {
  const isCurrent = index === centerCardOffset.value && !card.isRemoving

  // 竖直堆叠：当前卡片正常大小，上下卡片变小变暗
  const offsetFromCenter = index - centerCardOffset.value
  const translateY = offsetFromCenter * 100
  const scale = isCurrent ? 1 : 0.9 - Math.abs(offsetFromCenter) * 0.05
  const opacity = card.isRemoving ? 0 : (isCurrent ? 1 : 0.5 - Math.abs(offsetFromCenter) * 0.15)
  const zIndex = 10 - Math.abs(offsetFromCenter)

  let transform = `translateY(${translateY}px) scale(${scale})`

  // 当前卡片拖拽时添加旋转和水平位移
  if (isCurrent && isDragging.value) {
    transform = `translate(${dragX.value}px, ${translateY}px) rotate(${dragX.value * 0.05}deg) scale(${scale})`
  } else if (card.isRemoving) {
    // 移除动画
    transform = `translate(${dragX.value}px, ${translateY}px) scale(0.8) rotate(${dragX.value * 0.1}deg)`
  }

  return {
    transform,
    opacity,
    zIndex,
    transition: (isCurrent && isDragging.value) ? 'none' : 'transform 0.3s ease, opacity 0.3s ease',
    pointerEvents: card.isRemoving ? 'none' : 'auto',
    cursor: isCurrent && !card.isRemoving ? (isDragging.value ? 'grabbing' : 'grab') : 'default'
  }
}

// 鼠标滚轮切换卡片（支持循环）
function handleWheel(e: WheelEvent) {
  e.preventDefault()

  const len = filteredCards.value.length
  if (len === 0) return

  // 向下滚动(deltaY > 0) = 下一个用户
  // 向上滚动(deltaY < 0) = 上一个用户
  if (e.deltaY > 30) {
    if (currentIndex.value < len - 1) {
      currentIndex.value++
    } else {
      // 最后一个，循环到第一个
      currentIndex.value = 0
    }
  } else if (e.deltaY < -30) {
    if (currentIndex.value > 0) {
      currentIndex.value--
    } else {
      // 第一个，循环到最后一个
      currentIndex.value = len - 1
    }
  }
}

// 卡片拖拽处理（只处理水平滑动）
function handleCardDown(e: MouseEvent | TouchEvent) {
  // 只在当前卡片上开始拖拽
  const target = e.target as HTMLElement
  if (!target.closest('.match-card.is-current')) {
    return
  }

  if (e instanceof MouseEvent && e.button !== 0) return

  isDragging.value = false
  isActionTriggered.value = false
  dragX.value = 0

  const startX = 'touches' in e ? e.touches[0]!.clientX : e.clientX
  const SWIPE_THRESHOLD = 180 // 增大阈值到180px

  function onMove(e: MouseEvent | TouchEvent) {
    const currentX = 'touches' in e ? e.touches[0]!.clientX : e.clientX
    const deltaX = currentX - startX

    // 只有水平移动才触发拖拽
    if (Math.abs(deltaX) > 10) {
      isDragging.value = true
      dragX.value = deltaX

      // 达到阈值时立即触发，不需要等松开鼠标
      if (!isActionTriggered.value) {
        if (deltaX > SWIPE_THRESHOLD) {
          // 右滑超过阈值 = 关注
          isActionTriggered.value = true
          handleLike()
        } else if (deltaX < -SWIPE_THRESHOLD) {
          // 左滑超过阈值 = 不关注
          isActionTriggered.value = true
          handleSkip()
        }
      }
    }
  }

  function onUp() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.removeEventListener('touchmove', onMove)
    document.removeEventListener('touchend', onUp)

    // 如果没有触发动作，复位拖拽
    if (!isActionTriggered.value) {
      dragX.value = 0
    }

    // 总是重置拖拽状态
    isDragging.value = false
    isActionTriggered.value = false
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.addEventListener('touchmove', onMove, { passive: true })
  document.addEventListener('touchend', onUp)
}

async function handleLike() {
  const card = currentCard.value
  if (!card) return

  // 获取当前卡片ID，避免异步操作后引用丢失
  const targetUserId = card.userId
  const targetNickname = card.nickname

  reportUserAction(targetUserId, 'FOLLOW').catch(() => {})

  // 立即标记为移除，触发动画
  removeCardById(targetUserId, 'right')

  // 后台处理关注逻辑
  followUser(targetUserId).then(() => {
    ElMessage.success(`已关注 ${targetNickname}`)
  }).catch(() => {})
}

function handleSkip() {
  const card = currentCard.value
  if (!card) return

  const targetUserId = card.userId

  reportUserAction(targetUserId, 'IGNORE').catch(() => {})
  removeCardById(targetUserId, 'left')
}

function removeCardById(userId: number, direction: 'left' | 'right') {
  // 找到要移除的卡片
  const cardToRemove = cards.value.find(c => c.userId === userId)
  if (!cardToRemove) return

  // 标记为正在移除，触发飞出动画
  cardToRemove.isRemoving = true
  dragX.value = direction === 'left' ? -500 : 500

  // 立即重置拖拽状态，防止下一张卡片继承状态
  isDragging.value = false
  isActionTriggered.value = true // 标记已触发，防止 onUp 中再次处理

  // 动画完成后移除数据
  setTimeout(() => {
    // 从原数组中移除该卡片
    cards.value = cards.value.filter(c => c.userId !== userId)
    dragX.value = 0

    // 更新排名
    cards.value.forEach((c, i) => {
      c.globalIndex = i + 1
    })

    // 调整 currentIndex 确保显示有效卡片
    const newLength = filteredCards.value.length
    if (currentIndex.value >= newLength && newLength > 0) {
      currentIndex.value = newLength - 1
    }

    // 重置触发标记
    isActionTriggered.value = false
  }, 350)
}

function handleViewProfile(userId: number) {
  router.push(`/profile/${userId}`)
}
</script>

<style lang="scss" scoped>
@use 'sass:color';
.match-page {
  padding: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.page-header {
  padding: 16px 20px;
  border-bottom: 1px solid $border-light;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 20;

  .page-title { font-size: 18px; font-weight: 700; }
  .page-subtitle { font-size: 12px; color: $text-muted; margin-top: 2px; display: block; }

  .filter-tabs {
    display: flex;
    gap: 6px;
    margin-top: 12px;
  }

  .tab-btn {
    padding: 5px 14px;
    border: 1px solid $border-light;
    border-radius: $radius-full;
    background: $bg-secondary;
    color: $text-secondary;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover { border-color: $primary; color: $primary; }
    &.active { background: $primary; border-color: $primary; color: white; }
  }
}

.card-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  overflow: hidden;
}

.cards-wrapper {
  position: relative;
  width: 100%;
  max-width: 380px;
  height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.match-card {
  position: absolute;
  top: 50%;
  left: 50%;
  transform-origin: center center;
  width: 340px;
  height: 480px;
  margin-left: -170px;
  margin-top: -240px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  user-select: none;
  border: 1px solid rgba(0,0,0,0.05);
  transition: transform 0.3s ease, opacity 0.3s ease, box-shadow 0.3s ease;

  &.is-current {
    z-index: 10;
    box-shadow: 0 12px 40px rgba(0,0,0,0.2);
    border: 2px solid rgba(255,105,180,0.3);
  }
}

.swipe-overlay {
  position: absolute;
  top: 24px;
  z-index: 15;
  font-size: 32px;
  font-weight: 900;
  padding: 8px 24px;
  border-radius: $radius-md;
  border: 4px solid;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
  pointer-events: none;

  &.like { left: 24px; color: #4CAF50; border-color: #4CAF50; transform: rotate(-15deg); }
  &.nope { right: 24px; color: #f44336; border-color: #f44336; transform: rotate(15deg); }
}

.card-rank {
  position: absolute;
  top: 14px;
  left: 14px;
  width: 32px;
  height: 32px;
  background: rgba(0,0,0,0.6);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  z-index: 5;
}

.card-avatar {
  width: 100%;
  height: 260px;
  object-fit: cover;
}

.card-info {
  padding: 14px;
  height: calc(100% - 260px);
  overflow-y: auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.card-name { font-size: 20px; font-weight: 700; }

.card-gender {
  font-size: 18px;
  padding: 2px 8px;
  border-radius: $radius-full;
  font-weight: 600;

  &.male { background: rgba(#2196F3, 0.1); color: #2196F3; }
  &.female { background: rgba(#E91E63, 0.1); color: #E91E63; }
}

.card-tags { display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap; }

.card-tag {
  padding: 3px 10px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  font-size: 12px;
  color: $text-secondary;

  &.accent { background: rgba($primary, 0.1); color: $primary; }
}

.card-bio { font-size: 13px; color: $text-secondary; margin-bottom: 12px; }

.match-score-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .score-label { font-size: 12px; font-weight: 600; color: $text-secondary; }
  .score-track { flex: 1; height: 6px; background: $bg-tertiary; border-radius: $radius-full; overflow: hidden; }
  .score-fill { height: 100%; background: $primary-gradient; border-radius: $radius-full; transition: width 0.4s ease; }
  .score-value { font-size: 16px; font-weight: 800; color: $primary; }
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.dimension-item {
  text-align: center;
  padding: 6px 4px;
  background: $bg-tertiary;
  border-radius: $radius-md;

  .dim-score { font-size: 16px; font-weight: 700; color: $primary; }
  .dim-label { font-size: 10px; color: $text-muted; margin-top: 2px; }
}

.action-buttons {
  display: flex;
  gap: 40px;
  margin-top: 20px;
  position: relative;
  z-index: 20;
}

.action-btn {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid;
  transition: all 0.2s;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);

  &.skip { border-color: #f44336; color: #f44336; background: white; &:hover { background: #f44336; color: white; transform: scale(1.05); } }
  &.like { border-color: #4CAF50; color: #4CAF50; background: white; &:hover { background: #4CAF50; color: white; transform: scale(1.05); } }
}

.card-counter {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: $text-muted;
  font-weight: 600;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  .empty-icon { font-size: 64px; }
  p { color: $text-muted; font-size: 15px; }
}

.reset-filter-btn {
  margin-top: 8px;
  padding: 10px 20px;
  background: $primary;
  color: white;
  border: none;
  border-radius: $radius-md;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
  &:hover { background: color.adjust($primary, $lightness: -10%); }
}
</style>
