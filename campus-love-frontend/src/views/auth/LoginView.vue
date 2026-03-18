<template>
  <div class="auth-page">
    <!-- Left: Brand showcase -->
    <div class="auth-left">
      <!-- Animated background -->
      <div class="bg-layer">
        <div class="mesh-gradient"></div>
        <div v-for="i in 80" :key="i" class="star" :style="starStyle(i)"></div>
        <!-- Shooting stars -->
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

    <!-- Right: Login form -->
    <div class="auth-right">
      <div class="auth-card">
        <div class="auth-header">
          <span class="auth-brand">Campal</span>
          <h2 class="auth-title">欢迎回来</h2>
          <p class="auth-desc">登录你的校园交友账号</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="校园邮箱" prefix-icon="Message" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <button type="submit" class="login-btn" :disabled="loading">
              <span v-if="!loading">登 录</span>
              <span v-else class="loading-text">
                <span class="spinner"></span>
                登录中...
              </span>
            </button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          还没有账号？<router-link to="/register" class="auth-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { login, getPublicStats } from '@/api/authApi'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const activeUserCount = ref(0)

const activeUserCountDisplay = computed(() => {
  const n = activeUserCount.value
  if (n >= 1000) return `${Math.floor(n / 1000)}000+`
  if (n > 0) return `${n}+`
  return '-'
})

onMounted(async () => {
  try {
    const res = await getPublicStats()
    if (res.data?.data?.activeUserCount != null) {
      activeUserCount.value = res.data.data.activeUserCount
    }
  } catch {
    // 忽略，展示默认值
  }
})

const features = [
  { icon: '🎯', title: '多维匹配', desc: 'MBTI · 星座 · 八字精准推荐' },
  { icon: '💬', title: '即时社交', desc: '聊天 · 邀约 · 渐进信任' },
  { icon: '🛡️', title: '安全可信', desc: '校园认证 · 隐私保护' },
]

const form = reactive({ email: '', password: '' })
const rules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function starStyle(i: number) {
  // Pseudo-random distribution using prime multipliers
  const left = ((i * 73 + 17) % 1000) / 10
  const top = ((i * 47 + 31) % 1000) / 10
  const delay = ((i * 37) % 50) / 10 // 0-5s
  const dur = 2 + ((i * 13) % 30) / 10 // 2-5s

  // 3 tiers: dim (60%), medium (30%), bright (10%)
  const tier = i % 10
  let size: number, opacity: number, color: string
  if (tier === 0) {
    // Bright star — warm white with glow
    size = 2.5 + (i % 3)
    opacity = 0.9
    color = '#E8EAFF'
  } else if (tier < 4) {
    // Medium star
    size = 1.5 + (i % 2)
    opacity = 0.5 + (i % 3) * 0.1
    color = '#C8CDFF'
  } else {
    // Dim star — tiny
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

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    await userStore.setAuth(res.data.data)
    ElMessage.success('登录成功')
    if (!userStore.user?.profileComplete) {
      router.push('/setup-profile')
    } else {
      const school = encodeURIComponent(userStore.user?.school || '')
      router.push(school ? `/welcome?school=${school}` : '/welcome')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.auth-page {
  display: flex;
  min-height: 100vh;
}

// === Left Panel ===
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

// Stars
.star {
  position: absolute;
  border-radius: 50%;
  animation: star-flicker ease-in-out infinite;
}

@keyframes star-flicker {
  0%, 100% { opacity: var(--o, 0.3); transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

// Shooting stars
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

// Brand content
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

// Features
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

// Stats
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
  max-width: 380px;
  animation: card-in 0.5s ease 0.2s both;
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-header {
  margin-bottom: 32px;
}

.auth-brand {
  font-family: 'Pacifico', cursive;
  font-size: 20px;
  color: $primary;
  display: none; // 桌面端左侧已有，移动端显示
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 6px;
  color: $text-primary;
}

.auth-desc {
  font-size: 15px;
  color: $text-muted;
}

// Login button
.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: $radius-full;
  background: $primary;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all $transition-base;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
    transform: translateX(-100%);
    transition: transform 0.5s;
  }

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: $shadow-glow;

    &::before { transform: translateX(100%); }
  }

  &:active:not(:disabled) {
    transform: translateY(0) scale(0.98);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

.loading-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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

// === Mobile ===
@media (max-width: $bp-mobile) {
  .auth-left { display: none; }
  .auth-page { flex-direction: column; }
  .auth-right { flex: 1; min-height: 100vh; padding: 40px 24px; }
  .auth-brand { display: block; margin-bottom: 16px; }
}

@media (max-width: $bp-tablet) and (min-width: $bp-mobile) {
  .auth-left { padding: 40px; }
  .auth-right { padding: 40px; }
  .brand-title { font-size: 36px; }
  .brand-stats { gap: 12px; padding: 16px; }
}
</style>
