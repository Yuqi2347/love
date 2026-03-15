<template>
  <div class="setup-page">
    <div class="setup-container">
      <div class="setup-header">
        <div class="step-indicator">
          <div v-for="i in 3" :key="i" class="step-dot" :class="{ active: step >= i, done: step > i }" />
        </div>
        <h2 class="setup-title">{{ displayTitles[step - 1] }}</h2>
        <p class="setup-desc">{{ displayDescs[step - 1] }}</p>
      </div>

      <!-- Step 1: Basic Info -->
      <div v-if="step === 1" class="step-content">
        <el-form :model="form" size="large" label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="设置你的昵称" maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item label="性别">
            <div class="gender-select">
              <div
v-for="g in genderOptions" :key="g.value"
                   class="gender-card" :class="{ active: form.gender === g.value }"
                   @click="form.gender = g.value">
                <span class="gender-emoji">{{ g.value === 1 ? '👨' : '👩' }}</span>
                <span>{{ g.label }}</span>
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
        <div class="mbti-grid">
          <div
v-for="mbti in mbtiTypes" :key="mbti"
               class="mbti-card" :class="{ active: form.mbti === mbti }"
               @click="form.mbti = mbti">
            <div class="mbti-type">{{ mbti }}</div>
            <div class="mbti-label">{{ mbtiLabels[mbti] }}</div>
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
    router.push('/')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function doSave(interestTags: Record<string, { code: string; sharing: number; intensity: number }[]>) {
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim() || userStore.user?.nickname || '',
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
    router.push(isEditMode.value ? '/profile' : '/')
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
  background: $bg-secondary;
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

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;

  .step-dot {
    width: 40px;
    height: 4px;
    border-radius: 2px;
    background: $border-color;
    transition: all $transition-base;

    &.active { background: $primary; width: 56px; }
    &.done { background: $primary-light; }
  }
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

    &:hover {
      text-decoration: underline;
    }
  }
}

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
  gap: 8px;
  padding: 20px;
  border: 2px solid $border-color;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: all $transition-base;
  font-weight: 600;

  .gender-emoji { font-size: 32px; }

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.05);
    color: $primary;
  }

  &:hover { border-color: $primary-light; }
}

.bazi-unknown-check {
  margin-top: 8px;
  display: block;
}

.mbti-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
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

  .mbti-type { font-size: 16px; font-weight: 700; }
  .mbti-label { font-size: 11px; color: $text-secondary; margin-top: 4px; }

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.05);
    .mbti-type { color: $primary; }
  }

  &:hover { border-color: $primary-light; }
}

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
  }

  &:hover:not(.active) {
    border-color: $primary-light;
    color: $primary;
  }
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

    &:hover {
      color: $primary;
    }
  }
}
</style>
