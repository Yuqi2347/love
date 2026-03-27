<template>
  <div class="enroll-page">
    <div class="page-shell">
      <section class="survey-hero glass-panel panel-entrance">
        <div class="survey-hero__top">
          <span class="survey-badge glass-pill">
            <span class="pulse-dot"></span> 深度探测
          </span>
          <span class="survey-step glass-pill">阶段 {{ currentStep }} / {{ totalSteps }}</span>
        </div>

        <div class="survey-hero__body">
          <p class="survey-kicker">MOMENT ENROLLMENT</p>
          <h1 class="survey-title text-gradient-warm">心动档案录入</h1>
          <p class="survey-desc">
            抛开外界的杂音，用几个简单的问题，让引力场更精准地捕获你的同频者。
          </p>
        </div>

        <div class="survey-progress">
          <div class="survey-progress__track">
            <div class="survey-progress__fill glow-bg-warm" :style="{ width: `${completionPercent}%` }" />
          </div>
          <div class="survey-progress__meta">
            <span>档案完成度 {{ completionPercent }}%</span>
            <strong class="text-gradient-warm">{{ currentStepMeta.progressLabel }}</strong>
          </div>
        </div>

        <div class="step-tabs">
          <button
            v-for="item in stepItems"
            :key="item.id"
            type="button"
            class="step-tab glass-card-light"
            :class="{ active: item.id === currentStep, done: item.id < currentStep }"
            @click="jumpToStep(item.id)"
          >
            <span class="step-tab__index">0{{ item.id }}</span>
            <span class="step-tab__label">{{ item.shortTitle }}</span>
            <div v-if="item.id === currentStep" class="tab-glow-indicator"></div>
          </button>
        </div>
      </section>

      <section class="survey-sheet glass-panel panel-entrance">
        <header class="sheet-header">
          <span class="sheet-header__eyebrow glass-pill">{{ currentStepMeta.eyebrow }}</span>
          <h2 class="sheet-header__title">{{ currentStepMeta.title }}</h2>
          <p class="sheet-header__desc">{{ currentStepMeta.desc }}</p>
        </header>

        <section v-if="currentStep === 1" class="focus-panel glass-card-light">
          <div class="focus-panel__header">
            <span class="focus-panel__eyebrow glass-pill-light">仅限算法底层参考</span>
            <h3>客观自评颜值</h3>
            <p>该数据由算法单向加密处理，绝对不会向任何第三方或匹配对象公开展示。</p>
          </div>

          <div class="score-panel glass-pill-light">
            <div class="score-panel__value">
              <strong class="text-gradient-warm">{{ form.selfScore }}</strong>
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
                class="custom-aurora-slider"
              />
              <span class="score-side">10</span>
            </div>
          </div>
        </section>

        <section v-if="currentStep === 2" class="focus-group">
          <article class="focus-panel glass-card-light">
            <div class="focus-panel__header">
              <span class="focus-panel__eyebrow glass-pill-light">基础雷达</span>
              <h3>期望捕获的性别</h3>
              <p>为算法指定一个明确的扫描方向。</p>
            </div>

            <div class="target-grid">
              <button
                v-for="opt in TARGET_GENDER_OPTIONS"
                :key="opt.value"
                type="button"
                class="target-option glass-card-light"
                :class="{ selected: form.targetGender === opt.value }"
                @click="form.targetGender = normalizeTargetGender(opt.value)"
              >
                <span class="target-option__emoji">{{ opt.emoji }}</span>
                <span class="target-option__label">{{ opt.label }}</span>
                <div v-if="form.targetGender === opt.value" class="option-glow-bg"></div>
              </button>
            </div>
          </article>

          <article class="focus-panel glass-card-light">
            <div class="focus-panel__header">
              <span class="focus-panel__eyebrow glass-pill-light">基础雷达</span>
              <h3>年龄接受区间</h3>
              <p>若无硬性边界，可交给 AI 根据其他维度自由裁量。</p>
            </div>

            <div class="age-panel glass-pill-light">
              <div class="age-panel__toggle">
                <el-checkbox v-model="ageNoMatter" @change="onAgeNoMatterChange" class="custom-aurora-checkbox">
                  同频最重要，年龄无所谓
                </el-checkbox>
              </div>

              <template v-if="!ageNoMatter">
                <div class="score-panel__slider score-panel__slider--age score-panel__slider--age-only">
                  <el-slider
                    v-model="ageRange"
                    range
                    :min="-10"
                    :max="10"
                    :step="1"
                    :marks="ageRangeMarks"
                    show-stops
                    class="custom-aurora-slider"
                  />
                </div>
                <div class="age-panel__result">当前阈值：<span class="text-gradient-warm">{{ ageRangeLabel }}</span></div>
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
              <div class="appearance-warning glass-pill-light">
                <el-icon><WarningFilled /></el-icon>
                <span>设定极高颜值阈值会显著缩小雷达扫描范围，可能导致本周匹配落空，请确认。</span>
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
            <article class="focus-panel glass-card-light">
              <div class="focus-panel__header">
                <span class="focus-panel__eyebrow glass-pill-light">引力场策略</span>
                <h3>放宽匹配阈值</h3>
                <p>如果你更希望本周能结识新朋友，可以允许系统在局部维度上适度妥协。</p>
              </div>
              <el-checkbox v-model="form.prioritizeMatching" class="custom-aurora-checkbox">
                我接受适度降低绝对匹配度，优先促成相遇
              </el-checkbox>
            </article>

            <article class="focus-panel glass-card-light">
              <div class="focus-panel__header">
                <span class="focus-panel__eyebrow glass-pill-light">最终授权</span>
                <h3>发射前自检</h3>
                <p>算法的精准度建立在你的真诚之上。请确认所填信息反映了真实的你。</p>
              </div>
              <el-checkbox v-model="agreed" class="custom-aurora-checkbox">
                我承诺以上信息真实有效，愿对每一次心动负责
              </el-checkbox>
            </article>
          </section>
        </template>

        <footer class="sheet-actions">
          <div class="sheet-actions__summary">
            <span>{{ currentStepMeta.title }}</span>
            <strong class="text-gradient-warm">{{ currentStepMeta.progressLabel }}</strong>
          </div>

          <div class="sheet-actions__buttons">
            <button v-if="currentStep > 1" type="button" class="btn-back glass-btn" @click="prevStep">上一步</button>
            <button v-else type="button" class="btn-back glass-btn" @click="$router.back()">暂存并返回</button>

            <button
              v-if="currentStep < totalSteps"
              type="button"
              class="glow-btn-warm px-8"
              :disabled="!canProceed"
              @click="nextStep"
            >
              继续补全
            </button>
            <button
              v-else
              type="button"
              class="glow-btn-warm px-8"
              :disabled="!canSubmit || submitting"
              @click="handleSubmit"
            >
              {{ submitting ? '信号发射中...' : '确认发射档案' }}
            </button>
          </div>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心业务逻辑 100% 保持原封不动
// ==========================================
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { enrollMoment, getMomentProfile } from '@/api/momentApi'
import type { MomentEnrollRequest } from '@/api/momentApi'
import { STEP1_QUESTIONS, STEP2_QUESTIONS, STEP3_QUESTIONS, TARGET_GENDER_OPTIONS } from '@/constants/momentConst'
import QuestionCard from './components/QuestionCard.vue'

const router = useRouter()
const currentStep = ref(1)
const submitting = ref(false)
const agreed = ref(false)

const stepItems = [
  { id: 1, eyebrow: '第一部分', title: '探寻自我底色', shortTitle: '自我底色', subtitle: '性格与生活方式', desc: '在浩瀚引力场中，首先标定你自己的坐标。', summary: '补全你的基础画像', progressLabel: '基础画像' },
  { id: 2, eyebrow: '第二部分', title: '描绘理想星轨', shortTitle: '理想星轨', subtitle: '偏好与相处预期', desc: '告诉系统，你更希望被哪种特质的人吸引。', summary: '明确理想对象范围', progressLabel: '对象偏好' },
  { id: 3, eyebrow: '第三部分', title: '对齐底层引力', shortTitle: '底层引力', subtitle: '关系底层判断', desc: '三观的契合，是抵抗岁月漫长的核心引力。', summary: '完成最终提交检查', progressLabel: '提交确认' },
] as const

const totalSteps = stepItems.length
const currentStepMeta = computed(() => stepItems.find((item) => item.id === currentStep.value) ?? stepItems[0])
const completionPercent = computed(() => Math.round((currentStep.value / totalSteps) * 100))

const form = reactive<MomentEnrollRequest>({
  selfScore: 5, targetGender: 'any', prioritizeMatching: false, socialStyle: '', lifeRhythm: '',
  personalityBase: '', campusFocus: '', emotionStyle: '', companionshipStyle: '', appearanceRequirement: '',
  agePreferenceMin: -2, agePreferenceMax: 2, gradeRangePreference: '', partnerPersonality: '', majorPreference: '',
  careerAmbitionPref: '', dateStyle: '', intimacyPace: '', honestyLevel: '', premaritalCohabitation: '',
  premaritalSex: '', relationshipCoreValue: '', conflictStyle: '', socialBoundary: '', futureLifestyle: '',
  campusLovePlan: '', idolRole: '', temptationResponse: '', realityCondition: '', humanNatureView: '',
  breakupView: '', careerLoveConflict: '', emotionPriority: '', lifeGoalPriority: '',
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
  form.agePreferenceMin = val[0]; form.agePreferenceMax = val[1]
}, { deep: true })

function onAgeNoMatterChange(checked: boolean) {
  ageRange.value = checked ? [-10, 10] : [-2, 2]
  form.agePreferenceMin = ageRange.value[0]; form.agePreferenceMax = ageRange.value[1]
}

function migrateAgeRangeFromLegacy(pref?: string | null): [number, number] {
  if (!pref) return [-2, 2]
  const parts = pref.split(',').map((s) => s.trim()).filter(Boolean)
  let min = 10; let max = -10
  for (const p of parts) {
    if (p === 'A') { min = Math.min(min, 1); max = Math.max(max, 2) } 
    else if (p === 'B') { min = Math.min(min, -1); max = Math.max(max, 1) } 
    else if (p === 'C') { min = Math.min(min, -2); max = Math.max(max, -1) } 
    else if (p === 'D') return [-10, 10]
  }
  return min <= max ? [min, max] : [-2, 2]
}

const canProceed = computed(() => {
  if (currentStep.value === 1) return !!(form.socialStyle && form.lifeRhythm && form.personalityBase && form.campusFocus && form.emotionStyle)
  if (currentStep.value === 2) return !!(form.targetGender && form.appearanceRequirement && form.agePreferenceMin != null && form.agePreferenceMax != null && form.gradeRangePreference && form.partnerPersonality && form.majorPreference && form.careerAmbitionPref && form.companionshipStyle && form.dateStyle && form.intimacyPace)
  return true
})

const canSubmit = computed(() => {
  if (!canProceed.value || !agreed.value) return false
  return !!(form.honestyLevel && form.premaritalCohabitation && form.premaritalSex && form.relationshipCoreValue && form.conflictStyle && form.socialBoundary && form.futureLifestyle && form.campusLovePlan && form.idolRole && form.temptationResponse && form.realityCondition && form.humanNatureView && form.breakupView && form.careerLoveConflict && form.emotionPriority && form.lifeGoalPriority)
})

function jumpToStep(step: number) {
  if (step === currentStep.value) return
  if (step >= 1 && step <= totalSteps) { currentStep.value = step; window.scrollTo({ top: 0, behavior: 'smooth' }) }
}
function nextStep() {
  if (canProceed.value && currentStep.value < totalSteps) { currentStep.value++; window.scrollTo({ top: 0, behavior: 'smooth' }) }
}
function prevStep() {
  if (currentStep.value > 1) { currentStep.value--; window.scrollTo({ top: 0, behavior: 'smooth' }) }
}
function normalizeTargetGender(value?: string | null): MomentEnrollRequest['targetGender'] {
  return value === 'male' || value === 'female' || value === 'any' ? value : 'any'
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await enrollMoment({ ...form })
    ElMessage.success('档案发射成功！')
    router.replace('/moment')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '发射失败，请重试')
  } finally { submitting.value = false }
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
      form.agePreferenceMin = profile.agePreferenceMin; form.agePreferenceMax = profile.agePreferenceMax
    } else {
      const migrated = migrateAgeRangeFromLegacy(profile.ageRangePreference)
      ageRange.value = migrated; form.agePreferenceMin = migrated[0]; form.agePreferenceMax = migrated[1]
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
  } catch {}
})
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 问卷 UI
   ========================================== */
$bg-aurora: #f8fafc;
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$border-light: rgba(255, 255, 255, 0.8);

.enroll-page {
  min-height: 100vh;
  padding: 32px 20px 48px;
  background: $bg-aurora;
  position: relative;
  
  &::before {
    content: ''; position: fixed; inset: 0; pointer-events: none;
    background: 
      radial-gradient(circle at 10% 10%, rgba(79, 140, 255, 0.1), transparent 45%),
      radial-gradient(circle at 90% 40%, rgba(255, 51, 102, 0.08), transparent 45%),
      radial-gradient(circle at 50% 90%, rgba(255, 123, 84, 0.06), transparent 50%);
    z-index: 0;
  }
}

.page-shell {
  width: min(100%, 760px);
  margin: 0 auto; display: grid; gap: 24px; position: relative; z-index: 1;
}

// --- 通用毛玻璃组件 ---
.glass-panel {
  background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light; box-shadow: 0 10px 40px rgba(31, 38, 135, 0.05); border-radius: 28px;
}
.glass-card-light {
  background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(12px); 
  border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}
.glass-pill {
  background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 999px;
}
.glass-pill-light {
  background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 16px;
}

// 文本高光渐变
.text-gradient-warm {
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800;
}
.glow-bg-warm {
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  box-shadow: 0 0 12px rgba(255, 51, 102, 0.4);
}
.glow-btn-warm {
  height: 52px; border-radius: 999px; border: none;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white;
  font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 1px;
  box-shadow: 0 8px 25px rgba(255, 51, 102, 0.3); transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  &:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 12px 30px rgba(255, 51, 102, 0.4); }
  &:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.8; color: #fff; }
}
.glass-btn {
  height: 52px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  color: $text-sub; font-size: 15px; font-weight: 700; cursor: pointer; transition: all 0.3s;
  &:hover { background: #fff; color: $text-main; transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.05); }
}
.px-8 { padding-left: 32px; padding-right: 32px; }

// --- Hero 区 ---
.survey-hero { padding: 24px 28px; }
.survey-hero__top, .sheet-header, .sheet-actions { display: flex; justify-content: space-between; gap: 16px; }
.survey-badge, .survey-step { padding: 6px 16px; color: $text-sub; font-size: 12px; font-weight: 700; letter-spacing: 1px; display: inline-flex; align-items: center; gap: 6px;}
.pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: $accent-pink; animation: pulse 2s infinite; }

.survey-hero__body { padding: 32px 0; text-align: center; }
.survey-kicker { margin-bottom: 12px; color: $accent-blue; font-size: 12px; font-weight: 800; letter-spacing: 0.15em; }
.survey-title { margin-bottom: 16px; font-size: 40px; line-height: 1.1; letter-spacing: -1px; }
.survey-desc { max-width: 480px; margin: 0 auto; color: $text-sub; font-size: 15px; line-height: 1.6; }

// 进度条
.survey-progress { margin: 0 auto 24px; max-width: 520px; }
.survey-progress__track { height: 6px; border-radius: 999px; background: rgba(255, 255, 255, 0.5); border: 1px solid rgba(255,255,255,0.8); overflow: hidden; margin-bottom: 12px; }
.survey-progress__fill { height: 100%; border-radius: inherit; transition: width 0.5s cubic-bezier(0.2, 0.8, 0.2, 1); }
.survey-progress__meta { display: flex; justify-content: space-between; font-size: 13px; color: $text-sub; }

// 阶段导航 Tabs
.step-tabs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.step-tab {
  min-height: 64px; display: flex; align-items: center; gap: 12px; padding: 12px 16px; cursor: pointer;
  transition: all 0.3s; position: relative; overflow: hidden; border: none;
  
  &:hover { transform: translateY(-2px); background: rgba(255, 255, 255, 0.8); }
  &.active, &.done { border: 1px solid rgba(255, 51, 102, 0.2); }
  &.active {
    background: rgba(255, 255, 255, 0.9); box-shadow: 0 8px 25px rgba(255, 51, 102, 0.1);
    .step-tab__index { background: linear-gradient(135deg, $accent-pink, $accent-orange); color: #fff; border-color: transparent;}
  }
}
.step-tab__index {
  width: 36px; height: 36px; display: inline-flex; align-items: center; justify-content: center;
  border-radius: 12px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  color: $text-sub; font-size: 13px; font-weight: 800; flex-shrink: 0; transition: all 0.3s;
}
.step-tab__label { color: $text-main; font-size: 14px; font-weight: 700; text-align: left; }
.tab-glow-indicator { position: absolute; bottom: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg, $accent-pink, $accent-orange); }

// --- Sheet 问卷主体区 ---
.survey-sheet { padding: 40px 32px 32px; }
.sheet-header { display: flex; flex-direction: column; align-items: center; text-align: center; margin-bottom: 32px; gap: 12px; }
.sheet-header__eyebrow { padding: 4px 12px; font-size: 12px; color: $accent-pink; font-weight: 700; }
.sheet-header__title { font-size: 32px; font-weight: 800; color: $text-main; margin: 0; }
.sheet-header__desc { max-width: 400px; color: $text-sub; font-size: 15px; margin: 0;}

.focus-group { display: grid; gap: 12px; margin-bottom: 18px; }
.focus-group--final { margin-top: 12px; }

.focus-panel { padding: 20px; transition: transform 0.3s; &:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,0,0,0.04); } }
.focus-panel__header { margin-bottom: 16px; }
.focus-panel__header h3 { margin: 10px 0 6px; color: $text-main; font-size: 20px; font-weight: 800; }
.focus-panel__header p { color: $text-sub; font-size: 14px; line-height: 1.6;}
.focus-panel__eyebrow { padding: 4px 10px; font-size: 11px; font-weight: 700; color: $accent-blue; }

// --- 交互组件 (Slider / Options / Checkbox) ---
.score-panel { padding: 18px 20px; display: flex; flex-direction: column; gap: 14px; }
.score-panel__value { display: flex; align-items: baseline; gap: 6px; }
.score-panel__value strong { font-size: 42px; line-height: 1; }
.score-panel__value span { color: $text-sub; font-size: 16px; font-weight: 600; }
.score-panel__slider { display: flex; align-items: center; gap: 14px; }
.score-side { font-size: 14px; font-weight: 800; color: #cbd5e1; }

.target-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.target-option {
  position: relative; overflow: hidden; border: none; min-height: 96px; padding: 14px 12px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 8px; cursor: pointer; transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  
  &:hover { transform: translateY(-4px); background: rgba(255, 255, 255, 0.8); }
  &.selected {
    border: 1px solid rgba(255, 51, 102, 0.3); background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 10px 30px rgba(255, 51, 102, 0.12);
    .target-option__label { color: $accent-pink; }
    .target-option__emoji { transform: scale(1.15); }
  }
}
.target-option__emoji { font-size: 28px; transition: transform 0.3s; z-index: 1; }
.target-option__label { font-size: 14px; font-weight: 700; color: $text-main; z-index: 1; transition: color 0.3s;}
.option-glow-bg { position: absolute; bottom: -20px; right: -20px; width: 80px; height: 80px; background: radial-gradient(circle, rgba(255,51,102,0.15) 0%, transparent 70%); z-index: 0; }

.age-panel { padding: 18px 20px; }
.age-panel__toggle { margin-bottom: 16px; }
.age-panel__result { display: inline-block; margin-top: 14px; padding: 7px 14px; font-size: 13px; font-weight: 600; background: rgba(255,255,255,0.7); border: 1px solid #fff; border-radius: 999px; }

.question-stack { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }

.appearance-warning {
  display: flex; gap: 10px; margin-top: 12px; padding: 14px 16px;
  background: rgba(255, 247, 237, 0.8); border: 1px solid rgba(249, 115, 22, 0.3);
  color: #c2410c; font-size: 13px; line-height: 1.6;
}

// 底部动作区
.sheet-actions {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 32px; padding-top: 24px; border-top: 1px solid rgba(0,0,0,0.05);
}
.sheet-actions__summary { display: flex; flex-direction: column; gap: 4px; }
.sheet-actions__summary span { color: $text-sub; font-size: 13px; }
.sheet-actions__summary strong { font-size: 16px; }
.sheet-actions__buttons { display: flex; gap: 12px; }

// --- 深度定制 Element Plus 表单组件 ---
:deep(.custom-aurora-slider) {
  .el-slider__runway { height: 8px; background: rgba(226, 232, 240, 0.8); border-radius: 999px; }
  .el-slider__bar { height: 8px; background: linear-gradient(90deg, $accent-pink, $accent-orange); border-radius: 999px; }
  .el-slider__button { 
    width: 22px; height: 22px; border: 4px solid #fff; background: $accent-pink; 
    box-shadow: 0 4px 12px rgba(255, 51, 102, 0.4); transition: transform 0.2s;
    &:hover, &.hover, &.dragging { transform: scale(1.2); }
  }
  .el-slider__stop { width: 6px; height: 6px; background: #fff; border-radius: 50%; top: 1px; }
  .el-slider__marks-text { color: $text-sub; font-weight: 600; margin-top: 10px; }
}

:deep(.custom-aurora-checkbox) {
  .el-checkbox__label { color: $text-main; font-size: 15px; font-weight: 600; }
  .el-checkbox__inner { width: 18px; height: 18px; border-color: rgba(0,0,0,0.2); background: #fff; border-radius: 6px; transition: all 0.3s; }
  .el-checkbox__inner::after { top: 2px; left: 6px; }
  &.is-checked .el-checkbox__inner, &.is-indeterminate .el-checkbox__inner {
    background-color: $accent-pink; border-color: $accent-pink; box-shadow: 0 2px 8px rgba(255, 51, 102, 0.3);
  }
  &.is-checked .el-checkbox__label { color: $accent-pink; }
}

// --- 动画序列 ---
.panel-entrance { animation: rise-in 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
.survey-sheet { animation-delay: 0.1s; }
.question-stack :deep(.question-card) { animation: soft-in 0.5s ease both; }
@for $i from 1 through 10 {
  .question-stack :deep(.question-card:nth-child(#{$i})) { animation-delay: #{0.1 + $i * 0.05}s; }
}

@keyframes rise-in { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes soft-in { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(255,51,102, 0.4); } 70% { box-shadow: 0 0 0 6px rgba(255,51,102, 0); } 100% { box-shadow: 0 0 0 0 rgba(255,51,102, 0); } }

// --- 移动端适配 ---
@media (max-width: 720px) {
  .enroll-page { padding: 16px 12px 32px; }
  .survey-hero, .survey-sheet { padding: 24px 20px; border-radius: 24px; }
  .survey-title { font-size: 32px; }
  .sheet-header__title { font-size: 28px; }
  .step-tabs, .target-grid { grid-template-columns: 1fr; }
  .target-option { min-height: 84px; padding: 12px 14px; }
  .target-option__emoji { font-size: 26px; }
  .sheet-actions { flex-direction: column; align-items: stretch; gap: 20px; }
  .sheet-actions__buttons { flex-direction: column; }
  .btn-back, .glow-btn-warm { width: 100%; }
}
</style>
