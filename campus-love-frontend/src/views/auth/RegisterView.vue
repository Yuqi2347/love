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
            <span>校园邮箱认证，安全可靠</span>
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
        <p class="auth-desc">使用校园邮箱（.edu.cn）注册</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleRegister">
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="昵称" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="校园邮箱 xxx@xxx.edu.cn" prefix-icon="Message" />
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { register } from '@/api/authApi'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ nickname: '', email: '', password: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^@]+@[^@]+\.edu\.cn$/, message: '请使用校园邮箱(.edu.cn)', trigger: 'blur' },
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

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register({ email: form.email, password: form.password, nickname: form.nickname })
    userStore.setAuth(res.data.data)
    ElMessage.success('注册成功，请完善个人资料')
    router.push('/setup-profile')
  } finally {
    loading.value = false
  }
}
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

.auth-card { width: 100%; max-width: 400px; }
.auth-title { font-size: 28px; font-weight: 700; margin-bottom: 8px; color: $text-primary; }
.auth-desc { font-size: 15px; color: $text-secondary; margin-bottom: 36px; }
.auth-btn { width: 100%; height: 48px; font-size: 16px; }
.auth-footer {
  text-align: center; margin-top: 24px; font-size: 14px; color: $text-secondary;
  .auth-link { color: $primary; font-weight: 600; margin-left: 4px; &:hover { text-decoration: underline; } }
}
</style>
