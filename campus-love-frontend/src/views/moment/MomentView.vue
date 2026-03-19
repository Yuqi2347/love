<template>
  <div class="moment-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="pulse-ring" />
      <p>加载中...</p>
    </div>

    <!-- 未报名 -->
    <div v-else-if="status === 'NOT_ENROLLED'" class="hero-section">
      <div class="hero-card">
        <div class="hero-visual">
          <div class="floating-hearts">
            <span v-for="i in 6" :key="i" class="heart" :style="{ animationDelay: `${i * 0.5}s` }">💘</span>
          </div>
          <div class="hero-icon">💕</div>
        </div>
        <h1 class="hero-title">心动时刻</h1>
        <p class="hero-subtitle">每周一次的匿名深度配对活动</p>
        <p class="hero-desc">
          专为不善主动的你设计，填写问卷后由系统帮你找到最契合的人。<br>
          你可以自由选择想要匹配的<strong>对象类型</strong>，包容所有可能。
        </p>
        <div v-if="participantCount > 0" class="hero-stats">
          <span class="stat-number">{{ participantCount }}</span>
          <span class="stat-label">人已报名本周活动</span>
        </div>
        <button
          class="btn-hero"
          :disabled="!enrollmentOpen || !profileComplete"
          :title="!profileComplete ? '请先完善个人信息后进行分析' : ''"
          @click="profileComplete ? $router.push('/moment/enroll') : null"
        >
          {{ enrollmentOpen ? '参加本周活动' : '本周报名已截止' }}
        </button>
        <p v-if="!profileComplete" class="hero-tip profile-incomplete">请先完善个人信息后进行分析</p>
        <p v-else class="hero-tip">{{ enrollmentOpen ? '填写约 5 分钟的匿名问卷，即可参与匹配' : '请等待下周活动开放' }}</p>
      </div>
    </div>

    <!-- 已报名等待中 -->
    <div v-else-if="status === 'WAITING'" class="waiting-section">
      <div class="waiting-card">
        <div class="orbit-container">
          <div class="orbit orbit-1"><span class="orbit-dot">💗</span></div>
          <div class="orbit orbit-2"><span class="orbit-dot">💫</span></div>
          <div class="orbit orbit-3"><span class="orbit-dot">✨</span></div>
          <div class="center-icon">🔮</div>
        </div>
        <h2 class="waiting-title">正在为你寻找...</h2>
        <p class="waiting-desc">你的心动档案已提交，系统将为你匹配最契合的人</p>
        <div class="participant-badge">
          <span class="badge-icon">👥</span>
          <span>本周已有 <strong>{{ participantCount }}</strong> 人参与</span>
        </div>
        <p class="waiting-hint">匹配结果将在管理员触发后揭晓，请耐心等待</p>
      </div>
    </div>

    <!-- 匹配成功 -->
    <div v-else-if="status === 'MATCHED'" class="matched-section">
      <div class="matched-card">
        <div class="confetti-wrap">
          <span v-for="i in 12" :key="i" class="confetti" :style="confettiStyle(i)" />
        </div>
        <div class="matched-icon">💘</div>
        <h2 class="matched-title">{{ matchedTitle || '恭喜！找到你的心动对象' }}</h2>
        <p class="matched-desc">本周有人和你特别契合，快去看看吧！</p>
        <button class="btn-hero" @click="$router.push('/moment/result')">
          查看匹配结果
        </button>
      </div>
    </div>

    <!-- 未匹配 -->
    <div v-else-if="status === 'UNMATCHED'" class="unmatched-section">
      <div class="unmatched-card">
        <div class="moon-icon">🌙</div>
        <h2 class="unmatched-title">本周暂未找到最佳匹配</h2>
        <p class="unmatched-desc">
          这周报名人数中暂时没有找到和你完美契合的人，<br>
          但你的心动档案已保留，下周继续帮你寻找！
        </p>
        <div class="encouragement">
          <span class="encourage-icon">💪</span>
          <span>缘分总会在最好的时刻降临</span>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMomentStatus } from '@/api/momentApi'
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()
const profileComplete = computed(() => !!userStore.user?.profileComplete)

const loading = ref(true)
const status = ref<string>('NOT_ENROLLED')
const participantCount = ref(0)
const enrollmentOpen = ref(true)
const currentWeek = ref('')
const matchedTitle = ref('')

async function fetchStatus() {
  loading.value = true
  try {
    const res = await getMomentStatus()
    const data = res.data.data
    if (data) {
      status.value = data.status
      participantCount.value = data.participantCount
      enrollmentOpen.value = data.enrollmentOpen
      currentWeek.value = data.currentWeek
      matchedTitle.value = data.matchedTitle || ''
    }
  } catch {
    status.value = 'NOT_ENROLLED'
    matchedTitle.value = ''
  } finally {
    loading.value = false
  }
}

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

onMounted(fetchStatus)
</script>

<style lang="scss" scoped>
.moment-page {
  min-height: 100vh;
  padding: 40px 24px;
  padding-bottom: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

// ==================== 加载 ====================
.loading-state {
  text-align: center;
  color: $text-muted;

  .pulse-ring {
    width: 48px;
    height: 48px;
    margin: 0 auto 16px;
    border-radius: 50%;
    border: 3px solid #ffd6ea;
    border-top-color: #ff88bd;
    animation: spin 1s linear infinite;
  }
}

// ==================== 未报名 Hero ====================
.hero-section {
  width: 100%;
  max-width: 480px;
}

.hero-card {
  text-align: center;
  padding: 48px 32px;
  background: $bg-primary;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
  position: relative;
  overflow: hidden;
}

.hero-visual {
  position: relative;
  height: 120px;
  margin-bottom: 24px;
}

.hero-icon {
  font-size: 64px;
  position: relative;
  z-index: 1;
  animation: pulse 2s ease-in-out infinite;
}

.floating-hearts {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.heart {
  position: absolute;
  font-size: 20px;
  opacity: 0;
  animation: floatUp 4s ease-in-out infinite;

  &:nth-child(1) { left: 15%; }
  &:nth-child(2) { left: 30%; }
  &:nth-child(3) { left: 50%; }
  &:nth-child(4) { left: 65%; }
  &:nth-child(5) { left: 80%; }
  &:nth-child(6) { left: 45%; }
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff8fc3 0%, #ffb6d9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
}

.hero-subtitle {
  font-size: 16px;
  color: $text-secondary;
  margin-bottom: 20px;
}

.hero-desc {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.8;
  margin-bottom: 24px;

  strong { color: #ff78b4; font-weight: 600; }
}

.hero-stats {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 8px 20px;
  background: rgba(255, 122, 180, 0.1);
  border-radius: $radius-full;
  margin-bottom: 24px;

  .stat-number {
    font-size: 24px;
    font-weight: 800;
    color: #ff78b4;
  }
  .stat-label {
    font-size: 14px;
    color: $text-secondary;
  }
}

.btn-hero {
  width: 100%;
  max-width: 280px;
  height: 52px;
  background: linear-gradient(135deg, #ff7ab4 0%, #ff9dc9 100%);
  color: white;
  border: none;
  border-radius: $radius-full;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: all $transition-base;
  box-shadow: 0 4px 16px rgba(255, 122, 180, 0.35);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 24px rgba(255, 122, 180, 0.45);
  }

  &:active {
    transform: translateY(0);
  }
}

.hero-tip {
  margin-top: 16px;
  font-size: 13px;
  color: $text-muted;
}
.hero-tip.profile-incomplete {
  color: var(--el-color-warning);
}

// ==================== 等待中 ====================
.waiting-section {
  width: 100%;
  max-width: 480px;
}

.waiting-card {
  text-align: center;
  padding: 48px 32px;
  background: $bg-primary;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
}

.orbit-container {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 32px;
}

.center-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 40px;
  animation: pulse 2s ease-in-out infinite;
}

.orbit {
  position: absolute;
  border: 1px dashed $border-color;
  border-radius: 50%;
  animation: spin 6s linear infinite;

  &-1 { inset: 20px; }
  &-2 { inset: 5px; animation-duration: 8s; animation-direction: reverse; }
  &-3 { inset: -10px; animation-duration: 10s; }
}

.orbit-dot {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 18px;
}

.waiting-title {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.waiting-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 24px;
}

.participant-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 122, 180, 0.1);
  border-radius: $radius-full;
  font-size: 14px;
  color: $text-secondary;

  .badge-icon { font-size: 18px; }
  strong { color: #ff78b4; font-weight: 700; }
}

.waiting-hint {
  margin-top: 24px;
  font-size: 13px;
  color: $text-muted;
}

// ==================== 匹配成功 ====================
.matched-section {
  width: 100%;
  max-width: 480px;
}

.matched-card {
  text-align: center;
  padding: 48px 32px;
  background: $bg-primary;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
  position: relative;
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
  margin-bottom: 16px;
  animation: bounce 1s ease-in-out infinite;
  position: relative;
  z-index: 1;
}

.matched-title {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff8fc3 0%, #ffb6d9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
}

.matched-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

// ==================== 未匹配 ====================
.unmatched-section {
  width: 100%;
  max-width: 480px;
}

.unmatched-card {
  text-align: center;
  padding: 48px 32px;
  background: $bg-primary;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
}

.moon-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.unmatched-title {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 12px;
}

.unmatched-desc {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.8;
  margin-bottom: 24px;
}

.encouragement {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(255, 160, 206, 0.16);
  border-radius: $radius-full;
  font-size: 14px;
  color: $text-secondary;
  font-weight: 500;

  .encourage-icon { font-size: 18px; }
}

// ==================== 动画 ====================
@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes floatUp {
  0% { transform: translateY(40px); opacity: 0; }
  20% { opacity: 0.8; }
  80% { opacity: 0.4; }
  100% { transform: translateY(-80px); opacity: 0; }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes confettiFall {
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(400px) rotate(720deg); opacity: 0; }
}
</style>
