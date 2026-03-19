<template>
  <div class="enroll-page">
    <div class="page-shell">
      <section class="survey-hero panel-entrance">
        <div class="survey-hero__top">
          <span class="survey-badge">心动时刻</span>
          <span class="survey-step">第 {{ currentStep }} 步 / 共 {{ totalSteps }} 步</span>
        </div>

        <div class="survey-hero__body">
          <p class="survey-kicker">匿名匹配问卷</p>
          <h1 class="survey-title">心动时刻</h1>
          <p class="survey-desc">
            用几个简单问题，让系统更准确地理解你。
          </p>
        </div>

        <div class="survey-progress">
          <div class="survey-progress__track">
            <div class="survey-progress__fill" :style="{ width: `${completionPercent}%` }" />
          </div>
          <div class="survey-progress__meta">
            <span>已完成 {{ completionPercent }}%</span>
            <strong>{{ currentStepMeta.progressLabel }}</strong>
          </div>
        </div>

        <div class="step-tabs">
          <button
            v-for="item in stepItems"
            :key="item.id"
            type="button"
            class="step-tab"
            :class="{ active: item.id === currentStep, done: item.id < currentStep }"
            @click="jumpToStep(item.id)"
          >
            <span class="step-tab__index">0{{ item.id }}</span>
            <span class="step-tab__label">{{ item.shortTitle }}</span>
          </button>
        </div>
      </section>

      <section class="survey-sheet panel-entrance">
        <header class="sheet-header">
          <span class="sheet-header__eyebrow">{{ currentStepMeta.eyebrow }}</span>
          <h2 class="sheet-header__title">{{ currentStepMeta.title }}</h2>
          <p class="sheet-header__desc">{{ currentStepMeta.desc }}</p>
        </header>

        <section v-if="currentStep === 1" class="focus-panel">
          <div class="focus-panel__header">
            <span class="focus-panel__eyebrow">仅活动内参考</span>
            <h3>自评颜值</h3>
            <p>只作综合参考，不会单独公开展示。</p>
          </div>

          <div class="score-panel">
            <div class="score-panel__value">
              <strong>{{ form.selfScore }}</strong>
              <span>/ 10</span>
            </div>
            <div class="score-panel__slider">
              <span class="score-side">1</span>
              <el-slider
                v-model="form.selfScore"
                :min="1"
                :max="10"
                :step="1"
                :marks="selfScoreMarks"
                show-stops
              />
              <span class="score-side">10</span>
            </div>
          </div>
        </section>

        <section v-if="currentStep === 2" class="focus-group">
          <article class="focus-panel">
            <div class="focus-panel__header">
              <span class="focus-panel__eyebrow">基础偏好</span>
              <h3>匹配对象范围</h3>
              <p>先给系统一个明确方向。</p>
            </div>

            <div class="target-grid">
              <button
                v-for="opt in TARGET_GENDER_OPTIONS"
                :key="opt.value"
                type="button"
                class="target-option"
                :class="{ selected: form.targetGender === opt.value }"
                @click="form.targetGender = normalizeTargetGender(opt.value)"
              >
                <span class="target-option__emoji">{{ opt.emoji }}</span>
                <span class="target-option__label">{{ opt.label }}</span>
              </button>
            </div>
          </article>

          <article class="focus-panel">
            <div class="focus-panel__header">
              <span class="focus-panel__eyebrow">基础偏好</span>
              <h3>年龄接受区间</h3>
              <p>没有硬性要求时，可以交给系统自由判断。</p>
            </div>

            <div class="age-panel">
              <div class="age-panel__toggle">
                <el-checkbox v-model="ageNoMatter" @change="onAgeNoMatterChange">
                  年龄不是问题，合适最重要
                </el-checkbox>
              </div>

              <template v-if="!ageNoMatter">
                <div class="score-panel__slider score-panel__slider--age">
                  <span class="score-side">比我小 10 岁</span>
                  <el-slider
                    v-model="ageRange"
                    range
                    :min="-10"
                    :max="10"
                    :step="1"
                    :marks="ageRangeMarks"
                    show-stops
                  />
                  <span class="score-side">比我大 10 岁</span>
                </div>
                <div class="age-panel__result">当前接受：{{ ageRangeLabel }}</div>
              </template>
            </div>
          </article>
        </section>

        <div v-if="currentStep === 1" class="question-stack">
          <QuestionCard
            v-for="(q, index) in STEP1_QUESTIONS"
            :key="q.key"
            :index="index + 1"
            :question="q"
            :model-value="(form as any)[q.key]"
            @update:model-value="(v: string) => (form as any)[q.key] = v"
          />
        </div>

        <div v-if="currentStep === 2" class="question-stack">
          <QuestionCard
            v-for="(q, index) in STEP2_QUESTIONS"
            :key="q.key"
            :index="index + 1"
            :question="q"
            :model-value="(form as any)[q.key]"
            @update:model-value="(v: string) => (form as any)[q.key] = v"
          >
            <template v-if="q.key === 'appearanceRequirement' && form.appearanceRequirement === 'A'" #hint>
              <div class="appearance-warning">
                <el-icon><WarningFilled /></el-icon>
                <span>选择此项可能增加匹配难度，因为对颜值要求越高，符合条件的匹配对象越少。</span>
              </div>
            </template>
          </QuestionCard>
        </div>

        <template v-if="currentStep === 3">
          <div class="question-stack">
            <QuestionCard
              v-for="(q, index) in STEP3_QUESTIONS"
              :key="q.key"
              :index="index + 1"
              :question="q"
              :model-value="(form as any)[q.key]"
              @update:model-value="(v: string) => (form as any)[q.key] = v"
            />
          </div>

          <section class="focus-group focus-group--final">
            <article class="focus-panel">
              <div class="focus-panel__header">
                <span class="focus-panel__eyebrow">提交前设置</span>
                <h3>优先匹配设置</h3>
                <p>如果你更希望本周尽量获得配对结果，可以接受系统适度放宽阈值。</p>
              </div>

              <el-checkbox v-model="form.prioritizeMatching">
                我接受适当降低匹配度，优先达成匹配
              </el-checkbox>
              <p class="focus-panel__hint">勾选后会适度降低你的匹配阈值，用来提高进入候选边的机会。</p>
            </article>

            <article class="focus-panel focus-panel--soft">
              <div class="focus-panel__header">
                <span class="focus-panel__eyebrow">提交前确认</span>
                <h3>提交前确认</h3>
                <p>请确认以上信息真实有效，尽量避免明显偏差。</p>
              </div>

              <el-checkbox v-model="agreed">我已确认以上信息真实有效</el-checkbox>
            </article>
          </section>
        </template>

        <footer class="sheet-actions">
          <div class="sheet-actions__summary">
            <span>{{ currentStepMeta.title }}</span>
            <strong>{{ currentStepMeta.progressLabel }}</strong>
          </div>

          <div class="sheet-actions__buttons">
            <button v-if="currentStep > 1" type="button" class="btn-back" @click="prevStep">上一步</button>
            <button v-else type="button" class="btn-back" @click="$router.back()">取消</button>

            <button
              v-if="currentStep < totalSteps"
              type="button"
              class="btn-primary-action"
              :disabled="!canProceed"
              @click="nextStep"
            >
              下一步
            </button>
            <button
              v-else
              type="button"
              class="btn-primary-action"
              :disabled="!canSubmit || submitting"
              @click="handleSubmit"
            >
              {{ submitting ? '提交中...' : '确认提交' }}
            </button>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { enrollMoment, getMomentProfile } from '@/api/momentApi'
import type { MomentEnrollRequest } from '@/api/momentApi'
import { STEP1_QUESTIONS, STEP2_QUESTIONS, STEP3_QUESTIONS, TARGET_GENDER_OPTIONS } from '@/constants/momentConst'
import QuestionCard from './components/QuestionCard.vue'

const router = useRouter()
const currentStep = ref(1)
const submitting = ref(false)
const agreed = ref(false)

const stepItems = [
  { id: 1, eyebrow: '第一部分', title: '关于你自己', shortTitle: '关于你', subtitle: '性格与生活方式', desc: '先建立你的基础画像。', summary: '补全你的基础画像', progressLabel: '基础画像' },
  { id: 2, eyebrow: '第二部分', title: '关于你期待的 TA', shortTitle: '关于 TA', subtitle: '偏好与相处预期', desc: '明确你期待的人和相处方式。', summary: '明确理想对象范围', progressLabel: '对象偏好' },
  { id: 3, eyebrow: '第三部分', title: '关于价值观', shortTitle: '价值观', subtitle: '关系底层判断', desc: '补足更深层的关系判断。', summary: '完成最终提交检查', progressLabel: '提交确认' },
] as const

const totalSteps = stepItems.length
const currentStepMeta = computed(() => stepItems.find((item) => item.id === currentStep.value) ?? stepItems[0])
const completionPercent = computed(() => Math.round((currentStep.value / totalSteps) * 100))

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
const ageRange = ref<[number, number]>([-2, 2])
const ageNoMatter = ref(false)
const ageRangeMarks: Record<number, string> = { [-10]: '-10', [-5]: '-5', 0: '同龄', 5: '+5', 10: '+10' }

const ageRangeLabel = computed(() => {
  const [min, max] = ageRange.value
  const fmt = (n: number) => n === 0 ? '同龄' : n > 0 ? `比我大 ${n} 岁` : `比我小 ${-n} 岁`
  return min === max ? fmt(min) : `${fmt(min)} ~ ${fmt(max)}`
})

watch(ageRange, (val) => {
  form.agePreferenceMin = val[0]
  form.agePreferenceMax = val[1]
}, { deep: true })

function onAgeNoMatterChange(checked: boolean) {
  ageRange.value = checked ? [-10, 10] : [-2, 2]
  form.agePreferenceMin = ageRange.value[0]
  form.agePreferenceMax = ageRange.value[1]
}

function migrateAgeRangeFromLegacy(pref?: string | null): [number, number] {
  if (!pref) return [-2, 2]
  const parts = pref.split(',').map((s) => s.trim()).filter(Boolean)
  let min = 10
  let max = -10
  for (const p of parts) {
    if (p === 'A') {
      min = Math.min(min, 1)
      max = Math.max(max, 2)
    } else if (p === 'B') {
      min = Math.min(min, -1)
      max = Math.max(max, 1)
    } else if (p === 'C') {
      min = Math.min(min, -2)
      max = Math.max(max, -1)
    } else if (p === 'D') {
      return [-10, 10]
    }
  }
  return min <= max ? [min, max] : [-2, 2]
}

const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return !!(form.socialStyle && form.lifeRhythm && form.personalityBase && form.campusFocus && form.emotionStyle)
  }
  if (currentStep.value === 2) {
    return !!(
      form.targetGender &&
      form.appearanceRequirement &&
      form.agePreferenceMin != null &&
      form.agePreferenceMax != null &&
      form.gradeRangePreference &&
      form.partnerPersonality &&
      form.majorPreference &&
      form.careerAmbitionPref &&
      form.companionshipStyle &&
      form.dateStyle &&
      form.intimacyPace
    )
  }
  return true
})

const canSubmit = computed(() => {
  if (!canProceed.value || !agreed.value) return false
  return !!(
    form.honestyLevel &&
    form.premaritalCohabitation &&
    form.premaritalSex &&
    form.relationshipCoreValue &&
    form.conflictStyle &&
    form.socialBoundary &&
    form.futureLifestyle &&
    form.campusLovePlan &&
    form.idolRole &&
    form.temptationResponse &&
    form.realityCondition &&
    form.humanNatureView &&
    form.breakupView &&
    form.careerLoveConflict &&
    form.emotionPriority &&
    form.lifeGoalPriority
  )
})

function jumpToStep(step: number) {
  if (step === currentStep.value) return
  if (step < currentStep.value || (step === 2 && canProceed.value) || (step === 3 && canSubmit.value)) {
    currentStep.value = step
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextStep() {
  if (canProceed.value && currentStep.value < totalSteps) {
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

function normalizeTargetGender(value?: string | null): MomentEnrollRequest['targetGender'] {
  return value === 'male' || value === 'female' || value === 'any' ? value : 'any'
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await enrollMoment({ ...form })
    ElMessage.success('报名成功！')
    router.replace('/moment')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '报名失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getMomentProfile()
    const profile = res.data.data
    if (!profile) return

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
      ageRange.value = [profile.agePreferenceMin, profile.agePreferenceMax]
      form.agePreferenceMin = profile.agePreferenceMin
      form.agePreferenceMax = profile.agePreferenceMax
      ageNoMatter.value = profile.agePreferenceMin <= -10 && profile.agePreferenceMax >= 10
    } else {
      const migrated = migrateAgeRangeFromLegacy(profile.ageRangePreference)
      ageRange.value = migrated
      form.agePreferenceMin = migrated[0]
      form.agePreferenceMax = migrated[1]
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
  } catch {
    // 无已有问卷，正常流程
  }
})
</script>

<style lang="scss" scoped>
$pink: #d77fa2;
$pink-soft: #fff2f7;
$pink-border: rgba(215, 127, 162, 0.18);
$pink-border-strong: rgba(215, 127, 162, 0.32);
$text-main: #4f3941;
$text-soft: #8f7480;
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;

.enroll-page {
  min-height: 100vh;
  padding: 32px 20px 48px;
  background:
    radial-gradient(circle at top left, rgba(248, 206, 222, 0.2), transparent 28%),
    linear-gradient(180deg, #fffafc 0%, #fff5f8 44%, #ffffff 100%);
}

.page-shell {
  width: min(100%, 760px);
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.survey-hero,
.survey-sheet {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 248, 251, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 56px rgba(227, 191, 205, 0.14);
}

.survey-hero {
  padding: 22px 26px 24px;
  border-radius: 34px;
}

.survey-hero__top,
.sheet-header,
.sheet-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.survey-badge,
.survey-step,
.sheet-header__eyebrow,
.focus-panel__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 13px;
  border-radius: 999px;
  background: rgba(255, 246, 250, 0.96);
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.survey-hero__body {
  padding: 34px 0 24px;
  text-align: center;
}

.survey-kicker {
  margin-bottom: 14px;
  color: #b76587;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.survey-title,
.sheet-header__title,
.focus-panel__header h3 {
  font-family: $serif;
}

.survey-title {
  margin-bottom: 18px;
  color: $text-main;
  font-size: 46px;
  line-height: 1.02;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.survey-desc,
.sheet-header__desc,
.focus-panel__header p,
.focus-panel__hint {
  color: $text-soft;
  font-size: 14px;
  line-height: 1.85;
}

.survey-desc {
  width: min(100%, 420px);
  margin: 0 auto;
}

.survey-progress {
  margin: 0 auto 24px;
  width: min(100%, 520px);
}

.survey-progress__track {
  height: 7px;
  border-radius: 999px;
  background: rgba(243, 221, 230, 0.72);
  overflow: hidden;
  margin-bottom: 12px;
}

.survey-progress__fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #efabc4, #d77fa2);
  transition: width 0.35s ease;
}

.survey-progress__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: $text-soft;
  font-size: 12px;
}

.survey-progress__meta strong {
  color: $text-main;
  font-weight: 700;
}

.step-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.step-tab {
  min-height: 62px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(215, 127, 162, 0.12);
  transition: transform $transition-base, border-color $transition-base, box-shadow $transition-base;
}

.step-tab:hover {
  transform: translateY(-1px);
}

.step-tab.active,
.step-tab.done {
  border-color: $pink-border-strong;
}

.step-tab.active {
  background: linear-gradient(135deg, rgba(255, 247, 250, 0.98), rgba(255, 236, 242, 0.94));
  box-shadow: 0 14px 28px rgba(227, 191, 205, 0.14);
}

.step-tab__index {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: $pink-soft;
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-tab__label {
  color: $text-main;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
}

.survey-sheet {
  padding: 34px 34px 28px;
  border-radius: 34px;
}

.sheet-header {
  display: block;
  margin-bottom: 30px;
  text-align: center;
}

.sheet-header__title {
  margin: 18px 0 12px;
  color: $text-main;
  font-size: 34px;
  line-height: 1.08;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.sheet-header__eyebrow {
  margin: 0 auto;
}

.sheet-header__desc {
  width: min(100%, 360px);
  margin: 0 auto;
}

.focus-group {
  display: grid;
  gap: 14px;
  margin-bottom: 28px;
}

.focus-group--final {
  margin-top: 8px;
}

.focus-panel {
  padding: 24px 24px 22px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(255, 246, 250, 0.92));
  border: 1px solid rgba(215, 127, 162, 0.12);
  box-shadow: 0 14px 30px rgba(227, 191, 205, 0.1);
}

.focus-panel--soft {
  background: linear-gradient(180deg, rgba(255, 249, 252, 0.98), rgba(255, 241, 246, 0.94));
}

.focus-panel__header {
  margin-bottom: 18px;
}

.focus-panel__header h3 {
  margin: 14px 0 10px;
  color: $text-main;
  font-size: 24px;
  font-weight: 700;
}

.focus-panel__header p {
  max-width: 340px;
}

.score-panel {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(215, 127, 162, 0.12);
}

.score-panel__value {
  display: flex;
  align-items: end;
  gap: 6px;
  margin-bottom: 16px;
}

.score-panel__value strong {
  color: $text-main;
  font-size: 44px;
  line-height: 1;
}

.score-panel__value span {
  color: $text-soft;
  margin-bottom: 7px;
}

.score-panel__slider {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-panel__slider .el-slider {
  flex: 1;
}

.score-panel__slider--age {
  margin-top: 6px;
}

.score-side {
  flex-shrink: 0;
  color: $text-soft;
  font-size: 12px;
  font-weight: 700;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.target-option {
  min-height: 116px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 12px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(215, 127, 162, 0.12);
  transition: transform $transition-base, box-shadow $transition-base, border-color $transition-base;
}

.target-option:hover {
  transform: translateY(-1px);
}

.target-option.selected {
  background: linear-gradient(135deg, rgba(255, 247, 250, 0.98), rgba(255, 233, 240, 0.95));
  border-color: $pink-border-strong;
  box-shadow: 0 14px 26px rgba(227, 191, 205, 0.14);
}

.target-option__emoji {
  font-size: 28px;
  line-height: 1;
}

.target-option__label {
  color: $text-main;
  font-size: 15px;
  font-weight: 700;
}

.age-panel {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(215, 127, 162, 0.12);
}

.age-panel__toggle {
  margin-bottom: 12px;
}

.age-panel__result {
  display: inline-flex;
  margin-top: 14px;
  padding: 9px 13px;
  border-radius: 999px;
  background: rgba(255, 245, 249, 0.96);
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 13px;
  font-weight: 700;
}

.question-stack {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.appearance-warning {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 243, 232, 0.92);
  border: 1px solid rgba(245, 158, 11, 0.18);
  color: #92663f;
  font-size: 13px;
  line-height: 1.7;
}

.sheet-actions {
  align-items: center;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid rgba(215, 127, 162, 0.12);
}

.sheet-actions__summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sheet-actions__summary span {
  color: $text-soft;
  font-size: 12px;
}

.sheet-actions__summary strong {
  color: $text-main;
  font-size: 15px;
  font-weight: 700;
}

.sheet-actions__buttons {
  display: flex;
  gap: 12px;
}

.btn-back,
.btn-primary-action {
  min-width: 148px;
  height: 52px;
  padding: 0 24px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  transition: transform $transition-base, box-shadow $transition-base, opacity $transition-base;
}

.btn-back {
  background: rgba(255, 255, 255, 0.96);
  color: $text-soft;
  border: 1px solid $pink-border;
}

.btn-primary-action {
  color: #ffffff;
  background: linear-gradient(135deg, #efabc4 0%, #d77fa2 100%);
  box-shadow: 0 14px 28px rgba(215, 127, 162, 0.24);
}

.btn-back:hover,
.btn-primary-action:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-primary-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.panel-entrance {
  animation: rise-in 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.survey-sheet {
  animation-delay: 0.06s;
}

.question-stack :deep(.question-card) {
  animation: soft-in 0.5s ease both;
}

.question-stack :deep(.question-card:nth-child(2n)) {
  animation-delay: 0.04s;
}

.question-stack :deep(.question-card:nth-child(3n)) {
  animation-delay: 0.08s;
}

:deep(.el-slider__runway) {
  height: 8px;
  background: rgba(250, 225, 234, 0.88);
}

:deep(.el-slider__bar) {
  height: 8px;
  background: linear-gradient(135deg, #f0afc6, #d67d9f);
}

:deep(.el-slider__button) {
  width: 18px;
  height: 18px;
  border: 4px solid #ffffff;
  background: #d67d9f;
  box-shadow: 0 10px 18px rgba(214, 127, 162, 0.24);
}

:deep(.el-slider__stop) {
  width: 6px;
  height: 6px;
  background: rgba(255, 255, 255, 0.92);
}

:deep(.el-slider__marks-text) {
  color: $text-soft;
  font-size: 12px;
}

:deep(.el-checkbox__label) {
  color: $text-main;
  font-size: 14px;
  line-height: 1.7;
}

:deep(.el-checkbox__inner) {
  border-color: rgba(214, 127, 162, 0.32);
  background-color: #ffffff;
  border-radius: 6px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner),
:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  background-color: $pink;
  border-color: $pink;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes soft-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 720px) {
  .enroll-page {
    padding: 20px 14px 28px;
  }

  .survey-hero,
  .survey-sheet {
    padding-left: 18px;
    padding-right: 18px;
    border-radius: 28px;
  }

  .survey-hero__top,
  .sheet-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .survey-title {
    font-size: 36px;
  }

  .sheet-header__title {
    font-size: 30px;
  }

  .step-tabs,
  .target-grid {
    grid-template-columns: 1fr;
  }

  .score-panel__slider,
  .sheet-actions__buttons {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
