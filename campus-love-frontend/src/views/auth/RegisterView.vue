<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="brand-area">
        <div class="brand-icon">💕</div>
        <h1 class="brand-title">Campus Love</h1>
        <p class="brand-subtitle">加入校园交友，开启缘分之旅</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">🎓</span>
            <span>学校邮箱认证，安全可靠</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">✨</span>
            <span>多维匹配算法，找到契合的TA</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔒</span>
            <span>隐私保护，渐进式社交</span>
          </div>
        </div>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-card">
        <h2 class="auth-title">创建账号</h2>
        <p class="auth-desc">使用学校邮箱注册</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleRegister">
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="昵称" prefix-icon="User" />
          </el-form-item>

          <!-- 学校搜索选择 -->
          <el-form-item prop="schoolKeyword" label="学校">
            <div class="school-input-wrap">
              <el-input
                v-model="form.schoolKeyword"
                placeholder="输入学校名称搜索（如：深圳）"
                prefix-icon="Search"
                clearable
                @input="onSchoolSearch"
                @focus="onSchoolFocus"
              />
              <div v-if="showSchoolDropdown" class="school-dropdown">
                <div
                  v-for="s in schoolList"
                  :key="s.domain"
                  class="school-item"
                  @click="selectSchool(s)"
                >
                  {{ s.name }}
                  <span class="school-domain">{{ s.domain }}</span>
                </div>
                <div v-if="form.schoolKeyword && schoolList.length === 0" class="school-empty">未找到匹配的学校</div>
              </div>
            </div>
          </el-form-item>

          <!-- 邮箱输入（选择学校后显示） -->
          <el-form-item v-if="selectedSchool" prop="emailPrefix" label="邮箱">
            <div class="email-input-wrapper">
              <el-input
                v-model="form.emailPrefix"
                placeholder="邮箱前缀"
                prefix-icon="Message"
                class="email-prefix-input"
              />
              <span class="email-suffix">@{{ selectedSchool.domain }}</span>
              <button
                type="button"
                class="btn-send-code"
                :disabled="sendCodeCooldown > 0 || sendingCode"
                @click="handleSendCode"
              >
                {{ sendCodeCooldown > 0 ? `${sendCodeCooldown}s后重发` : (sendingCode ? '发送中...' : '发送验证码') }}
              </button>
            </div>
          </el-form-item>

          <!-- 验证码输入 -->
          <el-form-item v-if="selectedSchool" prop="verifyCode">
            <el-input
              v-model="form.verifyCode"
              placeholder="请输入收到的6位验证码"
              prefix-icon="Key"
              maxlength="6"
              show-word-limit
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <button type="submit" class="btn-primary auth-btn" :disabled="loading">
              {{ loading ? '注册中...' : '注 册' }}
            </button>
          </el-form-item>
        </el-form>
        <div class="auth-footer">
          已有账号？<router-link to="/login" class="auth-link">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { register, searchSchools, sendVerifyCode, type SchoolItem } from '@/api/authApi'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const schoolList = ref<SchoolItem[]>([])
const showSchoolDropdown = ref(false)
const selectedSchool = ref<SchoolItem | null>(null)
const sendCodeCooldown = ref(0)
const sendingCode = ref(false)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  nickname: '',
  schoolKeyword: '',
  emailPrefix: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
})

const fullEmail = computed(() => {
  if (!selectedSchool.value || !form.emailPrefix.trim()) return ''
  return form.emailPrefix.trim() + '@' + selectedSchool.value.domain
})

const validateConfirm = (
  _rule: unknown,
  value: string,
  callback: (err?: Error) => void
) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const validateEmail = (
  _rule: unknown,
  _value: unknown,
  callback: (err?: Error) => void
) => {
  if (!selectedSchool.value) {
    callback(new Error('请先选择学校'))
    return
  }
  if (!form.emailPrefix.trim()) {
    callback(new Error('请输入邮箱前缀'))
    return
  }
  const prefix = form.emailPrefix.trim()
  if (!/^[a-zA-Z0-9._-]+$/.test(prefix)) {
    callback(new Error('邮箱前缀只能包含字母、数字、点、下划线、横线'))
    return
  }
  callback()
}

const validateSchool = (
  _rule: unknown,
  _value: unknown,
  callback: (err?: Error) => void
) => {
  if (!selectedSchool.value) callback(new Error('请从列表中选择学校'))
  else callback()
}

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  schoolKeyword: [{ validator: validateSchool, trigger: 'blur' }],
  emailPrefix: [{ validator: validateEmail, trigger: 'blur' }],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function onSchoolSearch() {
  const kw = form.schoolKeyword.trim()
  if (!kw) {
    schoolList.value = []
    showSchoolDropdown.value = true
    return
  }
  try {
    const res = await searchSchools(kw)
    schoolList.value = res.data.data || []
    showSchoolDropdown.value = true
  } catch {
    schoolList.value = []
  }
}

function onSchoolFocus() {
  if (form.schoolKeyword.trim()) {
    showSchoolDropdown.value = true
    onSchoolSearch()
  } else {
    schoolList.value = []
    showSchoolDropdown.value = true
    searchSchools().then(res => {
      schoolList.value = res.data.data || []
    })
  }
}

function selectSchool(s: SchoolItem) {
  selectedSchool.value = s
  form.schoolKeyword = s.name
  showSchoolDropdown.value = false
  form.emailPrefix = ''
  form.verifyCode = ''
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.school-dropdown') && !target.closest('.el-form-item')) {
    showSchoolDropdown.value = false
  }
}

async function handleSendCode() {
  if (!fullEmail.value) {
    ElMessage.warning('请先输入邮箱前缀')
    return
  }
  sendingCode.value = true
  try {
    await sendVerifyCode(fullEmail.value)
    ElMessage.success('验证码已发送，请查收邮件')
    sendCodeCooldown.value = 60
    if (cooldownTimer) clearInterval(cooldownTimer)
    cooldownTimer = setInterval(() => {
      sendCodeCooldown.value--
      if (sendCodeCooldown.value <= 0 && cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
  } catch {
    // 错误已由 request 拦截器展示
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  if (!selectedSchool.value) {
    ElMessage.warning('请先选择学校')
    return
  }
  if (!form.emailPrefix.trim()) {
    ElMessage.warning('请输入邮箱前缀')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register({
      email: fullEmail.value,
      verifyCode: form.verifyCode,
      password: form.password,
      nickname: form.nickname,
    })
    await userStore.setAuth(res.data.data)
    ElMessage.success('注册成功，请完善个人资料')
    router.push('/setup-profile')
  } catch {
    // 错误已由 request 拦截器展示
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  searchSchools().then(res => {
    schoolList.value = res.data.data || []
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style lang="scss" scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
}

.auth-left {
  flex: 1;
  background: $primary-gradient;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;

  .brand-area {
    max-width: 420px;
    color: $text-inverse;
  }

  .brand-icon { font-size: 56px; margin-bottom: 16px; }
  .brand-title { font-size: 42px; font-weight: 800; letter-spacing: -1px; margin-bottom: 8px; }
  .brand-subtitle { font-size: 18px; opacity: 0.9; margin-bottom: 48px; }
  .brand-features { display: flex; flex-direction: column; gap: 20px; }
  .feature-item {
    display: flex; align-items: center; gap: 12px; font-size: 16px; opacity: 0.95;
    .feature-icon { font-size: 22px; }
  }
}

.auth-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: $bg-primary;
}

.auth-card { width: 100%; max-width: 400px; position: relative; }
.auth-title { font-size: 28px; font-weight: 700; margin-bottom: 8px; color: $text-primary; }
.auth-desc { font-size: 15px; color: $text-secondary; margin-bottom: 36px; }
.auth-btn { width: 100%; height: 48px; font-size: 16px; }

.school-input-wrap {
  position: relative;
  width: 100%;
}

.school-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  max-height: 200px;
  overflow-y: auto;
  z-index: 100;
}

.school-item {
  padding: 10px 16px;
  cursor: pointer;
  transition: background $transition-fast;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  &:hover { background: $bg-tertiary; }

  .school-domain {
    font-size: 12px;
    color: $text-muted;
  }
}

.school-empty {
  padding: 16px;
  text-align: center;
  color: $text-muted;
  font-size: 14px;
}

.email-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.email-prefix-input {
  flex: 1;
  min-width: 120px;
}

.email-suffix {
  font-size: 14px;
  color: $text-secondary;
  white-space: nowrap;
}

.btn-send-code {
  padding: 8px 16px;
  font-size: 13px;
  border-radius: $radius-md;
  border: 1px solid $primary;
  background: transparent;
  color: $primary;
  cursor: pointer;
  white-space: nowrap;
  transition: all $transition-fast;

  &:hover:not(:disabled) {
    background: $primary;
    color: white;
  }

  &:disabled {
    border-color: $border-light;
    color: $text-muted;
    cursor: not-allowed;
  }
}

.auth-footer {
  text-align: center; margin-top: 24px; font-size: 14px; color: $text-secondary;
  .auth-link { color: $primary; font-weight: 600; margin-left: 4px; &:hover { text-decoration: underline; } }
}
</style>
