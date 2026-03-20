<template>
  <el-dialog
    v-model="visible"
    :show-close="false"
    width="460px"
    class="yuanfen-dialog"
    :close-on-click-modal="true"
    align-center
    @close="$emit('close')"
  >
    <div class="yf-sheet-root">
      <!-- 固定顶栏：手机端可随时关闭，避免全屏无出口 -->
      <header class="yf-sheet-header">
        <button type="button" class="yf-header-btn" aria-label="关闭" @click="$emit('close')">
          <span class="yf-header-icon">&#8592;</span>
          <span class="yf-header-text">返回</span>
        </button>
        <span class="yf-header-title">缘分分析</span>
        <span class="yf-header-spacer" aria-hidden="true"></span>
      </header>

      <div class="yf-sheet-scroll">
        <!-- 加载状态 -->
        <div v-if="loading" class="yf-loading">
          <div class="loading-rings">
            <div class="ring ring-1"></div>
            <div class="ring ring-2"></div>
            <div class="ring ring-3"></div>
            <span class="ring-icon">&#10024;</span>
          </div>
          <p class="loading-title">正在解析你们的缘分...</p>
          <div class="yf-wait-timer" aria-live="polite">
            <span class="yf-wait-seconds">已等待 {{ loadingElapsedSec }} 秒</span>
            <span class="yf-wait-cap">单次请求最长约 {{ YUANFEN_REQUEST_TIMEOUT_SEC }} 秒</span>
          </div>
          <div class="yf-wait-bar" role="progressbar" :aria-valuenow="waitBarPercent" aria-valuemin="0" aria-valuemax="100">
            <div class="yf-wait-bar-fill" :style="{ width: waitBarPercent + '%' }"></div>
          </div>
          <button type="button" class="yf-btn yf-btn-cancel-load" @click="$emit('close')">关闭</button>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="error" class="yf-error">
          <div class="error-face">&#128533;</div>
          <p class="error-msg">{{ error }}</p>
          <button type="button" class="yf-btn yf-btn-retry" @click="() => fetchAnalysis(true)">重新尝试</button>
        </div>

        <!-- 结果展示 -->
        <div v-else-if="result" class="yf-result">
          <div class="yf-header">
            <div class="yf-spark">&#10024;</div>
            <h2 class="yf-index">{{ result.yuanFenIndex }}</h2>
            <p class="yf-pair">{{ currentNickname }} & {{ targetNickname }}</p>
          </div>

          <div class="yf-divider"></div>

          <div v-if="result.overallInterpretation" class="yf-section">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#128172;</span>
              <span class="yf-section-title">总体评价</span>
            </div>
            <p class="yf-section-body">{{ result.overallInterpretation }}</p>
          </div>

          <div v-if="personalityContent" class="yf-section">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#129504;</span>
              <span class="yf-section-title">性格契合</span>
            </div>
            <p class="yf-section-body">{{ personalityContent }}</p>
          </div>

          <div v-if="result.interestChemistry" class="yf-section">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#127925;</span>
              <span class="yf-section-title">兴趣互动</span>
            </div>
            <p class="yf-section-body">{{ result.interestChemistry }}</p>
          </div>

          <div v-if="campusContent" class="yf-section">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#127979;</span>
              <span class="yf-section-title">校园场景</span>
            </div>
            <p class="yf-section-body">{{ campusContent }}</p>
          </div>

          <div class="yf-section">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#127919;</span>
              <span class="yf-section-title">推荐一起做</span>
            </div>
            <div class="yf-activities">
              <div v-for="(act, i) in (result.recommendActivities || [])" :key="i" class="yf-activity">
                <span class="yf-activity-dot">{{ i + 1 }}</span>
                <span class="yf-activity-text">{{ act }}</span>
              </div>
            </div>
          </div>

          <div class="yf-section yf-section-tip">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#128161;</span>
              <span class="yf-section-title">温馨提示</span>
            </div>
            <p class="yf-section-body">{{ result.potentialChallenge }}</p>
          </div>

          <div v-if="developmentContent" class="yf-section">
            <div class="yf-section-head">
              <span class="yf-section-icon">&#127793;</span>
              <span class="yf-section-title">发展可能</span>
            </div>
            <p class="yf-section-body">{{ developmentContent }}</p>
          </div>

          <div class="yf-quote" @click="copyQuote">
            <p class="yf-quote-text">「{{ result.exclusiveQuote }}」</p>
            <span class="yf-quote-hint">{{ copied ? '已复制 &#10003;' : '点击复制金句' }}</span>
          </div>

          <button type="button" class="yf-btn yf-btn-close" @click="$emit('close')">关闭</button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getYuanFenAnalysis,
  YUANFEN_REQUEST_TIMEOUT_SEC,
  type YuanFenAnalysisResult,
} from '@/api/aiApi'

/** 与后端冷却窗同量级：仅作前端秒开展示，权威数据仍以接口为准 */
const YF_LOCAL_TTL_MS = 72 * 60 * 60 * 1000
const YF_LS_PREFIX = 'campusLove:yf:v1:'

const props = defineProps<{
  modelValue: boolean
  /** 当前登录用户 id，用于本地缓存 key（刷新/重开页仍可先展示上次结果） */
  viewerUserId: number
  targetUserId: number
  currentNickname: string
  targetNickname: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'analyzed', cooldownSeconds: number): void
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const error = ref('')
const result = ref<YuanFenAnalysisResult | null>(null)
const copied = ref(false)
/** 加载中已等待秒数（正计时，非精确倒计时） */
const loadingElapsedSec = ref(0)
let loadingTickTimer: ReturnType<typeof setInterval> | null = null

function clearLoadingTick() {
  if (loadingTickTimer) {
    clearInterval(loadingTickTimer)
    loadingTickTimer = null
  }
}

/** 进度条：随已等待时间缓慢增长，最多约 93%，避免「跑满却未完成」的错觉 */
const waitBarPercent = computed(() => {
  if (!loading.value) return 0
  const p = (loadingElapsedSec.value / YUANFEN_REQUEST_TIMEOUT_SEC) * 100
  return Math.min(93, Math.round(p))
})

function yfStorageKey(): string | null {
  const v = props.viewerUserId
  const t = props.targetUserId
  if (!Number.isFinite(v) || v <= 0 || !Number.isFinite(t) || t <= 0) return null
  return `${YF_LS_PREFIX}${v}:${t}`
}

function tryHydrateFromLocalStorage(): boolean {
  const key = yfStorageKey()
  if (!key) return false
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return false
    const wrap = JSON.parse(raw) as { at: number; data: YuanFenAnalysisResult }
    if (!wrap?.data || typeof wrap.at !== 'number') return false
    if (Date.now() - wrap.at > YF_LOCAL_TTL_MS) {
      localStorage.removeItem(key)
      return false
    }
    result.value = wrap.data
    return true
  } catch {
    return false
  }
}

function saveYuanfenToLocalStorage(data: YuanFenAnalysisResult) {
  const key = yfStorageKey()
  if (!key) return
  try {
    localStorage.setItem(key, JSON.stringify({ at: Date.now(), data }))
  } catch {
    /* 存储配额等 */
  }
}

onUnmounted(() => {
  clearLoadingTick()
})

const personalityContent = computed(() => result.value?.personalityAnalysis || result.value?.personalityInteraction)
const campusContent = computed(() => result.value?.campusStoryScene || result.value?.campusMoment)
const developmentContent = computed(() => result.value?.developmentPotential || result.value?.relationshipPotential)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (!val) return
  const uid = props.targetUserId
  if (!Number.isFinite(uid) || uid <= 0) {
    error.value = '用户信息无效，请刷新后重试'
    loading.value = false
    return
  }
  // 同一会话内已有内存结果：不调接口
  if (result.value) return
  // 刷新/重进页面后组件重建：先从本地恢复，再静默请求后端对齐（命中缓存时很快）
  if (tryHydrateFromLocalStorage()) {
    void fetchAnalysis(false, { silent: true })
    return
  }
  void fetchAnalysis(false)
}, { immediate: true })

watch(() => props.targetUserId, (uid, prev) => {
  if (prev === undefined) return
  if (uid !== prev) {
    result.value = null
    error.value = ''
    copied.value = false
  }
})

async function fetchAnalysis(force: boolean, opts?: { silent?: boolean }) {
  const uid = props.targetUserId
  if (!Number.isFinite(uid) || uid <= 0) return
  if (force) {
    result.value = null
    error.value = ''
    const k = yfStorageKey()
    if (k) localStorage.removeItem(k)
  }
  if (!force && result.value && !opts?.silent) return

  const silent = Boolean(opts?.silent && !force && result.value)

  clearLoadingTick()
  if (!silent) {
    loadingElapsedSec.value = 0
    loading.value = true
    error.value = ''
    loadingTickTimer = setInterval(() => {
      loadingElapsedSec.value++
    }, 1000)
  } else {
    error.value = ''
  }
  try {
    const res = await getYuanFenAnalysis(uid)
    const data = res.data?.data ?? res.data
    if (!data || typeof data !== 'object') {
      if (!silent) error.value = '分析结果格式异常，请稍后重试'
      return
    }
    result.value = data
    saveYuanfenToLocalStorage(data)

    if (data.fromCache && !silent) {
      ElMessage.success({ message: '已加载历史解析（未重新调用 AI）', duration: 2200 })
    }

    if (result.value?.nextAvailableAt) {
      const next = new Date(result.value.nextAvailableAt).getTime()
      const remaining = Math.max(0, Math.floor((next - Date.now()) / 1000))
      emit('analyzed', remaining)
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string; msg?: string } }; message?: string }
    const msg = err?.response?.data?.message || err?.response?.data?.msg || err?.message || 'AI 分析失败，请稍后重试'
    if (silent) {
      // 静默同步失败时保留本地已展示内容，不打断用户
    } else {
      error.value = msg
    }
  } finally {
    clearLoadingTick()
    if (!silent) loading.value = false
  }
}

async function copyQuote() {
  const text = result.value?.exclusiveQuote
  if (!text) return
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
    } catch {
      ElMessage.error('复制失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.yf-sheet-root {
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: inherit;
}

.yf-sheet-header {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: minmax(80px, 1fr) auto minmax(80px, 1fr);
  align-items: center;
  gap: 8px;
  padding: 10px 16px 12px;
  border-bottom: 1px solid $border-light;
  margin-bottom: 0;
}

.yf-header-btn {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  margin: 0;
  border: none;
  border-radius: $radius-full;
  background: rgba($primary, 0.08);
  color: $primary-dark;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background $transition-fast;

  &:active {
    opacity: 0.85;
  }
}

.yf-header-icon {
  font-size: 16px;
  line-height: 1;
}

.yf-header-text {
  line-height: 1;
}

.yf-header-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
  text-align: center;
  justify-self: center;
}

.yf-header-spacer {
  justify-self: end;
  width: 1px;
  height: 1px;
  visibility: hidden;
}

.yf-sheet-scroll {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  padding: 8px 24px 24px;
}

/* ==================== Loading ==================== */
.yf-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px 36px;
}

.loading-rings {
  position: relative;
  width: 72px;
  height: 72px;
  margin-bottom: 24px;
}

.ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid transparent;
}

.ring-1 {
  border-top-color: $primary;
  animation: spin 1.2s linear infinite;
}

.ring-2 {
  inset: 6px;
  border-right-color: $primary-dark;
  animation: spin 1.6s linear infinite reverse;
}

.ring-3 {
  inset: 12px;
  border-bottom-color: $primary-light;
  animation: spin 2s linear infinite;
}

.ring-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 22px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; transform: translate(-50%, -50%) scale(0.9); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
}

.loading-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 10px;
}

.yf-wait-timer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
}

.yf-wait-seconds {
  font-size: 28px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: $primary-dark;
  letter-spacing: -0.02em;
}

.yf-wait-cap {
  font-size: 12px;
  color: $text-muted;
}

.yf-wait-bar {
  width: 100%;
  max-width: 260px;
  height: 6px;
  border-radius: 3px;
  background: rgba($primary, 0.12);
  overflow: hidden;
  margin-bottom: 16px;
}

.yf-wait-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: $primary-gradient;
  transition: width 0.35s ease;
}

.yf-btn-cancel-load {
  margin-top: 4px;
  width: auto;
  min-width: 120px;
  padding: 10px 28px;
  background: $bg-tertiary;
  color: $text-secondary;
  border-radius: $radius-full;
  font-weight: 600;
  font-size: 14px;
  border: none;
  cursor: pointer;
}

/* ==================== Error ==================== */
.yf-error {
  text-align: center;
  padding: 40px 20px;
}

.error-face {
  font-size: 52px;
  margin-bottom: 16px;
}

.error-msg {
  font-size: 15px;
  color: $text-secondary;
  margin-bottom: 20px;
  line-height: 1.6;
}

/* ==================== Result ==================== */
.yf-result {
  padding: 4px 0 8px;
}

.yf-header {
  text-align: center;
  padding-bottom: 4px;
}

.yf-spark {
  font-size: 32px;
  margin-bottom: 2px;
}

.yf-index {
  font-size: 26px;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.yf-pair {
  font-size: 14px;
  color: $text-muted;
}

.yf-divider {
  height: 1px;
  background: $border-light;
  margin: 16px 0;
}

.yf-section {
  margin-bottom: 14px;

  &.yf-section-tip {
    background: rgba($accent, 0.07);
    border-radius: $radius-md;
    padding: 12px 14px;
    border-left: 3px solid $accent;
  }
}

.yf-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.yf-section-icon {
  font-size: 17px;
  flex-shrink: 0;
}

.yf-section-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}

.yf-section-body {
  font-size: 14px;
  line-height: 1.75;
  color: $text-secondary;
  padding-left: 25px;
}

.yf-activities {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 25px;
}

.yf-activity {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: $text-secondary;
}

.yf-activity-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: $primary-gradient;
  color: white;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.yf-activity-text {
  line-height: 1.5;
}

.yf-quote {
  margin: 18px 0;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary-dark, 0.1));
  border-radius: $radius-md;
  text-align: center;
  cursor: pointer;
  transition: transform $transition-fast, box-shadow $transition-fast;
  border: 1px solid rgba($primary, 0.1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba($primary, 0.12);
  }

  &:active {
    transform: scale(0.99);
  }
}

.yf-quote-text {
  font-size: 16px;
  font-weight: 700;
  color: $primary-dark;
  line-height: 1.7;
  margin-bottom: 6px;
}

.yf-quote-hint {
  font-size: 12px;
  color: $text-muted;
}

.yf-btn {
  display: block;
  width: 100%;
  padding: 12px;
  border-radius: $radius-full;
  font-weight: 600;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: all $transition-fast;
}

.yf-btn-retry {
  width: auto;
  display: inline-block;
  padding: 10px 32px;
  background: $primary-gradient;
  color: white;

  &:hover { opacity: 0.9; transform: translateY(-1px); }
}

.yf-btn-close {
  background: $bg-tertiary;
  color: $text-secondary;
  margin-top: 4px;

  &:hover { background: $border-color; }
}

@media (max-width: $bp-mobile) {
  .yf-loading {
    padding: 28px 14px 28px;
  }

  .yf-error {
    padding: 28px 14px;
  }

  .yf-spark {
    font-size: 26px;
  }

  .yf-index {
    font-size: 22px;
  }

  .yf-pair {
    font-size: 12px;
    padding: 0 4px;
    line-height: 1.35;
    word-break: break-all;
  }

  .yf-divider {
    margin: 12px 0;
  }

  .yf-section {
    margin-bottom: 12px;

    &.yf-section-tip {
      padding: 10px 12px;
    }
  }

  .yf-section-title {
    font-size: 14px;
  }

  .yf-section-body {
    font-size: 13px;
    line-height: 1.65;
    padding-left: 22px;
  }

  .yf-activities {
    padding-left: 22px;
  }

  .yf-activity {
    font-size: 13px;
  }

  .yf-quote {
    margin: 14px 0;
    padding: 12px 14px;
  }

  .yf-quote-text {
    font-size: 14px;
  }

  .yf-sheet-scroll {
    padding: 8px 14px calc(14px + env(safe-area-inset-bottom, 0px));
  }

  .yf-sheet-header {
    padding-left: 14px;
    padding-right: 14px;
    padding-top: calc(10px + env(safe-area-inset-top, 0px));
  }
}
</style>

<style lang="scss">
.yuanfen-dialog {
  .el-dialog__header {
    display: none;
  }
  .el-dialog__body {
    padding: 0 !important;
  }
}

.yuanfen-dialog.el-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  display: flex !important;
  flex-direction: column !important;
  max-height: 90vh !important;
}

.yuanfen-dialog .el-dialog__body {
  flex: 1 1 auto;
  min-height: 0;
  max-height: inherit;
  display: flex;
  flex-direction: column;
}

@media (max-width: $bp-mobile) {
  .yuanfen-dialog.el-dialog {
    width: min(100vw - 24px, 440px) !important;
    max-width: calc(100vw - 24px) !important;
    max-height: min(88dvh, 720px) !important;
    margin: 6dvh auto auto !important;
    top: 0 !important;
    transform: none !important;
    border-radius: 18px !important;
    box-sizing: border-box;
  }

  .yuanfen-dialog .yf-sheet-scroll {
    max-height: calc(min(88dvh, 720px) - 56px);
  }
}
</style>
