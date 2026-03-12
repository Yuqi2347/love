<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="brand-area">
        <img src="/logo.png" alt="Campal" class="auth-logo" />
        <h1 class="brand-title">Campal</h1>
        <p class="brand-subtitle">加入校园交友，开启缘分之旅</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">🎓</span>
            <span>邮箱认证，安全可靠</span>
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
        <p class="auth-desc">使用邮箱注册，不限制后缀</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleRegister">
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="昵称" prefix-icon="User" />
          </el-form-item>

          <el-form-item prop="school" label="学校">
            <el-select
              v-model="form.school"
              filterable
              remote
              placeholder="搜索或选择学校"
              :remote-method="searchSchools"
              :loading="schoolLoading"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="s in schoolOptions"
                :key="s.name"
                :label="s.name"
                :value="s.name"
              />
            </el-select>
          </el-form-item>

          <!-- 邮箱输入 -->
          <el-form-item prop="email" label="邮箱">
            <div class="email-input-wrapper">
              <el-input
                v-model="form.email"
                placeholder="请输入完整邮箱地址"
                prefix-icon="Message"
                class="email-full-input"
              />
              <button
                type="button"
                class="btn-send-code"
                :disabled="sendCodeCooldown > 0 || sendingCode || !isEmailValid"
                @click="handleSendCode"
              >
                {{ sendCodeCooldown > 0 ? `${sendCodeCooldown}s后重发` : (sendingCode ? '发送中...' : '发送验证码') }}
              </button>
            </div>
          </el-form-item>

          <!-- 验证码输入 -->
          <el-form-item prop="verifyCode">
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
import { ref, reactive, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { register, sendVerifyCode, searchSchools as searchSchoolsApi, type SchoolItem } from '@/api/authApi'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const sendCodeCooldown = ref(0)
const sendingCode = ref(false)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  nickname: '',
  school: '',
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: '',
})

const schoolOptions = ref<SchoolItem[]>([])
const schoolLoading = ref(false)

function checkEmailSuffix(email: string, suffix: string): boolean {
  const domain = email.split('@')[1]?.toLowerCase() || ''
  const s = suffix.trim().toLowerCase()
  return domain === s || domain.endsWith('.' + s)
}

const isEmailValid = computed(() => {
  const email = form.email?.trim() || ''
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return false
  if (!form.school?.trim()) return false
  const school = schoolOptions.value.find(s => s.name === form.school.trim())
  if (!school?.emailSuffix) return true
  return checkEmailSuffix(email, school.emailSuffix)
})

async function searchSchools(query: string) {
  if (!query || query.length < 2) {
    schoolOptions.value = []
    return
  }
  schoolLoading.value = true
  try {
    const res = await searchSchoolsApi(query)
    schoolOptions.value = res.data.data || []
  } catch {
    schoolOptions.value = []
  } finally {
    schoolLoading.value = false
  }
}

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
  value: string,
  callback: (err?: Error) => void
) => {
  const email = (value || form.email || '').trim()
  if (!email) {
    callback(new Error('请输入邮箱'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    callback(new Error('请输入正确的邮箱格式'))
    return
  }
  if (form.school?.trim()) {
    const school = schoolOptions.value.find(s => s.name === form.school.trim())
    if (school?.emailSuffix && !checkEmailSuffix(email, school.emailSuffix)) {
      callback(new Error(`请使用该校邮箱（@${school.emailSuffix}）`))
      return
    }
  }
  callback()
}

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: ['blur', 'change'] },
  ],
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

async function handleSendCode() {
  const email = form.email.trim()
  if (!email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }
  sendingCode.value = true
  try {
    await sendVerifyCode(email)
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
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register({
      email: form.email.trim(),
      verifyCode: form.verifyCode,
      password: form.password,
      nickname: form.nickname,
      school: form.school?.trim() || undefined,
    })
    if (form.school?.trim()) {
      sessionStorage.setItem('register_school', form.school.trim())
    }
    await userStore.setAuth(res.data.data)
    ElMessage.success('注册成功，请完善个人资料')
    router.push('/setup-profile')
  } catch {
    // 错误已由 request 拦截器展示
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
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

  .auth-logo { width: 64px; height: 64px; object-fit: contain; margin-bottom: 16px; }
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

.email-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.email-full-input {
  flex: 1;
  min-width: 120px;
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
