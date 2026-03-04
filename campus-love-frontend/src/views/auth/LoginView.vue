<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="brand-area">
        <div class="brand-icon">💕</div>
        <h1 class="brand-title">Campus Love</h1>
        <p class="brand-subtitle">遇见TA，从校园开始</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">🎯</span>
            <span>MBTI · 星座 · 八字 多维精准匹配</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">💬</span>
            <span>即时聊天 · 渐进信任社交</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🌟</span>
            <span>校园认证 · 真实安全</span>
          </div>
        </div>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-card">
        <h2 class="auth-title">欢迎回来</h2>
        <p class="auth-desc">登录你的校园交友账号</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="校园邮箱" prefix-icon="Message" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item>
            <button type="submit" class="btn-primary auth-btn" :disabled="loading">
              {{ loading ? '登录中...' : '登 录' }}
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { login } from '@/api/authApi'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ email: '', password: '' })
const rules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    await userStore.setAuth(res.data.data)
    ElMessage.success('登录成功')
    if (!res.data.data.profileComplete) {
      router.push('/setup-profile')
    } else {
      router.push('/')
    }
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

  .brand-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }

  .brand-title {
    font-size: 42px;
    font-weight: 800;
    letter-spacing: -1px;
    margin-bottom: 8px;
  }

  .brand-subtitle {
    font-size: 18px;
    opacity: 0.9;
    margin-bottom: 48px;
  }

  .brand-features {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .feature-item {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 16px;
    opacity: 0.95;

    .feature-icon {
      font-size: 22px;
    }
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

.auth-card {
  width: 100%;
  max-width: 400px;
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

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: $text-secondary;

  .auth-link {
    color: $primary;
    font-weight: 600;
    margin-left: 4px;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
