<template>
  <div class="result-page">
    <div v-if="loading" class="loading-state">
      <div class="reveal-animation">
        <div class="reveal-ring ring-1" />
        <div class="reveal-ring ring-2" />
        <div class="reveal-ring ring-3" />
        <span class="reveal-star">✨</span>
      </div>
      <p class="loading-text">正在揭晓你的心动对象...</p>
    </div>

    <div v-else-if="!result?.matched" class="unmatched-state">
      <div class="moon-icon">🌙</div>
      <h2>本周暂未找到最佳匹配</h2>
      <p>你的心动档案已保留，下周继续帮你寻找！</p>
      <button class="btn-back-home" @click="$router.replace('/moment')">返回心动一刻</button>
    </div>

    <div v-else class="matched-state">
      <div class="screen-progress">
        <button
          v-for="(label, index) in screenLabels"
          :key="label"
          :class="['progress-step', { active: activeScreen === index + 1, locked: index === 3 && !result.datePrepUnlocked }]"
          :disabled="index === 3 && !result.datePrepUnlocked"
          @click="goScreen(index + 1)"
        >
          <span class="progress-index">{{ index + 1 }}</span>
          <span class="progress-label">{{ label }}</span>
        </button>
      </div>

      <div class="screen-viewport">
        <div class="screen-track" :style="{ transform: `translateX(-${(activeScreen - 1) * 25}%)` }">
          <section class="screen-panel reveal-panel">
            <div class="confetti-wrap">
              <span v-for="i in 12" :key="i" class="confetti" :style="confettiStyle(i)" />
            </div>
            <div class="matched-icon">💘</div>
            <p class="section-eyebrow">心动揭晓</p>
            <h2 class="reveal-title">{{ result.yuanfenTitle || '刚好对频' }}</h2>
            <p class="reveal-desc">这一次，不用数字证明什么，先去看看你们为什么会彼此靠近。</p>

            <div class="user-card">
              <div class="card-header">
                <img :src="result.avatarUrl || defaultAvatar" class="card-avatar" />
                <div class="card-info">
                  <div class="card-name">{{ result.nickname }}</div>
                  <div class="card-meta">
                    <span v-if="result.age">{{ result.age }}岁</span>
                    <span v-if="result.school">{{ result.school }}</span>
                    <span v-if="result.major">{{ result.major }}</span>
                    <span v-if="result.grade">{{ result.grade }}</span>
                  </div>
                </div>
              </div>

              <div class="card-tags">
                <span v-if="result.mbti" class="tag mbti-tag">{{ result.mbti }}</span>
                <span v-if="result.zodiac" class="tag zodiac-tag">{{ result.zodiac }}</span>
              </div>

              <div v-if="result.bio" class="card-bio">{{ result.bio }}</div>
            </div>

            <div v-if="result.complementaryModes?.length" class="mode-list">
              <span v-for="mode in result.complementaryModes" :key="mode" class="mode-tag">{{ mode }}</span>
            </div>

            <div class="screen-actions dual">
              <button class="btn-secondary" @click="goProfile">查看TA的主页</button>
              <button class="btn-primary" @click="goScreen(2)">查看你们的缘分</button>
            </div>
          </section>

          <section class="screen-panel insight-panel">
            <p class="section-eyebrow">心动之处</p>
            <h2 class="panel-title">你们为什么会靠近</h2>

            <div class="insight-list">
              <article v-for="(text, index) in insightCards" :key="index" class="insight-card">
                <div class="insight-index">0{{ index + 1 }}</div>
                <h3>{{ insightTitles[index] }}</h3>
                <p>{{ text }}</p>
              </article>
            </div>

            <div v-if="result.dimensionLabels?.length" class="dimension-tags">
              <span v-for="item in result.dimensionLabels" :key="item" class="dimension-tag">{{ item }}</span>
            </div>

            <div v-if="result.goldenSentence" class="golden-card">
              <p class="golden-label">专属金句</p>
              <p class="golden-text">{{ result.goldenSentence }}</p>
            </div>

            <div class="screen-actions">
              <button class="btn-secondary" @click="goScreen(1)">回到揭晓</button>
              <button class="btn-primary" @click="goScreen(3)">继续了解TA</button>
            </div>
          </section>

          <section class="screen-panel about-panel">
            <p class="section-eyebrow">TA是这样的人</p>
            <h2 class="panel-title">如果有人替你先介绍TA</h2>

            <div class="about-card">
              <p>{{ result.aboutMatchedUser || 'TA身上有一种很适合慢慢了解的气质。' }}</p>
            </div>

            <div class="choice-card">
              <p class="choice-hint">{{ decisionHint }}</p>
              <p v-if="decisionDetail" class="choice-detail">{{ decisionDetail }}</p>

              <div class="choice-buttons">
                <button class="btn-primary choice-main" :disabled="choiceLocked || confirming" @click="handleChoice('YUE')">
                  {{ confirming && pendingChoice === 'YUE' ? '提交中...' : '心动，约起来 🔥' }}
                </button>
                <button class="btn-secondary choice-second" :disabled="choiceLocked || confirming" @click="handleChoice('GUANZHU')">
                  {{ confirming && pendingChoice === 'GUANZHU' ? '提交中...' : '先关注，慢慢来 🌱' }}
                </button>
              </div>

              <button v-if="result.datePrepUnlocked" class="btn-ghost prep-entry-btn" @click="openDatePrep">
                查看约会准备
              </button>

              <div class="quick-actions">
                <button class="text-action" @click="goProfile">查看TA的主页</button>
                <button class="text-action" @click="goChat">去打招呼</button>
              </div>
            </div>

            <div class="screen-actions">
              <button class="btn-secondary" @click="goScreen(2)">回看缘分</button>
              <button class="btn-primary" :disabled="!result.datePrepUnlocked" @click="openDatePrep">
                {{ result.datePrepUnlocked ? '进入第四屏' : '等待双方确认' }}
              </button>
            </div>
          </section>

          <section class="screen-panel prep-panel">
            <p class="section-eyebrow">约会准备</p>
            <h2 class="panel-title">把心动往现实里推一步</h2>

            <div v-if="!result.datePrepUnlocked" class="locked-card">
              <p class="locked-title">第四屏尚未解锁</p>
              <p>双方都选择“心动，约起来”后，这里会出现你们的约会准备内容。</p>
            </div>

            <div v-else-if="prepLoading" class="prep-loading">
              <div class="mini-spinner" />
              <p>正在为你准备第一次见面的细节...</p>
            </div>

            <div v-else-if="datePrep" class="prep-content">
              <article class="prep-card">
                <p class="prep-label">约会方式推荐 · {{ datePrep.dateSceneType }}</p>
                <p>{{ datePrep.dateSuggestion }}</p>
              </article>

              <article class="prep-card">
                <p class="prep-label">破冰话题</p>
                <div class="topic-list">
                  <div v-for="item in datePrep.iceBreakTopics" :key="item.title" class="topic-item">
                    <h3>{{ item.title }}</h3>
                    <p>{{ item.opener }}</p>
                  </div>
                </div>
              </article>

              <article class="prep-card">
                <p class="prep-label">约会小惊喜</p>
                <p>{{ datePrep.surpriseIdea }}</p>
              </article>

              <article class="prep-card">
                <p class="prep-label">打扮建议</p>
                <p>{{ datePrep.outfitAdvice }}</p>
              </article>

              <article class="prep-card">
                <p class="prep-label">心理准备</p>
                <p>{{ datePrep.mindsetAdvice }}</p>
              </article>
            </div>

            <div class="screen-actions">
              <button class="btn-secondary" @click="goScreen(3)">回到第三屏</button>
              <button class="btn-primary" @click="goChat">去打招呼</button>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { confirmMomentChoice, getMomentDatePrep, getMomentResult, type MomentDatePrepResponse, type MomentResultResponse } from '@/api/momentApi'

const router = useRouter()
const loading = ref(true)
const confirming = ref(false)
const prepLoading = ref(false)
const result = ref<MomentResultResponse | null>(null)
const datePrep = ref<MomentDatePrepResponse | null>(null)
const activeScreen = ref(1)
const pendingChoice = ref<'YUE' | 'GUANZHU' | null>(null)

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><rect fill="%23f0f2f5" width="80" height="80" rx="40"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="36">👤</text></svg>'
const screenLabels = ['心动揭晓', '心动之处', 'TA是这样的人', '约会准备']
const insightTitles = ['为什么你们可能互相吸引', '你们在一起可能是什么感觉', '你们可能需要磨合的地方']

const insightCards = computed(() => {
  const cards = result.value?.insightCards || []
  return [cards[0], cards[1], cards[2]].filter((item): item is string => !!item)
})

const choiceLocked = computed(() => {
  if (!result.value) return false
  return !!result.value.myChoice || result.value.confirmStatus === 'BOTH_YUE' || result.value.confirmStatus === 'ANY_GUANZHU' || result.value.confirmStatus === 'TIMEOUT_GUANZHU'
})

const decisionHint = computed(() => {
  if (!result.value) return '选择你的节奏，缘分自有安排 ✨'
  if (result.value.confirmStatus === 'BOTH_YUE') return '缘分给了答案。'
  if (result.value.confirmStatus === 'ANY_GUANZHU' || result.value.confirmStatus === 'TIMEOUT_GUANZHU') return '缘分让你们先成为朋友'
  return '选择你的节奏，缘分自有安排 ✨'
})

const decisionDetail = computed(() => {
  if (!result.value) return ''
  if (result.value.confirmStatus === 'BOTH_YUE') return '双方都选择了“约一次”，第四屏已经解锁。'
  if (result.value.confirmStatus === 'TIMEOUT_GUANZHU') return '48小时内没有等到双方都确认，系统已经为你们自动互相关注。'
  if (result.value.confirmStatus === 'ANY_GUANZHU') return '这次会先从关注开始，系统已经帮你们完成互关。'
  if (result.value.myChoice === 'YUE') return '你已经发出了心动信号，等TA给出选择后，就会看到下一步安排。'
  if (result.value.myChoice === 'GUANZHU') return '你已经选择先关注，接下来可以慢慢来。'
  return ''
})

function confettiStyle(i: number) {
  const hue = (i * 30) % 360
  const left = 10 + (i * 7) % 80
  const delay = (i * 0.3) % 2
  const duration = 2 + (i * 0.2) % 1.5
  return {
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    background: `hsl(${hue}, 80%, 65%)`,
  }
}

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

function goScreen(screen: number) {
  if (screen === 4) {
    if (!result.value?.datePrepUnlocked) return
    openDatePrep()
    return
  }
  activeScreen.value = screen
}

async function loadResult() {
  loading.value = true
  try {
    const res = await getMomentResult()
    result.value = res.data.data
  } catch {
    result.value = { matched: false, weekTag: '' }
  } finally {
    loading.value = false
  }
}

async function handleChoice(choice: 'YUE' | 'GUANZHU') {
  if (choiceLocked.value) return
  confirming.value = true
  pendingChoice.value = choice
  try {
    const res = await confirmMomentChoice(choice)
    result.value = res.data.data
    if (choice === 'GUANZHU') {
      ElMessage.success('已为你们自动互相关注')
    } else if (result.value?.datePrepUnlocked) {
      ElMessage.success('双方确认成功，第四屏已解锁')
      await openDatePrep()
      return
    } else {
      ElMessage.success('已发送你的选择')
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    confirming.value = false
    pendingChoice.value = null
  }
}

async function openDatePrep() {
  if (!result.value?.datePrepUnlocked) return
  activeScreen.value = 4
  if (datePrep.value || prepLoading.value) return
  prepLoading.value = true
  try {
    const res = await getMomentDatePrep()
    datePrep.value = res.data.data
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '约会准备加载失败')
  } finally {
    prepLoading.value = false
  }
}

onMounted(loadResult)
</script>

<style lang="scss" scoped>
.result-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 28px 20px 48px;
  min-height: 100vh;
}

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
  animation: pulseCenter 1.5s ease-in-out infinite;
}

.loading-text {
  font-size: 16px;
  color: $text-secondary;
}

.unmatched-state {
  text-align: center;
  padding-top: 100px;

  .moon-icon { font-size: 64px; margin-bottom: 16px; }
  h2 { font-size: 22px; font-weight: 700; color: $text-primary; margin-bottom: 8px; }
  p { font-size: 14px; color: $text-secondary; margin-bottom: 32px; }
}

.btn-back-home,
.btn-primary,
.btn-secondary,
.btn-ghost {
  border: none;
  border-radius: $radius-full;
  cursor: pointer;
  transition: all $transition-base;
}

.btn-back-home,
.btn-secondary {
  background: $bg-tertiary;
  color: $text-primary;

  &:hover { background: $border-color; }
}

.btn-primary {
  background: $primary-gradient;
  color: white;
  box-shadow: 0 8px 24px rgba($primary, 0.2);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 10px 24px rgba($primary, 0.26);
  }
}

.btn-ghost {
  background: transparent;
  color: $primary;
  border: 1px solid rgba($primary, 0.2);

  &:hover { background: rgba($primary, 0.04); }
}

.matched-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.screen-progress {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.progress-step {
  padding: 10px 8px;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  background: $bg-primary;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  transition: all 0.2s;

  &.active {
    border-color: rgba($primary, 0.45);
    background: rgba($primary, 0.06);
  }

  &.locked {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.progress-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-tertiary;
  color: $text-primary;
  font-size: 12px;
  font-weight: 700;
}

.progress-step.active .progress-index {
  background: $primary;
  color: white;
}

.progress-label {
  font-size: 12px;
  color: $text-secondary;
  text-align: center;
  line-height: 1.3;
}

.screen-viewport {
  overflow: hidden;
  width: 100%;
}

.screen-track {
  display: flex;
  width: 400%;
  transition: transform 0.35s ease;
  will-change: transform;
}

.screen-panel {
  flex: 0 0 25%;
  width: 25%;
  min-width: 0;
  padding: 24px 20px;
  background: $bg-primary;
  border-radius: $radius-xl;
  box-shadow: $shadow-md;
  position: relative;
  overflow-y: auto;
}

.section-eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: $primary;
  font-weight: 700;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 18px;
}

.reveal-panel {
  text-align: center;
  overflow: hidden;
}

.confetti-wrap {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.confetti {
  position: absolute;
  top: -10px;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  opacity: 0;
  animation: confettiFall 3s ease-in infinite;
}

.matched-icon {
  font-size: 64px;
  margin: 4px 0 10px;
  animation: bounce 1s ease-in-out infinite;
}

.reveal-title {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.25;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 10px;
}

.reveal-desc {
  color: $text-secondary;
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 18px;
}

.user-card,
.about-card,
.choice-card,
.golden-card,
.prep-card,
.locked-card {
  background: rgba($bg-tertiary, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: $radius-xl;
}

.user-card {
  text-align: left;
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.card-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba($primary, 0.15);
  flex-shrink: 0;
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
}

.card-tags,
.mode-list,
.dimension-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.card-tags {
  margin-bottom: 12px;
}

.tag,
.mode-tag,
.dimension-tag {
  padding: 6px 12px;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
}

.mbti-tag { background: rgba(#6C5CE7, 0.1); color: #6C5CE7; }
.zodiac-tag { background: rgba($accent, 0.1); color: #B8860B; }
.mode-tag { background: rgba($primary, 0.08); color: $primary; }
.dimension-tag { background: rgba($info, 0.08); color: $info; }

.card-bio {
  font-size: 14px;
  line-height: 1.7;
  color: $text-secondary;
  padding-top: 12px;
  border-top: 1px solid $border-light;
}

.mode-list {
  justify-content: center;
  margin-top: 14px;
}

.screen-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;

  &.dual > * {
    flex: 1;
  }

  button {
    min-height: 46px;
    padding: 0 18px;
    font-size: 15px;
    font-weight: 700;
  }
}

.insight-list,
.prep-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.insight-card {
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
  border: 1px solid rgba($primary, 0.12);
  border-radius: $radius-xl;
  padding: 18px;

  h3 {
    font-size: 16px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 10px;
  }

  p {
    font-size: 14px;
    line-height: 1.8;
    color: $text-secondary;
  }
}

.insight-index {
  font-size: 12px;
  color: $primary;
  font-weight: 700;
  margin-bottom: 8px;
}

.dimension-tags {
  margin-top: 16px;
}

.golden-card {
  margin-top: 16px;
  padding: 18px;
  text-align: center;
}

.golden-label {
  font-size: 12px;
  color: $text-muted;
  margin-bottom: 8px;
}

.golden-text {
  font-size: 18px;
  line-height: 1.7;
  font-weight: 700;
  color: $text-primary;
}

.about-card {
  padding: 22px 20px;

  p {
    font-size: 15px;
    line-height: 1.9;
    color: $text-secondary;
  }
}

.choice-card {
  margin-top: 16px;
  padding: 18px;
  text-align: center;
}

.choice-hint {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}

.choice-detail {
  font-size: 13px;
  line-height: 1.7;
  color: $text-secondary;
  margin-top: 8px;
}

.choice-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.choice-main,
.choice-second,
.prep-entry-btn {
  width: 100%;
  min-height: 48px;
  padding: 0 18px;
  font-size: 15px;
  font-weight: 700;
}

.prep-entry-btn {
  margin-top: 12px;
}

.quick-actions {
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 14px;
}

.text-action {
  border: none;
  background: transparent;
  color: $primary;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.locked-card,
.prep-loading {
  padding: 28px 20px;
  text-align: center;
}

.locked-title {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.locked-card p:last-child,
.prep-loading p {
  font-size: 14px;
  line-height: 1.7;
  color: $text-secondary;
}

.mini-spinner {
  width: 34px;
  height: 34px;
  margin: 0 auto 12px;
  border-radius: 50%;
  border: 3px solid rgba($primary, 0.18);
  border-top-color: $primary;
  animation: spin 1s linear infinite;
}

.prep-card {
  padding: 18px;

  p {
    font-size: 14px;
    line-height: 1.8;
    color: $text-secondary;
  }
}

.prep-label {
  font-size: 14px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 10px;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-item {
  padding: 14px;
  border-radius: $radius-lg;
  background: rgba($primary, 0.04);

  h3 {
    font-size: 15px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 6px;
  }

  p {
    font-size: 13px;
    line-height: 1.7;
    color: $text-secondary;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulseCenter {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.2); }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes confettiFall {
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(400px) rotate(720deg); opacity: 0; }
}

@media (max-width: 480px) {
  .result-page {
    padding: 20px 14px 32px;
  }

  .screen-progress {
    grid-template-columns: repeat(2, 1fr);
  }

  .screen-panel {
    padding: 20px 16px;
  }

  .screen-actions {
    flex-direction: column;
  }
}
</style>
