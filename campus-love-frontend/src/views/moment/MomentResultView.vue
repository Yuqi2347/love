<template>
  <div class="result-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="reveal-animation">
        <div class="reveal-ring ring-1" />
        <div class="reveal-ring ring-2" />
        <div class="reveal-ring ring-3" />
        <span class="reveal-star">✨</span>
      </div>
      <p class="loading-text">正在揭晓你的心动对象...</p>
    </div>

    <!-- 未匹配 -->
    <div v-else-if="!result?.matched" class="unmatched-state">
      <div class="moon-icon">🌙</div>
      <h2>本周暂未找到最佳匹配</h2>
      <p>你的心动档案已保留，下周继续帮你寻找！</p>
      <button class="btn-back-home" @click="$router.replace('/moment')">返回心动一刻</button>
    </div>

    <!-- 匹配成功 -->
    <div v-else class="matched-state">
      <!-- 顶部分数 -->
      <div class="score-header">
        <div class="score-circle">
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="54" class="score-bg" />
            <circle cx="60" cy="60" r="54" class="score-fill" :style="scoreCircleStyle" />
          </svg>
          <div class="score-value">
            <span class="score-num">{{ Math.round(result.totalScore || 0) }}</span>
            <span class="score-unit">分</span>
          </div>
        </div>
        <h2 class="score-title">匹配度</h2>
      </div>

      <!-- 对方信息卡片 -->
      <div class="user-card">
        <div class="card-header">
          <img :src="result.avatarUrl || defaultAvatar" class="card-avatar" />
          <div class="card-info">
            <div class="card-name">{{ result.nickname }}</div>
            <div class="card-meta">
              <span v-if="result.age">{{ result.age }}岁</span>
              <span v-if="result.school">{{ result.school }}</span>
              <span v-if="result.major">{{ result.major }}</span>
            </div>
          </div>
        </div>

        <div class="card-tags">
          <span v-if="result.mbti" class="tag mbti-tag">{{ result.mbti }}</span>
          <span v-if="result.zodiac" class="tag zodiac-tag">{{ result.zodiac }}</span>
          <span v-if="result.grade" class="tag grade-tag">{{ result.grade }}</span>
        </div>

        <div v-if="result.bio" class="card-bio">{{ result.bio }}</div>
      </div>

      <!-- AI 配对总结 -->
      <div v-if="result.summary" class="summary-section">
        <h3 class="summary-title">💫 配对解读</h3>
        <p class="summary-text">{{ result.summary }}</p>
      </div>

      <!-- 四维度分数 -->
      <div v-if="result.scoreDetail" class="score-detail">
        <h3 class="detail-title">匹配维度</h3>
        <div class="dimension-list">
          <div v-for="(value, key) in result.scoreDetail" :key="key" class="dimension-item">
            <div class="dim-header">
              <span class="dim-label">{{ MOMENT_SCORE_LABELS[key as string] || key }}</span>
              <span class="dim-value">{{ (Number(value)).toFixed(1) }}</span>
            </div>
            <div class="dim-bar">
              <div class="dim-fill" :style="{ width: `${Math.min(Number(value) * 4, 100)}%` }" />
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button class="btn-follow" @click="goProfile">
          查看TA的主页
        </button>
        <button class="btn-chat" @click="goChat">
          去打招呼
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMomentResult } from '@/api/momentApi'
import type { MomentResultResponse } from '@/api/momentApi'
import { MOMENT_SCORE_LABELS } from '@/constants/momentConst'

const router = useRouter()
const loading = ref(true)
const result = ref<MomentResultResponse | null>(null)

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><rect fill="%23f0f2f5" width="80" height="80" rx="40"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="36">👤</text></svg>'

const scoreCircleStyle = computed(() => {
  const score = result.value?.totalScore || 0
  const pct = Math.min(score, 100) / 100
  const circumference = 2 * Math.PI * 54
  const offset = circumference * (1 - pct)
  return {
    strokeDasharray: `${circumference}`,
    strokeDashoffset: `${offset}`,
  }
})

function goProfile() {
  if (result.value?.matchedUserId) {
    router.push(`/profile/${result.value.matchedUserId}`)
  }
}

function goChat() {
  if (result.value?.matchedUserId) {
    router.push(`/chat/${result.value.matchedUserId}`)
  }
}

onMounted(async () => {
  try {
    const res = await getMomentResult()
    result.value = res.data.data
  } catch {
    result.value = { matched: false, weekTag: '' }
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.result-page {
  max-width: $max-content-width;
  margin: 0 auto;
  padding: 40px 20px;
  min-height: 100vh;
}

// ==================== 加载动画 ====================
.loading-state {
  text-align: center;
  padding-top: 120px;
}

.reveal-animation {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
}

.reveal-ring {
  position: absolute;
  border: 2px solid;
  border-radius: 50%;
  animation: spin 3s linear infinite;

  &.ring-1 { inset: 0; border-color: $primary-light; }
  &.ring-2 { inset: 15px; border-color: $primary; animation-direction: reverse; animation-duration: 2s; }
  &.ring-3 { inset: 30px; border-color: $primary-dark; animation-duration: 4s; }
}

.reveal-star {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  animation: pulse 1.5s ease-in-out infinite;
}

.loading-text {
  font-size: 16px;
  color: $text-secondary;
}

// ==================== 未匹配 ====================
.unmatched-state {
  text-align: center;
  padding-top: 100px;

  .moon-icon { font-size: 64px; margin-bottom: 16px; }
  h2 { font-size: 22px; font-weight: 700; color: $text-primary; margin-bottom: 8px; }
  p { font-size: 14px; color: $text-secondary; margin-bottom: 32px; }
}

.btn-back-home {
  padding: 12px 32px;
  background: $bg-tertiary;
  color: $text-secondary;
  border: none;
  border-radius: $radius-full;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-base;

  &:hover { background: $border-color; }
}

// ==================== 匹配成功 ====================
.matched-state {
  animation: fadeInUp 0.6s ease;
}

// 分数圆环
.score-header {
  text-align: center;
  margin-bottom: 28px;
}

.score-circle {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 12px;

  svg { width: 100%; height: 100%; transform: rotate(-90deg); }

  .score-bg {
    fill: none;
    stroke: $bg-tertiary;
    stroke-width: 8;
  }

  .score-fill {
    fill: none;
    stroke: $primary;
    stroke-width: 8;
    stroke-linecap: round;
    transition: stroke-dashoffset 1.5s ease;
  }
}

.score-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: baseline;
  gap: 2px;

  .score-num {
    font-size: 36px;
    font-weight: 800;
    background: $primary-gradient;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .score-unit {
    font-size: 14px;
    color: $text-muted;
  }
}

.score-title {
  font-size: 16px;
  color: $text-secondary;
  font-weight: 500;
}

// 用户卡片
.user-card {
  background: $bg-primary;
  border-radius: $radius-xl;
  padding: 24px;
  box-shadow: $shadow-md;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.card-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid $primary-light;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 4px;
}

.card-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
  color: $text-muted;

  span:not(:last-child)::after {
    content: '·';
    margin-left: 8px;
    color: $border-color;
  }
}

.card-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag {
  padding: 4px 12px;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
}

.mbti-tag { background: rgba(#6C5CE7, 0.1); color: #6C5CE7; }
.zodiac-tag { background: rgba($accent, 0.1); color: #B8860B; }
.grade-tag { background: rgba($info, 0.1); color: $info; }

.card-bio {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.6;
  padding-top: 12px;
  border-top: 1px solid $border-light;
}

// AI 配对总结
.summary-section {
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
  border: 1px solid rgba($primary, 0.15);
  border-radius: $radius-xl;
  padding: 20px;
  margin-bottom: 20px;
}

.summary-title {
  font-size: 15px;
  font-weight: 700;
  color: $primary;
  margin-bottom: 12px;
}

.summary-text {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.8;
  white-space: pre-wrap;
}

// 四维度分数
.score-detail {
  background: $bg-primary;
  border-radius: $radius-xl;
  padding: 24px;
  box-shadow: $shadow-sm;
  margin-bottom: 24px;
}

.detail-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 16px;
}

.dimension-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dimension-item {
  .dim-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
  }
  .dim-label { font-size: 14px; color: $text-secondary; }
  .dim-value { font-size: 14px; font-weight: 700; color: $primary; }
}

.dim-bar {
  height: 8px;
  background: $bg-tertiary;
  border-radius: 4px;
  overflow: hidden;
}

.dim-fill {
  height: 100%;
  background: $primary-gradient;
  border-radius: 4px;
  transition: width 1s ease;
}

// 操作按钮
.action-buttons {
  display: flex;
  gap: 12px;
}

.btn-follow {
  flex: 1;
  height: 48px;
  background: $bg-tertiary;
  color: $text-primary;
  border: none;
  border-radius: $radius-full;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-base;

  &:hover { background: $border-color; }
}

.btn-chat {
  flex: 1;
  height: 48px;
  background: $primary-gradient;
  color: white;
  border: none;
  border-radius: $radius-full;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all $transition-base;

  &:hover { box-shadow: 0 4px 16px rgba($primary, 0.3); }
}

// ==================== 动画 ====================
@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.2); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
