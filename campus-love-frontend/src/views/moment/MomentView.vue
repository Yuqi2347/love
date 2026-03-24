<template>
  <div class="moment-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="pulse-ring" />
      <p>加载中...</p>
    </div>

    <template v-else>
      <header class="moment-head">
        <p class="moment-eyebrow">MOMENT</p>
        <h1 class="moment-title">
          遇见<span class="moment-title__accent">真正契合</span>你的人
        </h1>
        <p class="moment-lead">
          匿名问卷 + 多维度匹配，专为不善主动的你设计；<br />
          那个人也许就在这周，你不来就真的错过了。
        </p>
        <div class="moment-week-pill">{{ weekBadgeText }}</div>
      </header>

      <div class="moment-countdown" aria-label="心动揭晓倒计时" role="timer">
        <p class="moment-countdown__title">心动揭晓倒计时</p>
        <template v-if="resultPublished">
          <p class="moment-countdown__done">本期结果已公布</p>
        </template>
        <template v-else-if="showRevealOverdueWait">
          <p class="moment-countdown__wait">预计本期揭晓时刻已过，请等待系统发布结果</p>
        </template>
        <template v-else-if="countdownParts">
          <div class="moment-countdown__digits" aria-live="polite">
            <span v-if="countdownParts.days > 0" class="moment-countdown__unit">
              <strong>{{ countdownParts.days }}</strong>
              <small>天</small>
            </span>
            <span class="moment-countdown__sep" aria-hidden="true" />
            <span class="moment-countdown__unit moment-countdown__unit--time">
              <strong>{{ pad2(countdownParts.hours) }}</strong>
              <small>时</small>
            </span>
            <span class="moment-countdown__colon">:</span>
            <span class="moment-countdown__unit moment-countdown__unit--time">
              <strong>{{ pad2(countdownParts.minutes) }}</strong>
              <small>分</small>
            </span>
            <span class="moment-countdown__colon">:</span>
            <span class="moment-countdown__unit moment-countdown__unit--time">
              <strong>{{ pad2(countdownParts.seconds) }}</strong>
              <small>秒</small>
            </span>
          </div>
          <p class="moment-countdown__hint">
            <template v-if="countdownIsNextFridayHint">
              下期预计 {{ revealHintText }} 揭晓（北京时间），实际以系统发布为准
            </template>
            <template v-else>预计 {{ revealHintText }} 揭晓（北京时间），实际以系统发布为准</template>
          </p>
        </template>
        <p v-else class="moment-countdown__wait">预计每周五中午揭晓，请留意页面更新</p>
      </div>

      <div class="moment-tabs" role="tablist" aria-label="活动阶段">
        <span
          v-for="(tab, i) in phaseTabs"
          :key="tab.key"
          class="moment-tab"
          :class="{ 'moment-tab--on': phaseTabIndex === i }"
        >
          {{ tab.label }}
        </span>
      </div>

      <div class="moment-card">
        <!-- 未报名 -->
        <div v-if="status === 'NOT_ENROLLED'" class="moment-panel">
          <div class="moment-panel__icon" aria-hidden="true">💗</div>
          <p class="moment-panel__tag">{{ enrollmentOpen ? '本周报名开放中' : '本周报名已截止' }}</p>
          <h2 class="moment-panel__h">参加本周心动时刻</h2>
          <div class="moment-mini-grid">
            <div class="moment-mini-cell">
              <span class="moment-mini-cell__n">{{ participantCount }}</span>
              <span class="moment-mini-cell__t">人已报名</span>
            </div>
            <div class="moment-mini-cell">
              <span class="moment-mini-cell__n">周四</span>
              <span class="moment-mini-cell__t">截止参考</span>
            </div>
            <div class="moment-mini-cell">
              <span class="moment-mini-cell__n">周五</span>
              <span class="moment-mini-cell__t">匹配揭晓</span>
            </div>
          </div>
          <button
            class="moment-btn-outline"
            :disabled="!enrollmentOpen || !profileComplete"
            :title="!profileComplete ? '请先完善个人信息后进行分析' : ''"
            @click="profileComplete ? $router.push('/moment/enroll') : null"
          >
            {{ enrollmentOpen ? '参加本周活动' : '本周报名已截止' }}
          </button>
          <p v-if="!profileComplete" class="moment-footnote moment-footnote--warn">请先完善个人信息后进行分析</p>
          <p v-else class="moment-footnote">问卷约 5 分钟，提交前可随时修改或撤回报名。</p>
        </div>

        <!-- 已报名等待中 -->
        <div v-else-if="status === 'WAITING'" class="moment-panel moment-panel--soft">
          <div class="orbit-container">
            <div class="orbit orbit-1"><span class="orbit-dot">💗</span></div>
            <div class="orbit orbit-2"><span class="orbit-dot">💫</span></div>
            <div class="orbit orbit-3"><span class="orbit-dot">✨</span></div>
            <div class="center-icon">🔮</div>
          </div>
          <h2 class="moment-panel__h">正在为你寻找...</h2>
          <p class="moment-panel__p">你的心动档案已提交，系统将为你匹配更契合的人</p>
          <div class="participant-badge">
            <span class="badge-icon">👥</span>
            <span>本周已有 <strong>{{ participantCount }}</strong> 人参与</span>
          </div>
          <p class="moment-footnote">匹配结果将在管理员触发后揭晓，请耐心等待</p>
        </div>

        <!-- 匹配成功 -->
        <div v-else-if="status === 'MATCHED'" class="moment-panel">
          <div v-if="resultPublished" class="confetti-wrap">
            <span v-for="i in 12" :key="i" class="confetti" :style="confettiStyle(i)" />
          </div>
          <div class="moment-panel__icon" aria-hidden="true">💘</div>
          <h2 class="moment-panel__h">{{ matchedTitle || '恭喜！找到你的心动对象' }}</h2>
          <p class="moment-panel__p">
            <template v-if="resultPublished">本周有人和你特别契合，快去看看吧！</template>
            <template v-else>匹配已完成，请等待管理员公布本周结果后再查看详情。</template>
          </p>
          <button
            type="button"
            class="moment-btn-solid"
            :disabled="!resultPublished"
            @click="$router.push('/moment/result')"
          >
            {{ resultPublished ? '查看匹配结果' : '匹配完成，等待系统发布结果' }}
          </button>
        </div>

        <!-- 未匹配 -->
        <div v-else-if="status === 'UNMATCHED'" class="moment-panel moment-panel--soft">
          <div class="moon-icon">🌙</div>
          <h2 class="moment-panel__h">本周暂未找到最佳匹配</h2>
          <p class="moment-panel__p">
            这周暂时没有找到和你完美契合的人，心动档案已保留，下周继续帮你寻找。
          </p>
          <div class="encouragement">
            <span class="encourage-icon">💪</span>
            <span>缘分总会在最好的时刻降临</span>
          </div>
        </div>
      </div>

      <section class="moment-flow" aria-labelledby="moment-flow-title">
        <h2 id="moment-flow-title" class="moment-section-title">活动流程</h2>
        <ol class="moment-flow-list">
          <li>
            <span class="moment-flow-list__i">1</span>
            <div>
              <strong>填写匿名问卷</strong>
              <p>约 5 分钟，信息仅用于匹配算法，身份默认匿名。</p>
            </div>
          </li>
          <li>
            <span class="moment-flow-list__i">2</span>
            <div>
              <strong>算法深度匹配</strong>
              <p>报名截止后多维度打分，在契合与安全之间做平衡。</p>
            </div>
          </li>
          <li>
            <span class="moment-flow-list__i">3</span>
            <div>
              <strong>揭晓与选择</strong>
              <p>结果公布后可查看报告，并选择与 TA 的下一步节奏。</p>
            </div>
          </li>
        </ol>
      </section>

      <section class="moment-shield" aria-labelledby="moment-shield-title">
        <h2 id="moment-shield-title" class="moment-section-title">匿名保护</h2>
        <p class="moment-shield__p">
          在双方确认前，个人敏感信息不会向对方展示；匹配与展示规则遵循平台隐私说明。
        </p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
const weekStatus = ref<string | null>(null)
const revealAtEpochMillis = ref<number | null>(null)
const nowMs = ref(Date.now())

const resultPublished = computed(() => weekStatus.value === 'PUBLISHED')

const phaseTabs = [
  { key: 'open', label: '未报名' },
  { key: 'enrolled', label: '已报名' },
  { key: 'match', label: '匹配中' },
  { key: 'result', label: '结果已出' },
] as const

const phaseTabIndex = computed(() => {
  const ws = weekStatus.value
  if (status.value === 'NOT_ENROLLED') return 0
  if (ws === 'MATCHING' || ws === 'AI_ANALYZING') return 2
  if (status.value === 'MATCHED' || status.value === 'UNMATCHED') return 3
  if (status.value === 'WAITING') return 1
  return 0
})

const weekBadgeText = computed(() => {
  const w = currentWeek.value
  if (!w) return '本周活动'
  const m = w.match(/^(\d{4})-W(\d{1,2})$/i)
  if (!m) return w
  return `Week ${Number(m[2])} · ${m[1]}年`
})

/** ISO 周周一（UTC 日历日），与 Java WeekFields.ISO + Jan 4 锚点一致 */
function isoWeekMondayUtc(isoYear: number, week: number): Date {
  const jan4 = new Date(Date.UTC(isoYear, 0, 4))
  const jan4Dow = jan4.getUTCDay() || 7
  const week1Monday = new Date(jan4)
  week1Monday.setUTCDate(jan4.getUTCDate() - jan4Dow + 1)
  const monday = new Date(week1Monday)
  monday.setUTCDate(week1Monday.getUTCDate() + (week - 1) * 7)
  return monday
}

/** 与 PairDateTimeUtils.revealFriday + 周五 12:00 Asia/Shanghai 对齐（无夏令时，+8） */
function revealEpochFromWeekTag(weekTag: string): number | null {
  const m = weekTag.match(/^(\d{4})-W(\d{1,2})$/i)
  if (!m) return null
  const isoYear = Number(m[1])
  const week = Number(m[2])
  const monday = isoWeekMondayUtc(isoYear, week)
  const fr = new Date(monday)
  fr.setUTCDate(monday.getUTCDate() + 4)
  return Date.UTC(fr.getUTCFullYear(), fr.getUTCMonth(), fr.getUTCDate(), 4, 0, 0)
}

/** 本期周标签对应的周五 12:00（与后端一致） */
const baseRevealAt = computed(() => {
  if (revealAtEpochMillis.value != null) return revealAtEpochMillis.value
  const tag = currentWeek.value
  if (!tag) return null
  return revealEpochFromWeekTag(tag)
})

/**
 * 展示用揭晓时刻：未报名且「本期」周五已过 → 顺推到**下一场**周五 12:00，避免周末仍显示「时刻已到」却无倒计时。
 * 已报名/匹配中等仍看本期，过期则走 showRevealOverdueWait。
 */
const displayRevealAt = computed(() => {
  const t0 = baseRevealAt.value
  if (t0 == null || resultPublished.value) return null
  if (status.value !== 'NOT_ENROLLED') return t0
  const weekMs = 7 * 24 * 60 * 60 * 1000
  let t = t0
  let guard = 0
  while (nowMs.value >= t && guard < 104) {
    t += weekMs
    guard++
  }
  return t
})

/** 未使用下期顺推时，hint 仍显示「预计 x月x日」 */
const countdownIsNextFridayHint = computed(() => {
  const b = baseRevealAt.value
  const d = displayRevealAt.value
  if (b == null || d == null || status.value !== 'NOT_ENROLLED') return false
  return d > b
})

/** 已参与本期流程的用户：本期周五已过且仍未发布 → 提示等待发布（不顺推到下期） */
const showRevealOverdueWait = computed(() => {
  if (resultPublished.value) return false
  const t0 = baseRevealAt.value
  if (t0 == null) return false
  if (status.value === 'NOT_ENROLLED') return false
  return nowMs.value >= t0
})

const countdownParts = computed(() => {
  if (resultPublished.value) return null
  const t = displayRevealAt.value
  if (t == null) return null
  const diff = t - nowMs.value
  if (diff <= 0) return null
  const sec = Math.floor(diff / 1000)
  return {
    days: Math.floor(sec / 86400),
    hours: Math.floor((sec % 86400) / 3600),
    minutes: Math.floor((sec % 3600) / 60),
    seconds: sec % 60,
  }
})

const revealHintText = computed(() => {
  const t = displayRevealAt.value
  if (t == null) return ''
  const d = new Date(t)
  const mm = d.getMonth() + 1
  const dd = d.getDate()
  const hh = d.getHours()
  const min = d.getMinutes()
  return `${mm}月${dd}日 ${pad2(hh)}:${pad2(min)}`
})

function pad2(n: number) {
  return n < 10 ? `0${n}` : `${n}`
}

let countdownTimer: ReturnType<typeof setInterval> | null = null

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
      weekStatus.value = data.weekStatus ?? null
      revealAtEpochMillis.value =
        data.revealAtEpochMillis != null && data.revealAtEpochMillis !== undefined
          ? Number(data.revealAtEpochMillis)
          : null
    }
  } catch {
    status.value = 'NOT_ENROLLED'
    matchedTitle.value = ''
    weekStatus.value = null
    revealAtEpochMillis.value = null
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

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    fetchStatus()
  }
}

onMounted(() => {
  fetchStatus()
  document.addEventListener('visibilitychange', onVisibilityChange)
  countdownTimer = setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style lang="scss" scoped>
$burgundy: #5c2a38;
$rose: #c76b7e;
$bg: #faf6f7;
$card: #ffffff;
$muted: #8a7080;

.moment-page {
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
  padding: 28px 20px 100px;
  margin: 0;
  background:
    radial-gradient(ellipse 120% 80% at 50% -20%, rgba(255, 230, 240, 0.65), transparent 55%),
    linear-gradient(180deg, $bg 0%, #fff 45%, #fffefc 100%);
}

.loading-state {
  text-align: center;
  padding-top: 100px;
  color: $muted;

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

.moment-head {
  text-align: center;
  margin-bottom: 20px;
}

.moment-eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: 0.42em;
  color: #b0909c;
  font-weight: 600;
}

.moment-title {
  margin: 0 0 12px;
  font-size: 1.65rem;
  font-weight: 800;
  line-height: 1.35;
  color: $burgundy;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
}

.moment-title__accent {
  color: #8b2942;
  background: linear-gradient(120deg, #8b2942, #c76b7e);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.moment-lead {
  margin: 0 auto 16px;
  max-width: 400px;
  font-size: 0.88rem;
  line-height: 1.65;
  color: $muted;
}

.moment-week-pill {
  display: inline-block;
  padding: 8px 18px;
  border-radius: 999px;
  font-size: 0.78rem;
  color: #7a4e5c;
  background: rgba(255, 200, 215, 0.35);
  border: 1px solid rgba(200, 140, 160, 0.25);
}

.moment-countdown {
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 16px;
  text-align: center;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(200, 160, 175, 0.22);
  box-shadow: 0 4px 18px rgba(90, 40, 55, 0.05);
}

.moment-countdown__title {
  margin: 0 0 12px;
  font-size: 0.85rem;
  font-weight: 700;
  color: $burgundy;
  letter-spacing: 0.06em;
}

.moment-countdown__done {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: $rose;
}

.moment-countdown__wait {
  margin: 0;
  font-size: 0.82rem;
  line-height: 1.55;
  color: $muted;
}

.moment-countdown__digits {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: center;
  gap: 6px 10px;
  margin-bottom: 10px;
}

.moment-countdown__unit {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  min-width: 2.2rem;

  strong {
    font-size: 1.5rem;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    color: $rose;
    line-height: 1.1;
  }

  small {
    font-size: 0.62rem;
    color: #9a7d88;
    margin-top: 2px;
  }
}

.moment-countdown__unit--time strong {
  font-size: 1.35rem;
}

.moment-countdown__sep {
  width: 8px;
}

.moment-countdown__colon {
  font-size: 1.2rem;
  font-weight: 700;
  color: #c9a8b4;
  align-self: center;
  padding: 0 2px;
}

.moment-countdown__hint {
  margin: 0;
  font-size: 0.72rem;
  line-height: 1.5;
  color: #a898a0;
}

.moment-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 14px;
  padding: 4px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(200, 160, 175, 0.2);
}

.moment-tab {
  flex: 1;
  text-align: center;
  padding: 10px 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #a08894;
  border-radius: 10px;
  transition: background 0.2s, color 0.2s;
}

.moment-tab--on {
  background: #fff;
  color: $burgundy;
  box-shadow: 0 2px 10px rgba(80, 40, 55, 0.08);
}

.moment-card {
  border-radius: 18px;
  background: $card;
  border: 1px solid rgba(200, 170, 185, 0.28);
  box-shadow: 0 12px 40px rgba(70, 35, 50, 0.07);
  overflow: hidden;
}

.moment-panel {
  position: relative;
  padding: 28px 22px 26px;
  text-align: center;
}

.moment-panel--soft {
  background: linear-gradient(180deg, #fffefb 0%, #fff9fb 100%);
}

.moment-panel__icon {
  font-size: 2.5rem;
  margin-bottom: 8px;
}

.moment-panel__tag {
  margin: 0 0 6px;
  font-size: 0.8rem;
  font-weight: 600;
  color: $rose;
}

.moment-panel__h {
  margin: 0 0 10px;
  font-size: 1.2rem;
  font-weight: 700;
  color: $burgundy;
}

.moment-panel__p {
  margin: 0 0 16px;
  font-size: 0.88rem;
  line-height: 1.65;
  color: $muted;
}

.moment-mini-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 18px 0 20px;
}

.moment-mini-cell {
  padding: 10px 6px;
  border-radius: 12px;
  background: #fdf8f9;
  border: 1px solid rgba(220, 190, 200, 0.35);
}

.moment-mini-cell__n {
  display: block;
  font-size: 0.95rem;
  font-weight: 700;
  color: $burgundy;
}

.moment-mini-cell__t {
  font-size: 0.65rem;
  color: #a09098;
  margin-top: 4px;
  display: block;
}

.moment-btn-outline {
  width: 100%;
  max-width: 280px;
  height: 50px;
  margin: 0 auto;
  display: block;
  border-radius: 999px;
  border: 1.5px solid rgba(180, 120, 145, 0.55);
  background: #fff;
  color: $burgundy;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(90, 40, 55, 0.1);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.moment-btn-solid {
  width: 100%;
  max-width: 280px;
  height: 50px;
  margin: 0 auto;
  display: block;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #e08ba0 0%, #c76b7e 100%);
  color: #fff;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 22px rgba(180, 90, 110, 0.35);
  transition: transform 0.15s, opacity 0.15s;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
  }

  &:disabled {
    opacity: 0.85;
    cursor: not-allowed;
    background: linear-gradient(135deg, #c4b8bc 0%, #b5a9ae 100%);
    box-shadow: none;
  }
}

.moment-footnote {
  margin-top: 14px;
  font-size: 0.75rem;
  color: #a898a0;
  line-height: 1.5;
}

.moment-footnote--warn {
  color: var(--el-color-warning);
}

.orbit-container {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 24px;
}

.center-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 36px;
  animation: pulse 2s ease-in-out infinite;
}

.orbit {
  position: absolute;
  border: 1px dashed rgba(200, 160, 175, 0.45);
  border-radius: 50%;
  animation: spin 6s linear infinite;

  &-1 {
    inset: 18px;
  }
  &-2 {
    inset: 4px;
    animation-duration: 8s;
    animation-direction: reverse;
  }
  &-3 {
    inset: -8px;
    animation-duration: 10s;
  }
}

.orbit-dot {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 16px;
}

.participant-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: rgba(255, 200, 215, 0.25);
  border-radius: 999px;
  font-size: 0.85rem;
  color: $muted;

  .badge-icon {
    font-size: 16px;
  }
  strong {
    color: $rose;
    font-weight: 700;
  }
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

.moon-icon {
  font-size: 52px;
  margin-bottom: 8px;
}

.encouragement {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 210, 225, 0.25);
  border-radius: 999px;
  font-size: 0.85rem;
  color: $muted;
}

.moment-flow {
  margin-top: 28px;
  padding: 22px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(200, 170, 185, 0.22);
}

.moment-section-title {
  margin: 0 0 14px;
  font-size: 1rem;
  font-weight: 700;
  color: $burgundy;
}

.moment-flow-list {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    &:last-child {
      margin-bottom: 0;
    }
    strong {
      display: block;
      font-size: 0.9rem;
      color: #4a3038;
      margin-bottom: 4px;
    }
    p {
      margin: 0;
      font-size: 0.8rem;
      line-height: 1.55;
      color: $muted;
    }
  }
}

.moment-flow-list__i {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(145deg, #e8a0b0, #c76b7e);
}

.moment-shield {
  margin-top: 16px;
  padding: 18px 18px 20px;
  border-radius: 18px;
  background: rgba(255, 250, 252, 0.9);
  border: 1px dashed rgba(200, 160, 175, 0.35);
}

.moment-shield__p {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.6;
  color: #9a8890;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%,
  100% {
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    transform: translate(-50%, -50%) scale(1.08);
  }
}

@keyframes confettiFall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(400px) rotate(720deg);
    opacity: 0;
  }
}
</style>



