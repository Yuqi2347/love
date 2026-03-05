<template>
  <div class="invite-wait-page">
    <div class="page-header">
      <button class="btn-text" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <h1 class="page-title">等待邀约</h1>
      <div></div>
    </div>

    <div class="page-description">
      <p>创建等待邀约后，当有符合条件的邀约发布时，我们会通知你</p>
    </div>

    <div class="form-container">
      <div class="form-section">
        <h3 class="section-title">感兴趣的邀约类型</h3>
        <p class="form-hint">选择你想参与的邀约类型</p>
        <div class="type-select">
          <button
            v-for="type in INVITE_TYPE_OPTIONS"
            :key="type.value"
            :class="['type-btn', { active: form.inviteTypes.includes(type.value) }]"
            @click="toggleType(type.value)"
          >
            <span class="type-icon">{{ type.icon }}</span>
            <span class="type-label">{{ type.label }}</span>
          </button>
        </div>
      </div>

      <div class="form-section">
        <h3 class="section-title">时间偏好</h3>
        <el-date-picker
          v-model="selectedDates"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm"
          class="full-width"
        />
      </div>

      <div class="form-section">
        <label class="form-label">地点偏好</label>
        <el-input v-model="form.locationPref" placeholder="例如：图书馆、操场、食堂" />
      </div>

      <div class="form-section">
        <label class="form-label">有效期</label>
        <el-select v-model="form.expireHours" class="full-width">
          <el-option
            v-for="opt in WAIT_DURATION_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <p class="form-hint">等待邀约的有效时间，过期后自动失效</p>
      </div>

      <div class="form-section">
        <label class="flex-label">
          <span>自动受邀</span>
          <el-switch v-model="form.autoAccept" />
        </label>
        <p class="form-hint">开启后，有符合条件的邀约将自动加入</p>
      </div>

      <div class="form-section">
        <label class="form-label">备注</label>
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="添加一些备注信息..."
          maxlength="200"
          show-word-limit
        />
      </div>

      <div class="form-actions">
        <button class="btn-primary" :disabled="!canSubmit" @click="handleSubmit">
          创建等待邀约
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { createInviteWait, type InviteWaitCreateRequest } from '@/api/inviteApi'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  InviteType,
  INVITE_TYPE_OPTIONS,
  WAIT_DURATION_OPTIONS,
} from '@/constants/inviteConst'

const router = useRouter()

const form = ref<InviteWaitCreateRequest>({
  inviteTypes: [],
  periodConfig: '',
  locationPref: '',
  autoAccept: false,
  expireHours: 6,
  remark: '',
})

const selectedDates = ref<[Date, Date] | null>(null)

// 检查是否可以提交
const canSubmit = computed(() => {
  return form.value.inviteTypes.length > 0
})

// 切换类型
function toggleType(type: string) {
  const index = form.value.inviteTypes.indexOf(type)
  if (index > -1) {
    form.value.inviteTypes.splice(index, 1)
  } else {
    if (form.value.inviteTypes.length < 3) {
      form.value.inviteTypes.push(type)
    } else {
      ElMessage.warning('最多选择3种类型')
    }
  }
}

// 提交表单
async function handleSubmit() {
  // 构建周期配置
  let periodConfig = ''
  if (selectedDates.value) {
    periodConfig = JSON.stringify({
      start: selectedDates.value[0].toISOString(),
      end: selectedDates.value[1].toISOString(),
    })
  }

  const submitData: InviteWaitCreateRequest = {
    ...form.value,
    periodConfig,
  }

  try {
    ElMessage.info('创建中...')
    const res = await createInviteWait(submitData)
    ElMessage.success('等待邀约创建成功')
    router.push('/invite')
  } catch (error) {
    // Error handled by interceptor
  }
}
</script>

<style lang="scss" scoped>
.invite-wait-page { padding: 20px; max-width: 500px; margin: 0 auto; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  background: $primary-gradient;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-description {
  text-align: center;
  padding: 16px;
  margin-bottom: 24px;
  background: rgba($primary, 0.05);
  border-radius: $radius-lg;
  border: 1px solid rgba($primary, 0.1);

  p {
    font-size: 14px;
    color: $text-secondary;
    margin: 0;
  }
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: $text-primary;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.flex-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-hint {
  font-size: 12px;
  color: $text-muted;
  margin: -4px 0 0 0;
}

.full-width { width: 100%; }

// 类型选择
.type-select {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.type-btn {
  padding: 16px 12px;
  border: 2px solid $border-light;
  border-radius: $radius-md;
  background: $bg-primary;
  cursor: pointer;
  transition: all $transition-fast;
  text-align: center;

  &:hover {
    border-color: rgba($primary, 0.3);
  }

  &.active {
    border-color: $primary;
    background: rgba($primary, 0.08);
  }
}

.type-icon {
  display: block;
  font-size: 24px;
  margin-bottom: 6px;
}

.type-label {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
}

// 表单操作
.form-actions {
  margin-top: 16px;
}

.btn-primary {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.btn-text {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition: color $transition-fast;

  &:hover { color: $primary; }
}
</style>
