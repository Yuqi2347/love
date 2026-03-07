<template>
  <el-dialog
    v-model="visible"
    :show-close="false"
    width="460px"
    class="yuanfen-dialog"
    :close-on-click-modal="true"
    @close="$emit('close')"
  >
    <!-- 加载状态 -->
    <div v-if="loading" class="yf-loading">
      <div class="loading-rings">
        <div class="ring ring-1"></div>
        <div class="ring ring-2"></div>
        <div class="ring ring-3"></div>
        <span class="ring-icon">&#10024;</span>
      </div>
      <p class="loading-title">正在解析你们的缘分...</p>
      <p class="loading-sub">AI 正在综合多维度数据分析</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="yf-error">
      <div class="error-face">&#128533;</div>
      <p class="error-msg">{{ error }}</p>
      <button class="yf-btn yf-btn-retry" @click="fetchAnalysis">重新尝试</button>
    </div>

    <!-- 结果展示 -->
    <div v-else-if="result" class="yf-result">
      <!-- 顶部缘分指数 -->
      <div class="yf-header">
        <div class="yf-spark">&#10024;</div>
        <h2 class="yf-index">{{ result.yuanFenIndex }}</h2>
        <p class="yf-pair">{{ currentNickname }} & {{ targetNickname }}</p>
      </div>

      <div class="yf-divider"></div>

      <!-- 总体评价 -->
      <div v-if="result.overallInterpretation" class="yf-section">
        <div class="yf-section-head">
          <span class="yf-section-icon">&#128172;</span>
          <span class="yf-section-title">总体评价</span>
        </div>
        <p class="yf-section-body">{{ result.overallInterpretation }}</p>
      </div>

      <!-- 性格契合/互动 -->
      <div v-if="personalityContent" class="yf-section">
        <div class="yf-section-head">
          <span class="yf-section-icon">&#129504;</span>
          <span class="yf-section-title">性格契合</span>
        </div>
        <p class="yf-section-body">{{ personalityContent }}</p>
      </div>

      <!-- 兴趣互动 -->
      <div v-if="result.interestChemistry" class="yf-section">
        <div class="yf-section-head">
          <span class="yf-section-icon">&#127925;</span>
          <span class="yf-section-title">兴趣互动</span>
        </div>
        <p class="yf-section-body">{{ result.interestChemistry }}</p>
      </div>

      <!-- 校园场景/时刻 -->
      <div v-if="campusContent" class="yf-section">
        <div class="yf-section-head">
          <span class="yf-section-icon">&#127979;</span>
          <span class="yf-section-title">校园场景</span>
        </div>
        <p class="yf-section-body">{{ campusContent }}</p>
      </div>

      <!-- 推荐活动 -->
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

      <!-- 温馨提示 -->
      <div class="yf-section yf-section-tip">
        <div class="yf-section-head">
          <span class="yf-section-icon">&#128161;</span>
          <span class="yf-section-title">温馨提示</span>
        </div>
        <p class="yf-section-body">{{ result.potentialChallenge }}</p>
      </div>

      <!-- 发展可能/关系潜力 -->
      <div v-if="developmentContent" class="yf-section">
        <div class="yf-section-head">
          <span class="yf-section-icon">&#127793;</span>
          <span class="yf-section-title">发展可能</span>
        </div>
        <p class="yf-section-body">{{ developmentContent }}</p>
      </div>

      <!-- 缘分金句 -->
      <div class="yf-quote" @click="copyQuote">
        <p class="yf-quote-text">「{{ result.exclusiveQuote }}」</p>
        <span class="yf-quote-hint">{{ copied ? '已复制 &#10003;' : '点击复制金句' }}</span>
      </div>

      <!-- 关闭按钮 -->
      <button class="yf-btn yf-btn-close" @click="$emit('close')">关闭</button>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getYuanFenAnalysis, type YuanFenAnalysisResult } from '@/api/aiApi'

const props = defineProps<{
  modelValue: boolean
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

// 兼容异性/同性不同字段名
const personalityContent = computed(() => result.value?.personalityAnalysis || result.value?.personalityInteraction)
const campusContent = computed(() => result.value?.campusStoryScene || result.value?.campusMoment)
const developmentContent = computed(() => result.value?.developmentPotential || result.value?.relationshipPotential)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && !result.value) {
    fetchAnalysis()
  }
}, { immediate: true })

async function fetchAnalysis() {
  loading.value = true
  error.value = ''
  try {
    const res = await getYuanFenAnalysis(props.targetUserId)
    result.value = res.data.data

    // 通知父组件冷却时间
    if (result.value?.nextAvailableAt) {
      const next = new Date(result.value.nextAvailableAt).getTime()
      const remaining = Math.max(0, Math.floor((next - Date.now()) / 1000))
      emit('analyzed', remaining)
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.response?.data?.msg || e?.message || 'AI 分析失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function copyQuote() {
  if (!result.value?.exclusiveQuote) return
  try {
    await navigator.clipboard.writeText(result.value.exclusiveQuote)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // fallback
  }
}
</script>

<style lang="scss" scoped>
/* ==================== Loading ==================== */
.yf-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 24px 48px;
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
  margin-bottom: 6px;
}

.loading-sub {
  font-size: 13px;
  color: $text-muted;
}

/* ==================== Error ==================== */
.yf-error {
  text-align: center;
  padding: 48px 24px;
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
  padding: 4px 0;
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

/* ==================== Sections ==================== */
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

/* ==================== Activities ==================== */
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

/* ==================== Quote ==================== */
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

/* ==================== Buttons ==================== */
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
</style>

<style lang="scss">
/* el-dialog overrides (non-scoped) */
.yuanfen-dialog {
  .el-dialog__header { display: none; }
  .el-dialog__body { padding: 24px 28px; }
}
.yuanfen-dialog.el-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
</style>
