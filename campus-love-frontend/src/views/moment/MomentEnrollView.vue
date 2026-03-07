<template>
  <div class="enroll-page">
    <!-- Progress Bar -->
    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: `${((currentStep) / 3) * 100}%` }" />
    </div>
    <div class="step-indicator">
      <span
        v-for="s in 3" :key="s"
        class="step-dot"
        :class="{ active: s <= currentStep, current: s === currentStep }"
      />
    </div>

    <!-- Step 1: 关于你 -->
    <div v-if="currentStep === 1" class="step-section">
      <h2 class="step-title">关于你自己</h2>
      <p class="step-desc">先让我们了解一下你</p>

      <!-- 照片上传（可选） -->
      <div class="photo-upload-section">
        <div class="section-label">
          上传一张真实照片
          <span class="optional-tag">可选</span>
        </div>
        <div class="photo-area" @click="triggerPhotoUpload">
          <input ref="photoInputRef" type="file" accept="image/*" hidden @change="handlePhotoSelect" />
          <template v-if="photoPreview">
            <img :src="photoPreview" class="photo-preview" />
            <button class="photo-remove" @click.stop="removePhoto">
              <el-icon><Close /></el-icon>
            </button>
          </template>
          <template v-else>
            <div class="photo-placeholder">
              <el-icon :size="32"><Camera /></el-icon>
              <span>点击上传照片</span>
            </div>
          </template>
        </div>
        <p class="photo-hint">{{ existingPhotoUrl ? '已保留上次上传的照片，可点击重新选择' : '照片仅用于活动内部，不会公开展示' }}</p>
      </div>

      <!-- 自评颜值 -->
      <div class="slider-section">
        <div class="section-label">你认为自己的颜值大概是？</div>
        <div class="slider-wrap">
          <span class="slider-min">1</span>
          <el-slider
            v-model="form.selfScore"
            :min="1" :max="10" :step="1"
            :marks="selfScoreMarks"
            show-stops
          />
          <span class="slider-max">10</span>
        </div>
        <p class="slider-hint">诚实填写效果最好，系统会综合参考 😊</p>
      </div>

      <!-- Q3-Q5 -->
      <QuestionCard
        v-for="q in STEP1_QUESTIONS" :key="q.key"
        :question="q"
        :modelValue="(form as any)[q.key]"
        @update:modelValue="(v: string) => (form as any)[q.key] = v"
      />
    </div>

    <!-- Step 2: 关于TA -->
    <div v-if="currentStep === 2" class="step-section">
      <h2 class="step-title">关于你期待的 TA</h2>
      <p class="step-desc">描述一下你心目中的另一半</p>

      <!-- 目标性别 -->
      <div class="question-block">
        <div class="question-title">你想匹配的对象是？</div>
        <div class="option-grid">
          <div
            v-for="opt in TARGET_GENDER_OPTIONS" :key="opt.value"
            class="option-card"
            :class="{ selected: form.targetGender === opt.value }"
            @click="form.targetGender = opt.value"
          >
            <span class="option-emoji">{{ opt.emoji }}</span>
            <span class="option-label">{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <!-- Q6-Q11 -->
      <QuestionCard
        v-for="q in STEP2_QUESTIONS" :key="q.key"
        :question="q"
        :modelValue="(form as any)[q.key]"
        @update:modelValue="(v: string) => (form as any)[q.key] = v"
      >
        <!-- Q6 特殊提示 -->
        <template v-if="q.key === 'appearanceRequirement' && form.appearanceRequirement === 'A'" #hint>
          <div class="appearance-warning">
            <el-icon><WarningFilled /></el-icon>
            <span>选择此项可能增加匹配难度，因为对颜值要求越高，符合条件的匹配对象越少</span>
          </div>
        </template>
      </QuestionCard>
    </div>

    <!-- Step 3: 价值观 -->
    <div v-if="currentStep === 3" class="step-section">
      <h2 class="step-title">关于价值观</h2>
      <p class="step-desc">最后几个问题，帮助找到三观一致的人</p>

      <QuestionCard
        v-for="q in STEP3_QUESTIONS" :key="q.key"
        :question="q"
        :modelValue="(form as any)[q.key]"
        @update:modelValue="(v: string) => (form as any)[q.key] = v"
      />

      <!-- 确认提交区 -->
      <div class="confirm-section">
        <div class="confirm-check">
          <el-checkbox v-model="agreed">我已确认以上信息真实有效</el-checkbox>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <button v-if="currentStep > 1" class="btn-back" @click="prevStep">
        上一步
      </button>
      <button v-else class="btn-back" @click="$router.back()">
        取消
      </button>
      <button
        v-if="currentStep < 3"
        class="btn-next"
        :disabled="!canProceed"
        @click="nextStep"
      >
        下一步
      </button>
      <button
        v-else
        class="btn-submit"
        :disabled="!canSubmit || submitting"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '确认提交' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { enrollMoment, getMomentProfile, uploadMomentPhoto } from '@/api/momentApi'
import type { MomentEnrollRequest } from '@/api/momentApi'
import {
  STEP1_QUESTIONS, STEP2_QUESTIONS, STEP3_QUESTIONS,
  TARGET_GENDER_OPTIONS,
} from '@/constants/momentConst'
import QuestionCard from './components/QuestionCard.vue'

const router = useRouter()
const currentStep = ref(1)
const submitting = ref(false)
const agreed = ref(false)
const photoFile = ref<File | null>(null)
const photoPreview = ref<string>('')
const photoInputRef = ref<HTMLInputElement>()
const existingPhotoUrl = ref<string>('')  // 已有照片URL（来自上次上传）

const form = reactive<MomentEnrollRequest>({
  selfScore: 5,
  targetGender: '',
  socialStyle: '',
  lifeRhythm: '',
  companionshipStyle: '',
  appearanceRequirement: '',
  partnerPersonality: '',
  majorPreference: '',
  ageRangePreference: '',
  dateStyle: '',
  intimacyPace: '',
  loyaltyValue: '',
  premaritalCohabitation: '',
  futureLifestyle: '',
  relationshipCoreValue: '',
})

const selfScoreMarks: Record<number, string> = { 1: '1', 5: '5', 10: '10' }

// 步骤校验
const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return form.socialStyle && form.lifeRhythm && form.companionshipStyle
  }
  if (currentStep.value === 2) {
    return form.targetGender && form.appearanceRequirement &&
           form.partnerPersonality && form.majorPreference &&
           form.ageRangePreference && form.dateStyle && form.intimacyPace
  }
  return true
})

const canSubmit = computed(() => {
  return canProceed.value && agreed.value &&
         form.loyaltyValue && form.premaritalCohabitation &&
         form.futureLifestyle && form.relationshipCoreValue
})

function nextStep() {
  if (canProceed.value && currentStep.value < 3) {
    currentStep.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function prevStep() {
  if (currentStep.value > 1) {
    currentStep.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 照片处理
function triggerPhotoUpload() {
  photoInputRef.value?.click()
}

function handlePhotoSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }
  photoFile.value = file
  photoPreview.value = URL.createObjectURL(file)
  target.value = ''
}

function removePhoto() {
  photoFile.value = null
  photoPreview.value = ''
  existingPhotoUrl.value = ''
}

// 提交
async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true

  try {
    // 先上传照片（仅在用户选择了新文件时）
    if (photoFile.value) {
      try {
        await uploadMomentPhoto(photoFile.value)
      } catch {
        ElMessage.warning('照片上传失败，但不影响报名')
      }
    }

    // 提交问卷
    await enrollMoment(form)
    ElMessage.success('报名成功！')
    router.replace('/moment')
  } catch (err: any) {
    const msg = err?.response?.data?.message || '报名失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

// 加载已有问卷（回填）
onMounted(async () => {
  try {
    const res = await getMomentProfile()
    const profile = res.data.data
    if (profile) {
      form.targetGender = profile.targetGender || ''
      form.socialStyle = profile.socialStyle || ''
      form.lifeRhythm = profile.lifeRhythm || ''
      form.companionshipStyle = profile.companionshipStyle || ''
      form.appearanceRequirement = profile.appearanceRequirement || ''
      form.partnerPersonality = profile.partnerPersonality || ''
      form.majorPreference = profile.majorPreference || ''
      form.ageRangePreference = profile.ageRangePreference || ''
      form.dateStyle = profile.dateStyle || ''
      form.intimacyPace = profile.intimacyPace || ''
      form.loyaltyValue = profile.loyaltyValue || ''
      form.premaritalCohabitation = profile.premaritalCohabitation || ''
      form.futureLifestyle = profile.futureLifestyle || ''
      form.relationshipCoreValue = profile.relationshipCoreValue || ''
      // 回填自评分和照片
      if (profile.momentSelfScore) {
        form.selfScore = profile.momentSelfScore
      }
      if (profile.momentPhotoUrl) {
        photoPreview.value = '/api' + profile.momentPhotoUrl
        existingPhotoUrl.value = profile.momentPhotoUrl
      }
    }
  } catch {
    // 无已有问卷，正常流程
  }
})
</script>

<style lang="scss" scoped>
.enroll-page {
  max-width: $max-content-width;
  margin: 0 auto;
  padding: 24px 20px 100px;
}

// ==================== 进度条 ====================
.progress-bar {
  height: 4px;
  background: $bg-tertiary;
  border-radius: 2px;
  margin-bottom: 12px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: $primary-gradient;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.step-indicator {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 32px;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $border-color;
  transition: all $transition-base;

  &.active { background: $primary; }
  &.current { width: 24px; border-radius: 4px; }
}

// ==================== 步骤标题 ====================
.step-title {
  font-size: 24px;
  font-weight: 800;
  color: $text-primary;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 14px;
  color: $text-muted;
  margin-bottom: 28px;
}

// ==================== 照片上传 ====================
.photo-upload-section {
  margin-bottom: 28px;
}

.section-label {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.optional-tag {
  font-size: 12px;
  font-weight: 500;
  color: $text-muted;
  padding: 2px 8px;
  background: $bg-tertiary;
  border-radius: $radius-full;
}

.photo-area {
  width: 160px;
  height: 160px;
  border-radius: $radius-lg;
  border: 2px dashed $border-color;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: all $transition-base;

  &:hover {
    border-color: $primary;
    background: rgba($primary, 0.03);
  }
}

.photo-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: $text-muted;
  font-size: 13px;
}

.photo-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-hint {
  margin-top: 8px;
  font-size: 12px;
  color: $text-muted;
}

// ==================== 自评滑块 ====================
.slider-section {
  margin-bottom: 28px;
}

.slider-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 4px;

  .el-slider { flex: 1; }
  .slider-min, .slider-max {
    font-size: 14px;
    font-weight: 600;
    color: $text-muted;
    flex-shrink: 0;
  }
}

.slider-hint {
  margin-top: 8px;
  font-size: 12px;
  color: $text-muted;
}

// ==================== 目标性别选择 ====================
.question-block {
  margin-bottom: 24px;
}

.question-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 12px;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.option-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 12px;
  background: $bg-secondary;
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    background: rgba($primary, 0.05);
    border-color: $primary-light;
  }

  &.selected {
    background: rgba($primary, 0.08);
    border-color: $primary;
  }

  .option-emoji { font-size: 28px; }
  .option-label { font-size: 14px; font-weight: 500; color: $text-primary; }
}

// ==================== Q6 颜值提示 ====================
.appearance-warning {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding: 10px 14px;
  background: rgba($warning, 0.1);
  border-left: 3px solid $warning;
  border-radius: 0 $radius-sm $radius-sm 0;
  font-size: 13px;
  color: #8a6d3b;
  line-height: 1.5;

  .el-icon {
    color: $warning;
    flex-shrink: 0;
    margin-top: 2px;
  }
}

// ==================== 确认提交 ====================
.confirm-section {
  margin-top: 24px;
  padding: 20px;
  background: $bg-secondary;
  border-radius: $radius-lg;
}

.confirm-check {
  :deep(.el-checkbox__label) {
    font-size: 14px;
    color: $text-secondary;
  }
}

// ==================== 底部导航 ====================
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: rgba($bg-primary, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid $border-light;
  max-width: $max-content-width;
  margin: 0 auto;
  z-index: 10;
}

.btn-back {
  flex: 1;
  height: 48px;
  background: $bg-tertiary;
  color: $text-secondary;
  border: none;
  border-radius: $radius-full;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-base;

  &:hover { background: $border-color; }
}

.btn-next, .btn-submit {
  flex: 2;
  height: 48px;
  background: $primary-gradient;
  color: white;
  border: none;
  border-radius: $radius-full;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all $transition-base;

  &:hover:not(:disabled) {
    box-shadow: 0 4px 16px rgba($primary, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>
