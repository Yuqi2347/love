<template>
  <div class="setup-page">
    <div class="setup-container">
      <div class="setup-header">
        <div class="step-indicator">
          <div v-for="i in 3" :key="i" class="step-dot" :class="{ active: step >= i, done: step > i }" />
        </div>
        <h2 class="setup-title">{{ stepTitles[step - 1] }}</h2>
        <p class="setup-desc">{{ stepDescs[step - 1] }}</p>
      </div>

      <!-- Step 1: Basic Info -->
      <div v-if="step === 1" class="step-content">
        <el-form :model="form" size="large" label-position="top">
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
          <el-form-item label="学校">
            <el-input v-model="form.school" placeholder="你的学校" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="form.major" placeholder="你的专业" />
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
      <div v-if="step === 3" class="step-content">
        <p class="interest-hint">选择你感兴趣的标签（至少3个）</p>
        <div class="interest-grid">
          <div
v-for="tag in interestTags" :key="tag"
               class="interest-tag" :class="{ active: selectedInterests.includes(tag) }"
               @click="toggleInterest(tag)">
            {{ tag }}
          </div>
        </div>
      </div>

      <div class="step-actions">
        <button v-if="step > 1" class="btn-outline" @click="step--">上一步</button>
        <button v-if="step < 3" class="btn-primary" @click="step++">下一步</button>
        <button v-if="step === 3" class="btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '完成设置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updateProfile } from '@/api/userApi'
import { useUserStore } from '@/store/userStore'
import { MBTI_TYPES, MBTI_LABELS, INTEREST_TAGS } from '@/constants/matchConst'
import { GENDER_OPTIONS } from '@/constants/genderConst'

const router = useRouter()
const userStore = useUserStore()
const step = ref(1)
const saving = ref(false)
const selectedInterests = ref<string[]>([])

const mbtiTypes = MBTI_TYPES
const mbtiLabels = MBTI_LABELS
const interestTags = INTEREST_TAGS
const genderOptions = GENDER_OPTIONS
const grades = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '博士']

const stepTitles = ['完善基础信息', '选择你的MBTI', '选择兴趣爱好']
const stepDescs = ['让大家更好地认识你', '性格匹配的重要依据', '找到志同道合的朋友']

const form = reactive({
  gender: 0,
  birthDate: '',
  school: '',
  major: '',
  grade: '',
  mbti: '',
  bio: '',
})

function toggleInterest(tag: string) {
  const idx = selectedInterests.value.indexOf(tag)
  if (idx >= 0) {
    selectedInterests.value.splice(idx, 1)
  } else {
    selectedInterests.value.push(tag)
  }
}

async function handleSave() {
  if (selectedInterests.value.length < 3) {
    ElMessage.warning('请至少选择3个兴趣标签')
    return
  }

  saving.value = true
  try {
    await updateProfile({
      nickname: userStore.user?.nickname || '',
      gender: form.gender,
      birthDate: form.birthDate,
      school: form.school,
      major: form.major,
      grade: form.grade,
      mbti: form.mbti,
      bio: form.bio,
      interests: selectedInterests.value.join(','),
    })
    await userStore.fetchProfile()
    ElMessage.success('资料完善成功！')
    router.push('/')
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

.interest-hint {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 16px;
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
  justify-content: center;
  gap: 16px;
  margin-top: 36px;

  .btn-primary, .btn-outline {
    min-width: 140px;
    height: 46px;
  }
}
</style>
