<template>
  <div class="auth-page">
    <!-- Left: Brand showcase（与登录页一致） -->
    <div class="auth-left">
      <div class="bg-layer">
        <div class="mesh-gradient"></div>
        <div v-for="i in 80" :key="i" class="star" :style="starStyle(i)"></div>
        <div class="shooting-star s1"></div>
        <div class="shooting-star s2"></div>
        <div class="shooting-star s3"></div>
        <div class="glow-orb orb-1"></div>
        <div class="glow-orb orb-2"></div>
      </div>

      <div class="brand-area">
        <h1 class="brand-title">Campal</h1>
        <p class="brand-subtitle">遇见TA，从校园开始</p>

        <div class="brand-features">
          <div v-for="(f, i) in features" :key="i" class="feature-item" :style="{ animationDelay: `${0.3 + i * 0.15}s` }">
            <div class="feature-icon-wrap">
              <span class="feature-icon">{{ f.icon }}</span>
            </div>
            <div class="feature-text">
              <span class="feature-label">{{ f.title }}</span>
              <span class="feature-desc">{{ f.desc }}</span>
            </div>
          </div>
        </div>

        <div class="brand-stats">
          <div class="stat-item stat-item-single">
            <span class="stat-num">{{ activeUserCountDisplay }}</span>
            <span class="stat-label">活跃用户</span>
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
            <el-input v-model="form.nickname" placeholder="昵称（最多10字）" prefix-icon="User" maxlength="10" show-word-limit />
          </el-form-item>

          <el-form-item prop="school" label="学校">
            <el-select
              v-model="form.school"
              filterable
              remote
              placeholder="搜索或选择学校"
              :remote-method="onSchoolRemoteQuery"
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
        <div class="auth-agreement">
          <el-checkbox v-model="agreed">
            我已阅读并同意
            <router-link class="auth-legal-link" to="/legal/user-agreement" target="_blank">《用户协议》</router-link>
            与
            <router-link class="auth-legal-link" to="/legal/privacy-policy" target="_blank">《隐私政策》</router-link>
          </el-checkbox>
          <div class="auth-legal-links">
            <router-link class="auth-legal-link" to="/legal/personal-info-list" target="_blank">个人信息收集清单</router-link>
            <router-link class="auth-legal-link" to="/legal/third-party-sharing" target="_blank">第三方信息共享清单</router-link>
            <router-link class="auth-legal-link" to="/legal/feature-description" target="_blank">功能说明</router-link>
          </div>
        </div>
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
import { register, sendVerifyCode, searchSchools as searchSchoolsApi, getPublicStats, type SchoolItem } from '@/api/authApi'
import { SCHOOLS_FALLBACK, filterSchoolsByKeyword } from '@/constants/schoolsFallback'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const sendCodeCooldown = ref(0)
const activeUserCount = ref(0)
const agreed = ref(false)

const activeUserCountDisplay = computed(() => {
  const n = activeUserCount.value
  if (n >= 1000) return `${Math.floor(n / 1000)}000+`
  if (n > 0) return `${n}+`
  return '-'
})

onMounted(async () => {
  await Promise.all([
    (async () => {
      try {
        const res = await getPublicStats()
        if (res.data?.data?.activeUserCount != null) {
          activeUserCount.value = res.data.data.activeUserCount
        }
      } catch {
        /* 忽略 */
      }
    })(),
    (async () => {
      try {
        const res = await searchSchoolsApi()
        prefetchedSchools.value = res.data.data ?? []
        schoolOptions.value =
          prefetchedSchools.value.length > 0 ? prefetchedSchools.value : SCHOOLS_FALLBACK
      } catch {
        prefetchedSchools.value = SCHOOLS_FALLBACK
        schoolOptions.value = SCHOOLS_FALLBACK
      }
    })(),
  ])
})

const features = [
  { icon: '🎯', title: '多维匹配', desc: 'MBTI · 星座 · 八字精准推荐' },
  { icon: '💬', title: '即时社交', desc: '聊天 · 邀约 · 渐进信任' },
  { icon: '🛡️', title: '安全可信', desc: '校园认证 · 隐私保护' },
]

function starStyle(i: number) {
  const left = ((i * 73 + 17) % 1000) / 10
  const top = ((i * 47 + 31) % 1000) / 10
  const delay = ((i * 37) % 50) / 10
  const dur = 2 + ((i * 13) % 30) / 10
  const tier = i % 10
  let size: number, opacity: number, color: string
  if (tier === 0) {
    size = 2.5 + (i % 3)
    opacity = 0.9
    color = '#E8EAFF'
  } else if (tier < 4) {
    size = 1.5 + (i % 2)
    opacity = 0.5 + (i % 3) * 0.1
    color = '#C8CDFF'
  } else {
    size = 0.8 + (i % 2) * 0.5
    opacity = 0.2 + (i % 4) * 0.05
    color = '#A0A8D0'
  }
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    opacity,
    background: color,
    animationDelay: `${delay}s`,
    animationDuration: `${dur}s`,
    boxShadow: tier === 0 ? `0 0 ${size * 2}px ${color}` : 'none',
  }
}
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
const prefetchedSchools = ref<SchoolItem[]>([])
let schoolSearchTimer: ReturnType<typeof setTimeout> | null = null

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

/** 减少 el-select 远程搜索的连发请求，降低连接被中间层关闭的概率 */
function onSchoolRemoteQuery(query: string) {
  if (schoolSearchTimer) {
    clearTimeout(schoolSearchTimer)
    schoolSearchTimer = null
  }
  const q = (query || '').trim()
  if (q.length < 2) {
    schoolOptions.value =
      prefetchedSchools.value.length > 0 ? prefetchedSchools.value : SCHOOLS_FALLBACK
    return
  }
  schoolSearchTimer = setTimeout(() => {
    schoolSearchTimer = null
    void fetchSchoolOptions(q)
  }, 320)
}

async function fetchSchoolOptions(query: string) {
  if (query.length < 2) return
  schoolLoading.value = true
  try {
    const res = await searchSchoolsApi(query)
    let list = res.data.data ?? []
    if (!list.length) list = filterSchoolsByKeyword(query)
    schoolOptions.value = list
  } catch {
    schoolOptions.value = filterSchoolsByKeyword(query)
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
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 10, message: '昵称最多10个字符', trigger: 'blur' },
  ],
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
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意用户协议与隐私政策')
    return
  }

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
  if (schoolSearchTimer) clearTimeout(schoolSearchTimer)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.auth-page {
  display: flex;
  min-height: 100vh;
}

// === Left Panel（与登录页一致）===
.auth-left {
  flex: 1.1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  overflow: hidden;
  background: linear-gradient(135deg, #070B14 0%, #0D1321 35%, #111827 65%, #0F172A 100%);
}

.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.mesh-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 15% 60%, rgba(79, 70, 229, 0.1) 0%, transparent 45%),
    radial-gradient(ellipse at 75% 20%, rgba(6, 182, 212, 0.06) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 80%, rgba(139, 92, 246, 0.07) 0%, transparent 45%),
    radial-gradient(ellipse at 85% 70%, rgba(59, 130, 246, 0.05) 0%, transparent 35%);
  animation: nebula-shift 20s ease-in-out infinite alternate;
}

@keyframes nebula-shift {
  0% { filter: blur(0px); opacity: 1; }
  50% { filter: blur(2px); opacity: 0.8; }
  100% { filter: blur(0px); opacity: 1; }
}

.star {
  position: absolute;
  border-radius: 50%;
  animation: star-flicker ease-in-out infinite;
}

@keyframes star-flicker {
  0%, 100% { opacity: var(--o, 0.3); transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

.shooting-star {
  position: absolute;
  width: 80px;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.8), transparent);
  border-radius: 1px;
  opacity: 0;
  animation: shoot ease-in infinite;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: -1px;
    width: 4px;
    height: 3px;
    border-radius: 50%;
    background: white;
    box-shadow: 0 0 6px 2px rgba(200, 210, 255, 0.6);
  }
}

.s1 {
  top: 15%;
  left: 60%;
  transform: rotate(-35deg);
  animation-duration: 6s;
  animation-delay: 1s;
}

.s2 {
  top: 40%;
  left: 30%;
  transform: rotate(-30deg);
  animation-duration: 8s;
  animation-delay: 4s;
  width: 60px;
}

.s3 {
  top: 65%;
  left: 70%;
  transform: rotate(-40deg);
  animation-duration: 7s;
  animation-delay: 7s;
  width: 50px;
}

@keyframes shoot {
  0% { opacity: 0; transform: rotate(var(--r, -35deg)) translateX(0); }
  2% { opacity: 1; }
  8% { opacity: 0; transform: rotate(var(--r, -35deg)) translateX(-200px); }
  100% { opacity: 0; }
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: orb-float 8s ease-in-out infinite;
}

.orb-1 {
  width: 250px;
  height: 250px;
  top: 10%;
  left: -5%;
  background: rgba($primary, 0.15);
}

.orb-2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  right: -5%;
  background: rgba($accent, 0.1);
  animation-delay: -4s;
}

@keyframes orb-float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -15px) scale(1.1); }
}

.brand-area {
  position: relative;
  z-index: 1;
  max-width: 420px;
  animation: brand-in 0.8s ease both;
}

@keyframes brand-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.brand-title {
  font-family: 'Pacifico', cursive;
  font-size: 48px;
  font-weight: 400;
  color: white;
  margin-bottom: 8px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 48px;
  letter-spacing: 1px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 48px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0;
  animation: feature-slide 0.5s ease forwards;
}

@keyframes feature-slide {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

.feature-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-icon {
  font-size: 20px;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-label {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.feature-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}

.brand-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: $radius-lg;
  backdrop-filter: blur(8px);
  animation: stats-in 0.6s ease 0.8s both;
}

@keyframes stats-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.stat-num {
  font-size: 22px;
  font-weight: 800;
  color: white;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  white-space: nowrap;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
}

// === Right Panel ===
.auth-right {
  flex: 0.9;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: $bg-primary;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  position: relative;
  animation: card-in 0.5s ease 0.2s both;
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
  color: $text-primary;
}

.auth-desc {
  font-size: 15px;
  color: $text-secondary;
  margin-bottom: 36px;
}

.auth-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

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
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: $text-muted;

  .auth-link {
    color: $primary;
    font-weight: 600;
    margin-left: 4px;
    transition: color $transition-fast;

    &:hover { color: $primary-dark; }
  }
}

.auth-agreement {
  margin-top: 6px;
  font-size: 13px;
  color: $text-muted;
}

.auth-legal-links {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  padding-left: 24px;
}

.auth-legal-link {
  color: $primary;
  text-decoration: none;

  &:hover {
    color: $primary-dark;
  }
}

// === Mobile（与登录页一致）===
@media (max-width: $bp-mobile) {
  .auth-left { display: none; }
  .auth-page { flex-direction: column; }
  .auth-right { flex: 1; min-height: 100vh; padding: 40px 24px; }
}

@media (max-width: $bp-tablet) and (min-width: $bp-mobile) {
  .auth-left { padding: 40px; }
  .auth-right { padding: 40px; }
  .brand-title { font-size: 36px; }
  .brand-stats { gap: 12px; padding: 16px; }
}
</style>
