<template>
  <div class="moment-page">
    <div v-if="loading" class="loading-state">
      <div class="pulse-ring" />
      <p>感知心动信号中...</p>
    </div>

    <template v-else>
      <header class="moment-head">
        <p class="moment-eyebrow">WEEKLY MOMENT</p>
        <h1 class="moment-title">
          遇见<span class="moment-title__accent">真正契合</span>你的人
        </h1>
        <p class="moment-lead">
          匿名问卷 + 多维度匹配，专为不善主动的你设计；<br />
          那个人也许就在这周，你不来就真的错过了。
        </p>
        <div class="moment-week-pill glass-pill">{{ weekBadgeText }}</div>
      </header>

      <div class="moment-countdown glass-panel" aria-label="心动揭晓倒计时" role="timer">
        <p class="moment-countdown__title">心动揭晓倒计时</p>
        <template v-if="resultPublished">
          <p class="moment-countdown__done text-gradient-warm">本期结果已公布</p>
        </template>
        <template v-else-if="showRevealOverdueWait">
          <p class="moment-countdown__wait">预计本期揭晓时刻已过，请等待系统发布结果</p>
        </template>
        <template v-else-if="countdownParts">
          <div class="moment-countdown__digits" aria-live="polite">
            <span v-if="countdownParts.days > 0" class="moment-countdown__unit">
              <strong>{{ countdownParts.days }}</strong>
              <small>DAYS</small>
            </span>
            <span class="moment-countdown__sep" aria-hidden="true" />
            <span class="moment-countdown__unit moment-countdown__unit--time">
              <strong>{{ pad2(countdownParts.hours) }}</strong>
              <small>HOURS</small>
            </span>
            <span class="moment-countdown__colon">:</span>
            <span class="moment-countdown__unit moment-countdown__unit--time">
              <strong>{{ pad2(countdownParts.minutes) }}</strong>
              <small>MINS</small>
            </span>
            <span class="moment-countdown__colon">:</span>
            <span class="moment-countdown__unit moment-countdown__unit--time">
              <strong>{{ pad2(countdownParts.seconds) }}</strong>
              <small>SECS</small>
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

      <div class="moment-tabs tuner-capsule glass-panel" role="tablist" aria-label="活动阶段">
        <span
          v-for="(tab, i) in phaseTabs"
          :key="tab.key"
          class="moment-tab tuner-btn"
          :class="{ 'active': phaseTabIndex === i }"
        >
          {{ tab.label }}
        </span>
      </div>

      <div class="moment-card glass-panel">
        
        <div v-if="status === 'NOT_ENROLLED'" class="moment-panel">
          <div class="moment-panel__icon float-anim" aria-hidden="true">💌</div>
          <p class="moment-panel__tag text-gradient-warm">{{ enrollmentOpen ? '本周报名通道开放中' : '本周报名已截止' }}</p>
          <h2 class="moment-panel__h">参加本周心动时刻</h2>
          <div class="moment-mini-grid">
            <div class="moment-mini-cell glass-pill-light">
              <span class="moment-mini-cell__n">{{ participantCount }}</span>
              <span class="moment-mini-cell__t">人已报名</span>
            </div>
            <div class="moment-mini-cell glass-pill-light">
              <span class="moment-mini-cell__n">周四</span>
              <span class="moment-mini-cell__t">截止参考</span>
            </div>
            <div class="moment-mini-cell glass-pill-light">
              <span class="moment-mini-cell__n">周五</span>
              <span class="moment-mini-cell__t">匹配揭晓</span>
            </div>
          </div>
          <button
            class="glow-btn-warm w-full mt-4"
            :disabled="!enrollmentOpen || !profileComplete"
            :title="!profileComplete ? '请先完善个人信息后进行分析' : ''"
            @click="profileComplete ? $router.push('/moment/enroll') : null"
          >
            {{ enrollmentOpen ? '立即参加本周活动' : '本周报名已截止' }}
          </button>
          <p v-if="!profileComplete" class="moment-footnote moment-footnote--warn">请先完善个人信息后进行分析</p>
          <p v-else class="moment-footnote">仅需 5 分钟填写问卷，提交前可随时修改或撤回。</p>
        </div>

        <div v-else-if="status === 'WAITING'" class="moment-panel">
          <div class="orbit-container">
            <div class="orbit orbit-1"><span class="orbit-dot dot-pink"></span></div>
            <div class="orbit orbit-2"><span class="orbit-dot dot-blue"></span></div>
            <div class="orbit orbit-3"><span class="orbit-dot dot-orange"></span></div>
            <div class="center-icon pulse-anim">🔮</div>
          </div>
          <h2 class="moment-panel__h">宇宙正在为你调频...</h2>
          <p class="moment-panel__p">你的心动档案已接入引力场，AI 正在计算最契合的星轨</p>
          <div class="participant-badge glass-pill">
            <span class="badge-icon">👥</span>
            <span>本周已有 <strong class="text-gradient-warm">{{ participantCount }}</strong> 人发出信号</span>
          </div>
          <p class="moment-footnote mt-4">当两颗星交汇时，我们会第一时间通知你</p>
        </div>

        <div v-else-if="status === 'MATCHED'" class="moment-panel">
          <div v-if="resultPublished" class="confetti-wrap">
            <span v-for="i in 12" :key="i" class="confetti" :style="confettiStyle(i)" />
          </div>
          <div class="moment-panel__icon heartbeat-anim" aria-hidden="true">💘</div>
          <h2 class="moment-panel__h text-gradient-warm">{{ matchedTitle || '恭喜！信号捕捉成功' }}</h2>
          <p class="moment-panel__p">
            <template v-if="resultPublished">本周有人和你处于同一频段，快去查收专属匹配报告吧！</template>
            <template v-else>星轨交汇已完成。等待管理员发布最终结果后即可查看详情。</template>
          </p>
          <button
            type="button"
            class="glow-btn-warm w-full mt-4"
            :disabled="!resultPublished"
            @click="$router.push('/moment/result')"
          >
            {{ resultPublished ? '查看心动对象' : '等待系统揭晓' }}
          </button>
        </div>

        <div v-else-if="status === 'UNMATCHED'" class="moment-panel">
          <div class="moment-panel__icon float-anim">🌙</div>
          <h2 class="moment-panel__h">本周星轨暂未交汇</h2>
          <p class="moment-panel__p">
            这周的引力场中暂未发现与你完美同频的信号。你的档案已安全保留，下周我们继续寻找。
          </p>
          <div class="encouragement glass-pill">
            <span class="encourage-icon">✨</span>
            <span>最对的人，往往需要一点时间</span>
          </div>
        </div>
      </div>

      <div class="info-sections-wrapper">
        <section class="moment-flow glass-card-light" aria-labelledby="moment-flow-title">
          <h2 id="moment-flow-title" class="moment-section-title">运作机制</h2>
          <ol class="moment-flow-list">
            <li>
              <span class="moment-flow-list__i">1</span>
              <div>
                <strong>提交匿名档案</strong>
                <p>约 5 分钟，信息仅喂给匹配算法，对人类保持绝对匿名。</p>
              </div>
            </li>
            <li>
              <span class="moment-flow-list__i">2</span>
              <div>
                <strong>AI 深度演算</strong>
                <p>多维度偏好打分，在三观契合与校园安全之间找到最佳平衡。</p>
              </div>
            </li>
            <li>
              <span class="moment-flow-list__i">3</span>
              <div>
                <strong>揭晓与破冰</strong>
                <p>解锁匹配报告，由你决定是否要与 TA 开启真实的校园相遇。</p>
              </div>
            </li>
          </ol>
        </section>

        <section class="moment-shield glass-card-light" aria-labelledby="moment-shield-title">
          <h2 id="moment-shield-title" class="moment-section-title">
            <el-icon><Lock /></el-icon> 绝对隐私保护
          </h2>
          <p class="moment-shield__p">
            在双方点击“确认破冰”前，你的姓名、专业等敏感身份信息将被完全隐藏。Campal 采用加密逻辑，确保你的每一次心动都安全无虞。
          </p>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getMomentStatus } from '@/api/momentApi'
import { useUserStore } from '@/store/userStore'
import { Lock } from '@element-plus/icons-vue' // 新增的小图标

// ==========================================
// 核心业务逻辑 100% 保持原封不动
// ==========================================
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

function isoWeekMondayUtc(isoYear: number, week: number): Date {
  const jan4 = new Date(Date.UTC(isoYear, 0, 4))
  const jan4Dow = jan4.getUTCDay() || 7
  const week1Monday = new Date(jan4)
  week1Monday.setUTCDate(jan4.getUTCDate() - jan4Dow + 1)
  const monday = new Date(week1Monday)
  monday.setUTCDate(week1Monday.getUTCDate() + (week - 1) * 7)
  return monday
}

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

const baseRevealAt = computed(() => {
  if (revealAtEpochMillis.value != null) return revealAtEpochMillis.value
  const tag = currentWeek.value
  if (!tag) return null
  return revealEpochFromWeekTag(tag)
})

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

const countdownIsNextFridayHint = computed(() => {
  const b = baseRevealAt.value
  const d = displayRevealAt.value
  if (b == null || d == null || status.value !== 'NOT_ENROLLED') return false
  return d > b
})

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

function pad2(n: number) { return n < 10 ? `0${n}` : `${n}` }

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
      revealAtEpochMillis.value = data.revealAtEpochMillis != null && data.revealAtEpochMillis !== undefined ? Number(data.revealAtEpochMillis) : null
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

// 修改了纸屑颜色的生成，更契合晨曦极光主题
function confettiStyle(i: number) {
  const colors = ['#FF3366', '#FF7B54', '#4f8cff', '#a78bfa'];
  const color = colors[i % colors.length];
  const left = 10 + (i * 7) % 80
  const delay = (i * 0.2) % 1.5
  const duration = 2.5 + (i * 0.2) % 1.5
  return {
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    background: color,
  }
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') fetchStatus()
}

onMounted(() => {
  fetchStatus()
  document.addEventListener('visibilitychange', onVisibilityChange)
  countdownTimer = setInterval(() => { nowMs.value = Date.now() }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 心动时刻 UI
   ========================================== */
$bg-aurora: #f8fafc; 
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b; 
$text-sub: #64748b;  
$border-light: rgba(255, 255, 255, 0.8);

.moment-page {
  box-sizing: border-box; width: 100%; min-height: 100vh;
  padding: 24px 20px 100px; margin: 0; position: relative;
  background: $bg-aurora; color: $text-main;
  
  // 晨曦极光弥散背景
  &::before {
    content: ''; position: fixed; inset: 0; pointer-events: none;
    background: 
      radial-gradient(circle at 15% 10%, rgba(255, 51, 102, 0.08), transparent 45%),
      radial-gradient(circle at 85% 30%, rgba(79, 140, 255, 0.06), transparent 45%),
      radial-gradient(circle at 50% 80%, rgba(255, 123, 84, 0.05), transparent 50%);
    z-index: 0;
  }
}
.moment-page > * { position: relative; z-index: 1; max-width: 600px; margin-left: auto; margin-right: auto; }

// --- 通用组件 ---
.glass-panel {
  background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light; box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05); border-radius: 20px;
}
.glass-pill {
  background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 999px;
}
.glass-pill-light {
  background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.5); border-radius: 12px;
}
.glass-card-light {
  background: rgba(255, 255, 255, 0.4); backdrop-filter: blur(12px); border: 1px solid #fff; border-radius: 18px;
}

// 文本高光渐变
.text-gradient-warm {
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800;
}

// 通用流光暖色按钮
.glow-btn-warm {
  height: 52px; border-radius: 999px; border: none;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white;
  font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 1px;
  box-shadow: 0 8px 25px rgba(255, 51, 102, 0.3); transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  
  &:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 12px 30px rgba(255, 51, 102, 0.4); }
  &:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.8; color: #fff; }
}
.w-full { width: 100%; }
.mt-4 { margin-top: 16px; }

// --- 加载与动画 ---
.loading-state { text-align: center; padding-top: 120px; color: $text-sub; }
.pulse-ring { width: 48px; height: 48px; margin: 0 auto 16px; border-radius: 50%; border: 3px solid rgba(255,51,102,0.2); border-top-color: $accent-pink; animation: spin 1s linear infinite; }
.float-anim { animation: float 3s ease-in-out infinite; }
.pulse-anim { animation: scale-pulse 2s ease-in-out infinite; }
.heartbeat-anim { animation: heartbeat 1.5s ease-in-out infinite; }

// --- 头部 ---
.moment-head { text-align: center; margin-bottom: 24px; }
.moment-eyebrow { margin: 0 0 8px; font-size: 11px; letter-spacing: 0.2em; color: $accent-blue; font-weight: 700; }
.moment-title { margin: 0 0 12px; font-size: 28px; font-weight: 800; color: $text-main; }
.moment-title__accent { @extend .text-gradient-warm; }
.moment-lead { margin: 0 auto 16px; font-size: 14px; line-height: 1.6; color: $text-sub; }
.moment-week-pill { display: inline-block; padding: 6px 16px; font-size: 12px; font-weight: 600; color: $text-main; }

// --- 倒计时 ---
.moment-countdown { margin-bottom: 20px; padding: 20px 16px; text-align: center; position: relative; overflow: hidden; }
// 倒计时内部微妙光晕
.moment-countdown::before {
  content: ''; position: absolute; top: -50%; left: 50%; width: 100%; height: 100%;
  background: radial-gradient(ellipse at center, rgba(255, 51, 102, 0.05), transparent 70%);
  transform: translateX(-50%); pointer-events: none;
}
.moment-countdown__title { margin: 0 0 12px; font-size: 13px; font-weight: 700; color: $text-sub; letter-spacing: 1px; }
.moment-countdown__done { margin: 0; font-size: 18px; }
.moment-countdown__wait { margin: 0; font-size: 14px; color: $text-sub; }
.moment-countdown__digits { display: flex; align-items: baseline; justify-content: center; gap: 8px; margin-bottom: 8px; }
.moment-countdown__unit {
  display: flex; flex-direction: column; align-items: center; min-width: 48px;
  strong { font-size: 32px; font-weight: 800; font-variant-numeric: tabular-nums; line-height: 1; @extend .text-gradient-warm; }
  small { font-size: 10px; color: #94a3b8; margin-top: 4px; font-weight: 700; letter-spacing: 1px; }
}
.moment-countdown__sep { width: 4px; }
.moment-countdown__colon { font-size: 24px; font-weight: 800; color: #cbd5e1; transform: translateY(-8px); }
.moment-countdown__hint { margin: 0; font-size: 12px; color: #94a3b8; }

// --- 阶段胶囊 (Tuner) ---
.tuner-capsule { display: flex; padding: 6px; gap: 4px; margin-bottom: 24px; }
.tuner-btn {
  flex: 1; padding: 10px 0; border-radius: 999px; text-align: center; font-size: 14px; font-weight: 600;
  color: $text-sub; transition: all 0.3s;
  &.active {
    color: $accent-pink; background: linear-gradient(135deg, rgba(79, 140, 255, 0.08), rgba(255, 51, 102, 0.08));
    box-shadow: 0 2px 8px rgba(255, 51, 102, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  }
}

// --- 核心状态卡片 ---
.moment-card { padding: 32px 24px; text-align: center; position: relative; overflow: hidden; }
.moment-panel__icon { font-size: 48px; margin-bottom: 12px; line-height: 1; }
.moment-panel__tag { margin: 0 0 8px; font-size: 13px; }
.moment-panel__h { margin: 0 0 12px; font-size: 22px; font-weight: 800; color: $text-main; }
.moment-panel__p { margin: 0; font-size: 15px; line-height: 1.6; color: $text-sub; }

.moment-mini-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin: 24px 0; }
.moment-mini-cell { padding: 12px 4px; text-align: center; }
.moment-mini-cell__n { display: block; font-size: 18px; font-weight: 800; color: $text-main; }
.moment-mini-cell__t { display: block; font-size: 11px; color: $text-sub; margin-top: 4px; font-weight: 600; }
.moment-footnote { margin-top: 16px; font-size: 12px; color: #94a3b8; }
.moment-footnote--warn { color: #f56c6c; }

// --- 等待中动画 (科技浪漫系引力轨道) ---
.orbit-container { position: relative; width: 160px; height: 160px; margin: 0 auto 32px; }
.center-icon { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 42px; text-shadow: 0 0 20px rgba(167, 139, 250, 0.5); }
.orbit {
  position: absolute; border-radius: 50%; border: 1px solid rgba(79, 140, 255, 0.2);
  animation: spin linear infinite;
  &-1 { inset: 20px; animation-duration: 4s; }
  &-2 { inset: 0px; border-color: rgba(255, 51, 102, 0.15); animation-duration: 7s; animation-direction: reverse; }
  &-3 { inset: -20px; border-color: rgba(255, 123, 84, 0.15); animation-duration: 12s; }
}
.orbit-dot {
  position: absolute; top: -6px; left: 50%; transform: translateX(-50%); width: 10px; height: 10px; border-radius: 50%;
  &.dot-pink { background: $accent-pink; box-shadow: 0 0 12px $accent-pink; }
  &.dot-blue { background: $accent-blue; box-shadow: 0 0 12px $accent-blue; width: 8px; height: 8px; top: -4px;}
  &.dot-orange { background: $accent-orange; box-shadow: 0 0 12px $accent-orange; width: 6px; height: 6px; top: -3px;}
}
.participant-badge { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; color: $text-sub; margin-top: 24px; }

// --- 纸屑特效 ---
.confetti-wrap { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.confetti { position: absolute; top: -10px; width: 6px; height: 12px; border-radius: 4px; opacity: 0; animation: confettiFall ease-in infinite; }

.encouragement { display: inline-flex; align-items: center; gap: 8px; padding: 12px 24px; font-size: 14px; color: $text-main; font-weight: 600; margin-top: 24px; }

// --- 底部信息区 ---
.info-sections-wrapper { display: flex; flex-direction: column; gap: 16px; margin-top: 24px; }
.moment-flow, .moment-shield { padding: 24px 20px; }
.moment-section-title { margin: 0 0 16px; font-size: 16px; font-weight: 800; color: $text-main; display: flex; align-items: center; gap: 6px;}

.moment-flow-list {
  margin: 0; padding: 0; list-style: none;
  li { display: flex; gap: 14px; margin-bottom: 20px; &:last-child { margin-bottom: 0; } }
}
.moment-flow-list__i {
  flex-shrink: 0; width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 800; color: #fff; background: linear-gradient(135deg, $accent-pink, $accent-orange);
  box-shadow: 0 4px 10px rgba(255, 51, 102, 0.3);
}
.moment-flow-list p { margin: 4px 0 0; font-size: 13px; line-height: 1.6; color: $text-sub; }
.moment-flow-list strong { font-size: 15px; color: $text-main; }
.moment-shield__p { margin: 0; font-size: 13px; line-height: 1.6; color: $text-sub; }

// --- Keyframes ---
@keyframes spin { 100% { transform: rotate(360deg); } }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
@keyframes scale-pulse { 0%, 100% { transform: translate(-50%, -50%) scale(1); } 50% { transform: translate(-50%, -50%) scale(1.1); } }
@keyframes heartbeat { 0% { transform: scale(1); } 15% { transform: scale(1.15); } 30% { transform: scale(1); } 45% { transform: scale(1.15); } 60%, 100% { transform: scale(1); } }
@keyframes confettiFall { 0% { transform: translateY(0) rotate(0deg); opacity: 1; } 100% { transform: translateY(500px) rotate(720deg); opacity: 0; } }

@media (max-width: 480px) {
  .moment-countdown__unit strong { font-size: 28px; }
  .moment-title { font-size: 24px; }
}
</style>