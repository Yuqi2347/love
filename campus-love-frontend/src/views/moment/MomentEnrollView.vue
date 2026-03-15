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

      <!-- Step 1 题目 -->
      <QuestionCard
        v-for="q in STEP1_QUESTIONS" :key="q.key"
        :question="q"
        :model-value="(form as any)[q.key]"
        @update:model-value="(v: string) => (form as any)[q.key] = v"
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
            @click="form.targetGender = normalizeTargetGender(opt.value)"
          >
            <span class="option-emoji">{{ opt.emoji }}</span>
            <span class="option-label">{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <!-- 年龄接受范围（滑块） -->
      <div class="age-range-section">
        <div class="section-label">年龄方面，你可以接受的范围？</div>
        <div class="age-no-matter">
          <el-checkbox v-model="ageNoMatter" @change="onAgeNoMatterChange">
            年龄不是问题，合适最重要
          </el-checkbox>
        </div>
        <div v-if="!ageNoMatter" class="slider-wrap">
          <span class="slider-min">比我小 10 岁</span>
          <el-slider
            v-model="ageRange"
            range
            :min="-10"
            :max="10"
            :step="1"
            :marks="ageRangeMarks"
            show-stops
          />
          <span class="slider-max">比我大 10 岁</span>
        </div>
        <p v-if="!ageNoMatter" class="slider-hint">
          当前接受：{{ ageRangeLabel }}
        </p>
      </div>

      <!-- Step 2 题目 -->
      <QuestionCard
        v-for="q in STEP2_QUESTIONS" :key="q.key"
        :question="q"
        :model-value="(form as any)[q.key]"
        @update:model-value="(v: string) => (form as any)[q.key] = v"
      >
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
        :model-value="(form as any)[q.key]"
        @update:model-value="(v: string) => (form as any)[q.key] = v"
      />

      <!-- 确认提交区 -->
      <div class="confirm-section">
        <div class="priority-card">
          <div class="priority-title">优先匹配</div>
          <el-checkbox v-model="form.prioritizeMatching">
            我接受适当降低匹配度，优先达成匹配
          </el-checkbox>
          <p class="priority-hint">勾选后会适度降低你的匹配阈值，用来提高进入候选边的机会。</p>
        </div>
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
import { ref, computed, onMounted, reactive, watch } from 'vue'
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
const existingPhotoUrl = ref<string>('')

const form = reactive<MomentEnrollRequest>({
  selfScore: 5,
  targetGender: 'any',
  prioritizeMatching: false,
  socialStyle: '',
  lifeRhythm: '',
  personalityBase: '',
  campusFocus: '',
  emotionStyle: '',
  companionshipStyle: '',
  appearanceRequirement: '',
  agePreferenceMin: -2,
  agePreferenceMax: 2,
  gradeRangePreference: '',
  partnerPersonality: '',
  majorPreference: '',
  careerAmbitionPref: '',
  dateStyle: '',
  intimacyPace: '',
  honestyLevel: '',
  premaritalCohabitation: '',
  premaritalSex: '',
  relationshipCoreValue: '',
  conflictStyle: '',
  socialBoundary: '',
  futureLifestyle: '',
  campusLovePlan: '',
  idolRole: '',
  temptationResponse: '',
  realityCondition: '',
  humanNatureView: '',
  breakupView: '',
  careerLoveConflict: '',
  emotionPriority: '',
  lifeGoalPriority: '',
})

const selfScoreMarks: Record<number, string> = { 1: '1', 5: '5', 10: '10' }

// 年龄范围滑块：相对年龄 [min, max]，负=比我小，正=比我大
const ageRange = ref<[number, number]>([-2, 2])
const ageNoMatter = ref(false)
const ageRangeMarks: Record<number, string> = {
  [-10]: '-10',
  [-5]: '-5',
  0: '同龄',
  5: '+5',
  10: '+10',
}
const ageRangeLabel = computed(() => {
  const [min, max] = ageRange.value
  const fmt = (n: number) => n === 0 ? '同龄' : n > 0 ? `比我大 ${n} 岁` : `比我小 ${-n} 岁`
  if (min === max) return fmt(min)
  return `${fmt(min)} ~ ${fmt(max)}`
})
watch(ageRange, (val) => {
  form.agePreferenceMin = val[0]
  form.agePreferenceMax = val[1]
}, { deep: true })
function onAgeNoMatterChange(checked: boolean) {
  if (checked) {
    ageRange.value = [-10, 10]
    form.agePreferenceMin = -10
    form.agePreferenceMax = 10
  } else {
    ageRange.value = [-2, 2]
    form.agePreferenceMin = -2
    form.agePreferenceMax = 2
  }
}

/** 从旧版 ageRangePreference (A/B/C/D 多选) 迁移到 [min, max] */
function migrateAgeRangeFromLegacy(pref?: string | null): [number, number] {
  if (!pref) return [-2, 2]
  const parts = pref.split(',').map((s) => s.trim()).filter(Boolean)
  if (parts.length === 0) return [-2, 2]
  let min = 10
  let max = -10
  for (const p of parts) {
    switch (p) {
      case 'A': min = Math.min(min, 1); max = Math.max(max, 2); break   // 比我大1-2岁
      case 'B': min = Math.min(min, -1); max = Math.max(max, 1); break  // 同龄±1
      case 'C': min = Math.min(min, -2); max = Math.max(max, -1); break // 比我小1-2岁
      case 'D': return [-10, 10]  // 无所谓
      default: break
    }
  }
  return [min, max]
}

// 步骤校验
const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return !!(
      form.socialStyle && form.lifeRhythm && form.personalityBase &&
      form.campusFocus && form.emotionStyle
    )
  }
  if (currentStep.value === 2) {
    return !!(
      form.targetGender && form.appearanceRequirement &&
      form.agePreferenceMin != null && form.agePreferenceMax != null &&
      form.gradeRangePreference && form.partnerPersonality &&
      form.majorPreference && form.careerAmbitionPref &&
      form.companionshipStyle && form.dateStyle && form.intimacyPace
    )
  }
  return true
})

const canSubmit = computed(() => {
  if (!canProceed.value || !agreed.value) return false
  return !!(
    form.honestyLevel && form.premaritalCohabitation && form.premaritalSex &&
    form.relationshipCoreValue && form.conflictStyle && form.socialBoundary &&
    form.futureLifestyle && form.campusLovePlan && form.idolRole &&
    form.temptationResponse && form.realityCondition && form.humanNatureView &&
    form.breakupView && form.careerLoveConflict && form.emotionPriority && form.lifeGoalPriority
  )
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

function normalizeTargetGender(value?: string | null): MomentEnrollRequest['targetGender'] {
  return value === 'male' || value === 'female' || value === 'any' ? value : 'any'
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true

  try {
    if (photoFile.value) {
      try {
        await uploadMomentPhoto(photoFile.value)
      } catch {
        ElMessage.warning('照片上传失败，但不影响报名')
      }
    }

    const payload: MomentEnrollRequest = { ...form }
    await enrollMoment(payload)
    ElMessage.success('报名成功！')
    router.replace('/moment')
  } catch (err: any) {
    const msg = err?.response?.data?.message || '报名失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getMomentProfile()
    const profile = res.data.data
    if (profile) {
      form.targetGender = normalizeTargetGender(profile.targetGender)
      form.prioritizeMatching = !!profile.prioritizeMatching
      form.socialStyle = profile.socialStyle || ''
      form.lifeRhythm = profile.lifeRhythm || ''
      form.personalityBase = profile.personalityBase || ''
      form.campusFocus = profile.campusFocus || ''
      form.emotionStyle = profile.emotionStyle || ''
      form.companionshipStyle = profile.companionshipStyle || ''
      form.appearanceRequirement = profile.appearanceRequirement || ''
      if (profile.agePreferenceMin != null && profile.agePreferenceMax != null) {
        form.agePreferenceMin = profile.agePreferenceMin
        form.agePreferenceMax = profile.agePreferenceMax
        ageRange.value = [profile.agePreferenceMin, profile.agePreferenceMax]
        ageNoMatter.value = profile.agePreferenceMin <= -10 && profile.agePreferenceMax >= 10
      } else {
        // 兼容旧数据：从 ageRangePreference 迁移
        const migrated = migrateAgeRangeFromLegacy(profile.ageRangePreference)
        form.agePreferenceMin = migrated[0]
        form.agePreferenceMax = migrated[1]
        ageRange.value = migrated
        ageNoMatter.value = migrated[0] <= -10 && migrated[1] >= 10
      }
      form.gradeRangePreference = profile.gradeRangePreference || ''
      form.partnerPersonality = profile.partnerPersonality || ''
      form.majorPreference = profile.majorPreference || ''
      form.careerAmbitionPref = profile.careerAmbitionPref || ''
      form.dateStyle = profile.dateStyle || ''
      form.intimacyPace = profile.intimacyPace || ''
      form.honestyLevel = profile.honestyLevel || ''
      form.premaritalCohabitation = profile.premaritalCohabitation || ''
      form.premaritalSex = profile.premaritalSex || ''
      form.relationshipCoreValue = profile.relationshipCoreValue || ''
      form.conflictStyle = profile.conflictStyle || ''
      form.socialBoundary = profile.socialBoundary || ''
      form.futureLifestyle = profile.futureLifestyle || ''
      form.campusLovePlan = profile.campusLovePlan || ''
      form.idolRole = profile.idolRole || ''
      form.temptationResponse = profile.temptationResponse || ''
      form.realityCondition = profile.realityCondition || ''
      form.humanNatureView = profile.humanNatureView || ''
      form.breakupView = profile.breakupView || ''
      form.careerLoveConflict = profile.careerLoveConflict || ''
      form.emotionPriority = profile.emotionPriority || ''
      form.lifeGoalPriority = profile.lifeGoalPriority || ''
      if (profile.momentSelfScore) form.selfScore = profile.momentSelfScore
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

.age-range-section {
  margin-bottom: 28px;

  .age-no-matter {
    margin-bottom: 12px;
  }
}

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

.confirm-section {
  margin-top: 24px;
  padding: 20px;
  background: $bg-secondary;
  border-radius: $radius-lg;
}

.priority-card {
  margin-bottom: 16px;
  padding: 16px;
  background: rgba($primary, 0.05);
  border: 1px solid rgba($primary, 0.12);
  border-radius: $radius-md;
}

.priority-title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
}

.priority-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: $text-muted;
  line-height: 1.6;
}

.confirm-check {
  :deep(.el-checkbox__label) {
    font-size: 14px;
    color: $text-secondary;
  }
}

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
