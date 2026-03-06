<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">管理后台</h1>
      <p class="subtitle">校园交友 · 仅管理员登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" class="form">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="管理员邮箱" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ email: '', password: '' })
const rules: FormRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    await userStore.login(form.email, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-secondary;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: $bg-primary;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

  .title { font-size: 24px; font-weight: 700; text-align: center; margin-bottom: 8px; }
  .subtitle { font-size: 14px; color: $text-muted; text-align: center; margin-bottom: 24px; }
  .form { margin-top: 24px; }
  .submit-btn { width: 100%; }
}
</style>
