<template>
  <div class="invite-wait-create-page">
    <header class="page-header card-shell">
      <button type="button" class="btn-text" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="header-center">
        <h1>开启等待雷达</h1>
        <p>选择感兴趣的邀约类型与时段，系统会帮你匹配公开邀约；也可开启自动上车。</p>
      </div>
    </header>

    <div class="form-grid">
      <section class="card-shell">
        <h3 class="section-title">想等的类型（至少选一项）</h3>
        <div class="type-grid">
          <button
            v-for="opt in INVITE_TYPE_OPTIONS"
            :key="opt.value"
            type="button"
            :class="['type-btn', { active: selectedTypes.includes(opt.value) }]"
            @click="toggleType(opt.value)"
          >
            <span class="type-icon">{{ opt.icon }}</span>
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </section>

      <section class="card-shell">
        <h3 class="section-title">时段预期（选填）</h3>
        <el-date-picker
          v-model="periodRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          class="w-full"
        />
        <p class="field-hint">不填则不限定时段，仅按类型匹配。</p>
      </section>

      <section class="card-shell">
        <h3 class="section-title">地点预期（选填）</h3>
        <el-input v-model="locationPref" placeholder="例如：图书馆附近、东门商圈" clearable />
      </section>

      <section class="card-shell">
        <h3 class="section-title">雷达有效期</h3>
        <el-select v-model="expireHours" class="w-full" placeholder="选择有效时长">
          <el-option label="6 小时" :value="6" />
          <el-option label="12 小时" :value="12" />
          <el-option label="24 小时" :value="24" />
          <el-option label="48 小时" :value="48" />
          <el-option label="72 小时" :value="72" />
          <el-option label="7 天" :value="168" />
        </el-select>
      </section>

      <section class="card-shell">
        <div class="switch-row">
          <div>
            <div class="switch-title">自动上车</div>
            <p class="field-hint m-0">开启后，会尝试匹配当前符合条件的公开邀约并自动加入。</p>
          </div>
          <el-switch v-model="autoAccept" />
        </div>
      </section>

      <section class="card-shell">
        <h3 class="section-title">备注（选填）</h3>
        <el-input v-model="remark" type="textarea" :rows="3" placeholder="补充说明，便于自己回顾" maxlength="200" show-word-limit />
      </section>
    </div>

    <footer class="action-bar card-shell">
      <button class="btn-primary" :disabled="!canSubmit || submitting" @click="handleSubmit">
        {{ submitting ? '提交中…' : '开启雷达' }}
      </button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createInviteWait } from '@/api/inviteApi'
import { InviteType, INVITE_TYPE_OPTIONS } from '@/constants/inviteConst'

defineOptions({ name: 'InviteWaitCreate' })

const router = useRouter()

const selectedTypes = ref<InviteType[]>([])
const periodRange = ref<[Date, Date] | null>(null)
const locationPref = ref('')
const expireHours = ref<number>(24)
const autoAccept = ref(false)
const remark = ref('')
const submitting = ref(false)

const canSubmit = computed(() => selectedTypes.value.length > 0 && typeof expireHours.value === 'number')

function toggleType(t: InviteType) {
  const i = selectedTypes.value.indexOf(t)
  if (i === -1) selectedTypes.value = [...selectedTypes.value, t]
  else selectedTypes.value = selectedTypes.value.filter((x) => x !== t)
}

function buildPeriodConfig(): string | undefined {
  const r = periodRange.value
  if (!r || r.length !== 2) return undefined
  const [a, b] = r
  return JSON.stringify({ start: a.toISOString(), end: b.toISOString() })
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const periodConfig = buildPeriodConfig()
    await createInviteWait({
      inviteTypes: selectedTypes.value,
      periodConfig,
      locationPref: locationPref.value.trim() || undefined,
      autoAccept: autoAccept.value,
      expireHours: expireHours.value,
      remark: remark.value.trim() || undefined,
    })
    ElMessage.success('雷达已开启')
    await router.replace({ path: '/invite', query: { tab: 'wait' } })
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message ?? err?.message ?? '开启失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.invite-wait-create-page {
  max-width: 560px;
  margin: 0 auto;
  padding: 16px 16px 96px;
}

.card-shell {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 10px 40px rgba(148, 163, 184, 0.12);
}

.page-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.btn-text {
  border: none;
  background: transparent;
  padding: 8px;
  cursor: pointer;
  color: #64748b;
  border-radius: 12px;
  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: #1e293b;
  }
}

.header-center h1 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 800;
  color: #1e293b;
}

.header-center p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.section-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
}

.type-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.type-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
  &.active {
    border-color: #ff3366;
    background: linear-gradient(135deg, rgba(255, 51, 102, 0.12), rgba(79, 140, 255, 0.1));
    color: #c0264a;
    box-shadow: 0 4px 14px rgba(255, 51, 102, 0.15);
  }
}

.type-icon {
  font-size: 16px;
}

.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.switch-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.switch-title {
  font-weight: 800;
  font-size: 15px;
  color: #1e293b;
  margin-bottom: 4px;
}

.action-bar {
  position: sticky;
  bottom: 12px;
  z-index: 10;
}

.btn-primary {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff3366, #ff7b54);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(255, 51, 102, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 12px 28px rgba(255, 51, 102, 0.4);
  }
  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.w-full {
  width: 100%;
}
</style>
