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
v-model="form.birthTime" placeholder="选择出生时间"
                              value-format="HH:mm" format="HH:mm" style="width:100%" />
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
      <div v-if="step === 3" class="step-content">
        <p class="interest-hint">选择你感兴趣的标签（至少3个）</p>
        <div class="interest-grid">
          <div
            v-for="tag in interestTags" :key="tag"
            class="interest-tag" :class="{ active: selectedInterests.includes(tag) }"
            @click="toggleInterest(tag)">
            {{ tag }}
          </div>
          <div
            class="interest-tag" :class="{ active: selectedInterests.includes('其他') }"
            @click="toggleInterest('其他')">
            其他
          </div>
        </div>

        <!-- 自定义兴趣输入区域 -->
        <div v-if="selectedInterests.includes('其他')" class="custom-interest-section">
          <div class="custom-interest-input-row">
            <el-input
              v-model="customInterestInput"
              placeholder="输入自定义兴趣标签"
              maxlength="10"
              show-word-limit
              class="custom-interest-input"
              @keyup.enter="addCustomInterest"
            />
            <button class="btn-add-interest" @click="addCustomInterest">
              添加
            </button>
          </div>
          <!-- 已添加的自定义兴趣 -->
          <div v-if="customInterests.length" class="custom-interest-list">
            <span
              v-for="(tag, idx) in customInterests"
              :key="`custom-${idx}`"
              class="custom-interest-tag"
            >
              {{ tag }}
              <button class="custom-tag-remove" @click="removeCustomInterest(idx)">×</button>
            </span>
          </div>
        </div>
      </div>

      <div class="step-actions">
        <button v-if="step > 1" class="btn-outline" @click="step--">上一步</button>
        <button v-if="step < 3" class="btn-primary" @click="step++">下一步</button>
        <button v-if="step === 3" class="btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : (isEditMode ? '保存修改' : '完成设置') }}
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
import { MBTI_TYPES, MBTI_LABELS, INTEREST_TAGS } from '@/constants/matchConst'
import { GENDER_OPTIONS } from '@/constants/genderConst'

const router = useRouter()
const userStore = useUserStore()
const step = ref(1)
const saving = ref(false)
const selectedInterests = ref<string[]>([])
const customInterests = ref<string[]>([])
const customInterestInput = ref('')
const isEditMode = ref(false)

const mbtiTypes = MBTI_TYPES
const mbtiLabels = MBTI_LABELS
const interestTags = INTEREST_TAGS
const genderOptions = GENDER_OPTIONS
const grades = ['大一', '大二', '大三', '大四', '研一', '研二', '研三', '博士']

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
  school: '',
  major: '',
  grade: '',
  mbti: '',
  bio: '',
})

// 加载用户原有数据
onMounted(async () => {
  // 初始化昵称为当前用户昵称
  form.nickname = userStore.user?.nickname ?? ''
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
        form.school = data.school ?? ''
        form.major = data.major ?? ''
        form.grade = data.grade ?? ''
        form.mbti = data.mbti ?? ''
        form.bio = data.bio ?? ''
        if (data.interests) {
          const allInterests = data.interests.split(',')
          // 分离预定义标签和自定义标签
          selectedInterests.value = allInterests.filter(t => INTEREST_TAGS.includes(t as any) || t === '其他')
          customInterests.value = allInterests.filter(t => !INTEREST_TAGS.includes(t as any) && t !== '其他')
        }
      }
    } catch {
      // 加载失败，使用空表单
    }
  }
})

function toggleInterest(tag: string) {
  const idx = selectedInterests.value.indexOf(tag)
  if (idx >= 0) {
    selectedInterests.value.splice(idx, 1)
  } else {
    selectedInterests.value.push(tag)
  }
}

// 添加自定义兴趣
function addCustomInterest() {
  const val = customInterestInput.value.trim()
  if (!val) {
    ElMessage.warning('请输入自定义兴趣标签')
    return
  }
  if (val.length > 10) {
    ElMessage.warning('标签长度不能超过10个字符')
    return
  }
  // 检查是否已存在
  if (customInterests.value.includes(val) || INTEREST_TAGS.includes(val as any)) {
    ElMessage.warning('该标签已存在')
    return
  }
  customInterests.value.push(val)
  customInterestInput.value = ''
}

// 删除自定义兴趣
function removeCustomInterest(idx: number) {
  customInterests.value.splice(idx, 1)
}

async function handleSave() {
  const totalInterests = [
    ...selectedInterests.value.filter(t => t !== '其他'),
    ...customInterests.value
  ]
  if (totalInterests.length < 3) {
    ElMessage.warning('请至少选择3个兴趣标签')
    return
  }

  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim() || userStore.user?.nickname || '',
      gender: form.gender,
      birthDate: form.birthDate,
      birthTime: form.birthTime,
      school: form.school,
      major: form.major,
      grade: form.grade,
      mbti: form.mbti,
      bio: form.bio,
      interests: totalInterests.join(','),
    })
    await userStore.fetchProfile()
    const message = isEditMode.value ? '资料更新成功！' : '资料完善成功！'
    ElMessage.success(message)
    // 编辑模式返回我的页面，首次完善返回首页
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

// 自定义兴趣输入区域
.custom-interest-section {
  margin-top: 20px;
  padding: 16px;
  background: rgba($primary, 0.04);
  border-radius: $radius-lg;
  border: 1px dashed $border-color;
}

.custom-interest-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.custom-interest-input {
  flex: 1;

  :deep(.el-input__wrapper) {
    border-radius: $radius-md;
  }
}

.btn-add-interest {
  padding: 8px 20px;
  background: $primary;
  color: white;
  border: none;
  border-radius: $radius-md;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover {
    opacity: 0.9;
  }
}

.custom-interest-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.custom-interest-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: $primary-gradient;
  color: white;
  border-radius: $radius-full;
  font-size: 13px;
  font-weight: 500;
}

.custom-tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  border: none;
  color: white;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba(255, 255, 255, 0.5);
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
