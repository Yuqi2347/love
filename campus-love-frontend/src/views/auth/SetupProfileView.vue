<template>
  <div class="setup-page">
    <div class="setup-container">
      <div class="setup-header">
        <div class="step-indicator">
          <template v-for="i in 3" :key="i">
            <div class="step-node" :class="{ active: step >= i, done: step > i }">
              <span class="step-num">{{ step > i ? '✓' : i }}</span>
            </div>
            <div v-if="i < 3" class="step-line" :class="{ active: step > i }" />
          </template>
        </div>
        <h2 class="setup-title">{{ displayTitles[step - 1] }}</h2>
        <p class="setup-desc">{{ displayDescs[step - 1] }}</p>
      </div>

      <!-- Step 1: Basic Info -->
      <div v-if="step === 1" class="step-content">
        <el-form :model="form" size="large" label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="设置你的昵称（最多10字）" maxlength="10" show-word-limit />
          </el-form-item>
          <el-form-item label="性别">
            <div class="gender-select">
              <div
v-for="g in genderOptions" :key="g.value"
                   class="gender-card" :class="{ active: form.gender === g.value }"
                   @click="form.gender = g.value">
                <span class="gender-emoji">{{ g.value === 1 ? '👨' : '👩' }}</span>
                <span class="gender-label">{{ g.label }}</span>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker
v-model="form.birthDate" type="date" placeholder="选择生日"
                            value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="出生时间（可选，用于八字精确计算）">
            <el-time-picker
              v-model="form.birthTime"
              placeholder="选择出生时间"
              value-format="HH:mm"
              format="HH:mm"
              :disabled="form.baziUnknown"
              style="width:100%"
            />
            <el-checkbox v-model="form.baziUnknown" class="bazi-unknown-check">
              我不清楚出生时辰
            </el-checkbox>
          </el-form-item>
          <el-form-item label="学校">
            <el-input v-model="form.school" placeholder="你的学校" />
          </el-form-item>
          <el-form-item label="专业">
            <el-cascader
              v-model="majorCascaderValue"
              :options="MAJOR_CASCADER_OPTIONS"
              placeholder="选择学科门类与专业类"
              :props="{ expandTrigger: 'hover' }"
              style="width:100%"
              clearable
              @change="onMajorCascaderChange"
            />
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="form.grade" placeholder="选择年级" style="width:100%">
              <el-option v-for="g in grades" :key="g" :label="g" :value="g" />
            </el-select>
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="介绍一下自己..." maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: MBTI -->
      <div v-if="step === 2" class="step-content">
        <div class="mbti-test-tip">
          <span>不确定自己的类型？</span>
          <a href="https://www.16personalities.com/ch" target="_blank" rel="noopener noreferrer" class="test-link">
            免费人格测试 →
          </a>
        </div>
        <div class="mbti-groups">
          <div v-for="(group, key) in MBTI_GROUPS" :key="key" class="mbti-group">
            <div class="mbti-group-header" :style="{ color: group.color }">
              <span>{{ group.emoji }}</span>
              <span class="mbti-group-label">{{ group.label }}</span>
            </div>
            <div class="mbti-group-grid">
              <div
                v-for="mbti in group.types" :key="mbti"
                class="mbti-card"
                :class="{ active: form.mbti === mbti }"
                :style="form.mbti === mbti ? { borderColor: group.color, background: group.bg } : {}"
                @click="form.mbti = mbti">
                <div class="mbti-type" :style="form.mbti === mbti ? { color: group.color } : {}">{{ mbti }}</div>
                <div class="mbti-label">{{ mbtiLabels[mbti] }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 3: Interests -->
      <div v-if="step === 3" class="step-content step-interests">
        <p class="interest-hint">选择你感兴趣的标签（至少 3 个，可多选），尽量涉及所有维度</p>
        <div class="interest-dimensions">
          <div v-for="dim in interestMatrix" :key="dim.key" class="interest-dimension">
            <div class="dimension-title">{{ dim.name }}</div>
            <div class="interest-grid">
              <div
                v-for="tag in dim.tags"
                :key="tag.code"
                class="interest-tag"
                :class="{ active: selectedTagCodes.has(tag.code) }"
                @click="toggleInterest(tag.code)">
                {{ tag.name }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="step-actions">
        <button v-if="step > 1" class="btn-outline" @click="step--">上一步</button>
        <button v-if="step < 3" class="btn-primary" @click="step++">下一步</button>
        <button v-if="step === 3" class="btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : (isEditMode ? '保存修改' : '完成设置') }}
        </button>
        <button
          v-if="step === 1 && !isEditMode"
          class="btn-skip-all"
          @click="handleSkipAll"
        >
          跳过全部
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updateProfile, getMyProfile } from '@/api/userApi'
import { useUserStore } from '@/store/userStore'
import { MBTI_TYPES, MBTI_LABELS, INTEREST_TAG_MATRIX, LEGACY_INTEREST_TO_CODE } from '@/constants/matchConst'
import { GENDER_OPTIONS } from '@/constants/genderConst'
import { MAJOR_CASCADER_OPTIONS, getMajorCascaderPath } from '@/constants/majorConst'
import { MBTI_GROUPS, getMbtiGroup } from '@/constants/emojiConst'

const router = useRouter()
const userStore = useUserStore()
const step = ref(1)
const saving = ref(false)
/** 已选中的 tag code 集合 */
const selectedTagCodes = ref<Set<string>>(new Set())
const isEditMode = ref(false)

const mbtiTypes = MBTI_TYPES
const mbtiLabels = MBTI_LABELS
const interestMatrix = INTEREST_TAG_MATRIX
const genderOptions = GENDER_OPTIONS
const grades = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '博士']

/** 专业级联选择器绑定值 [学科门类, 专业类]，存储时只存专业类 */
const majorCascaderValue = ref<string[]>([])

const stepTitles = ['完善基础信息', '选择你的MBTI', '选择兴趣爱好']
const stepDescs = ['让大家更好地认识你', '性格匹配的重要依据', '找到志同道合的朋友']

// 根据是否编辑模式显示不同标题
const displayTitles = computed(() => isEditMode.value ? ['编辑基础信息', '选择你的MBTI', '选择兴趣爱好'] : stepTitles)
const displayDescs = computed(() => isEditMode.value ? ['更新你的个人信息', '性格匹配的重要依据', '找到志同道合的朋友'] : stepDescs)

const form = reactive({
  nickname: '',
  gender: 0,
  birthDate: '',
  birthTime: '',
  baziUnknown: false,
  school: '',
  major: '',
  grade: '',
  mbti: '',
  bio: '',
})

// 加载用户原有数据
onMounted(async () => {
  // 初始化昵称为当前用户昵称，学校从注册带入
  form.nickname = userStore.user?.nickname ?? ''
  form.school = userStore.user?.school ?? sessionStorage.getItem('register_school') ?? ''
  if (userStore.user?.profileComplete) {
    isEditMode.value = true
    try {
      const res = await getMyProfile()
      const data = res.data.data
      if (data) {
        form.nickname = data.nickname ?? userStore.user?.nickname ?? ''
        form.gender = data.gender ?? 0
        form.birthDate = data.birthDate ?? ''
        form.birthTime = data.birthTime ?? ''
        form.baziUnknown = data.baziUnknown ?? false
        form.school = data.school ?? ''
        form.major = data.major ?? ''
        majorCascaderValue.value = getMajorCascaderPath(data.major)
        form.grade = data.grade ?? ''
        form.mbti = data.mbti ?? ''
        form.bio = data.bio ?? ''
        if (data.interestTags) {
          // 新格式 JSON: { dimension: [{code, sharing, intensity}] }
          try {
            const parsed = typeof data.interestTags === 'string' ? JSON.parse(data.interestTags) : data.interestTags
            const codes = new Set<string>()
            for (const arr of Object.values(parsed) as { code: string }[][]) {
              if (Array.isArray(arr)) arr.forEach((t) => t?.code && codes.add(t.code))
            }
            selectedTagCodes.value = codes
          } catch {
            selectedTagCodes.value = new Set()
          }
        } else if (data.interests) {
          // 旧格式逗号分隔：映射到新 code
          const codes = new Set<string>()
          data.interests.split(/[,，、]/).forEach((t) => {
            const trimmed = t.trim()
            if (LEGACY_INTEREST_TO_CODE[trimmed]) codes.add(LEGACY_INTEREST_TO_CODE[trimmed])
          })
          selectedTagCodes.value = codes
        }
      }
    } catch {
      // 加载失败，使用空表单
    }
  }
})

function onMajorCascaderChange(val: string[] | undefined) {
  const last = val?.length ? val[val.length - 1] : undefined
  form.major = last ?? ''
}

function toggleInterest(code: string) {
  const set = new Set(selectedTagCodes.value)
  if (set.has(code)) set.delete(code)
  else set.add(code)
  selectedTagCodes.value = set
}

/** 构建新格式 interest_tags JSON */
function buildInterestTagsJson(): Record<string, { code: string; sharing: number; intensity: number }[]> {
  const result: Record<string, { code: string; sharing: number; intensity: number }[]> = {}
  for (const dim of INTEREST_TAG_MATRIX) {
    const selected = dim.tags.filter((t) => selectedTagCodes.value.has(t.code))
    if (selected.length) {
      result[dim.key] = selected.map((t) => ({ code: t.code, sharing: 0.5, intensity: 0.5 }))
    }
  }
  return result
}

async function handleSave() {
  if (selectedTagCodes.value.size < 3) {
    ElMessage.warning('请至少选择 3 个兴趣标签')
    return
  }
  await doSave(buildInterestTagsJson())
}

async function handleSkipAll() {
  if (!form.nickname?.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  if (form.nickname.trim().length > 10) {
    ElMessage.warning('昵称最多10个字符')
    return
  }
  if (!form.gender) {
    ElMessage.warning('请选择性别')
    return
  }
  if (!form.birthDate) {
    ElMessage.warning('请选择生日')
    return
  }
  if (!form.grade) {
    ElMessage.warning('请选择年级')
    return
  }

  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim(),
      gender: form.gender,
      birthDate: form.birthDate,
      birthTime: form.baziUnknown ? undefined : (form.birthTime || undefined),
      baziUnknown: form.baziUnknown,
      school: form.school || undefined,
      major: form.major || undefined,
      grade: form.grade,
      mbti: '',
      bio: form.bio || undefined,
      interestTags: undefined,
    })
    await userStore.fetchProfile()
    ElMessage.success('已保存必填信息，可稍后在个人主页完善')
    const school = encodeURIComponent(form.school || sessionStorage.getItem('register_school') || '')
    router.push(school ? `/welcome?school=${school}` : '/welcome')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function doSave(interestTags: Record<string, { code: string; sharing: number; intensity: number }[]>) {
  const nick = (form.nickname.trim() || userStore.user?.nickname || '').trim()
  if (nick.length > 10) {
    ElMessage.warning('昵称最多10个字符')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: nick,
      gender: form.gender,
      birthDate: form.birthDate,
      birthTime: form.baziUnknown ? undefined : form.birthTime,
      baziUnknown: form.baziUnknown,
      school: form.school,
      major: form.major,
      grade: form.grade,
      mbti: form.mbti,
      bio: form.bio,
      interestTags: JSON.stringify(interestTags),
    })
    await userStore.fetchProfile()
    const message = isEditMode.value ? '资料更新成功！' : '资料完善成功！'
    ElMessage.success(message)
    if (isEditMode.value) {
      router.push('/profile')
    } else {
      const school = encodeURIComponent(form.school || sessionStorage.getItem('register_school') || '')
      router.push(school ? `/welcome?school=${school}` : '/welcome')
    }
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.setup-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 40px 20px;
}

.setup-container {
  width: 100%;
  max-width: 560px;
  background: $bg-primary;
  border-radius: $radius-xl;
  padding: 48px 40px;
  box-shadow: $shadow-lg;
}

.setup-header {
  text-align: center;
  margin-bottom: 36px;
}

// Connected dots step indicator
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 24px;
}

.step-node {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid $border-color;
  background: $bg-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all $transition-base;
  position: relative;

  .step-num {
    font-size: 14px;
    font-weight: 700;
    color: $text-muted;
    transition: color $transition-base;
  }

  &.active {
    border-color: $primary;
    background: $primary;
    transform: scale(1.15);
    box-shadow: 0 0 0 4px rgba($primary, 0.15);

    .step-num { color: white; }
  }

  &.done {
    border-color: $primary;
    background: rgba($primary, 0.1);
    transform: scale(1);
    box-shadow: none;

    .step-num { color: $primary; font-size: 16px; }
  }
}

.step-line {
  width: 48px;
  height: 2px;
  background: $border-color;
  transition: background $transition-base;

  &.active { background: $primary; }
}

.setup-title {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.setup-desc {
  font-size: 14px;
  color: $text-secondary;
}

.step-content {
  min-height: 300px;
  animation: step-slide-in 0.35s ease both;
}

@keyframes step-slide-in {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.mbti-test-tip {
  text-align: center;
  margin-bottom: 20px;
  font-size: 14px;
  color: $text-secondary;

  .test-link {
    color: $primary;
    text-decoration: none;
    font-weight: 500;
    margin-left: 4px;

    &:hover { text-decoration: underline; }
  }
}

// Gender cards with gradient
.gender-select {
  display: flex;
  gap: 16px;
  width: 100%;
}

.gender-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 20px;
  border: 2px solid $border-color;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-base;
  background: $bg-secondary;

  .gender-emoji { font-size: 42px; transition: transform $transition-base; }
  .gender-label { font-weight: 600; font-size: 15px; }

  &:hover {
    border-color: $primary-light;
    .gender-emoji { transform: scale(1.1); }
  }

  &.active {
    border-color: $primary;
    background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
    box-shadow: 0 0 0 3px rgba($primary, 0.1);
    color: $primary;

    .gender-emoji { transform: scale(1.15); }
  }
}

.bazi-unknown-check {
  margin-top: 8px;
  display: block;
}

// MBTI color-grouped
.mbti-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mbti-group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.mbti-group-label {
  font-size: 12px;
}

.mbti-group-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.mbti-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 8px;
  border: 2px solid $border-color;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-base;

  .mbti-type { font-size: 16px; font-weight: 700; transition: color $transition-fast; }
  .mbti-label { font-size: 11px; color: $text-secondary; margin-top: 4px; }

  &:hover {
    border-color: $primary-light;
    transform: translateY(-2px);
  }

  &.active {
    transform: translateY(-2px);
    box-shadow: $shadow-sm;
  }
}

// Interests
.step-interests {
  max-height: 420px;
  overflow-y: auto;
}

.interest-hint {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 16px;
}

.interest-dimensions {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.interest-dimension {
  .dimension-title {
    font-size: 13px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 10px;
  }
}

.interest-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.interest-tag {
  padding: 8px 18px;
  border: 1.5px solid $border-color;
  border-radius: $radius-full;
  font-size: 14px;
  cursor: pointer;
  transition: all $transition-base;
  user-select: none;

  &.active {
    background: $primary;
    color: $text-inverse;
    border-color: $primary;
    animation: tag-bounce 0.3s ease;
  }

  &:hover:not(.active) {
    border-color: $primary-light;
    color: $primary;
  }
}

@keyframes tag-bounce {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.step-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  margin-top: 36px;

  .btn-primary, .btn-outline {
    min-width: 140px;
    height: 46px;
  }

  .btn-skip-all {
    width: 100%;
    margin-top: 8px;
    padding: 10px;
    font-size: 14px;
    color: $text-muted;
    background: transparent;
    border: none;
    cursor: pointer;

    &:hover { color: $primary; }
  }
}

@media (max-width: $bp-mobile) {
  .setup-container { padding: 32px 20px; }
  .mbti-group-grid { grid-template-columns: repeat(2, 1fr); }
  .step-line { width: 32px; }
}
</style>
