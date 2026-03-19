<template>
  <div class="moment-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>匹配配置</span>
          <el-button :loading="loading" @click="loadConfig">刷新</el-button>
        </div>
      </template>

      <el-form label-width="160px" class="config-form">
        <el-form-item label="基础阈值">
          <el-input-number v-model="form.baseThreshold" :min="0" :max="100" />
          <span class="field-hint">所有用户的基础进入阈值。</span>
        </el-form-item>

        <el-form-item label="优先匹配偏移">
          <el-input-number v-model="form.prioritizeOffset" :min="0" :max="50" />
          <span class="field-hint">勾选“优先匹配”时的阈值减免。</span>
        </el-form-item>

        <el-form-item label="优先权单次偏移">
          <el-input-number v-model="form.priorityOffset" :min="0" :max="20" />
          <span class="field-hint">每次未匹配后新增的阈值减免。</span>
        </el-form-item>

        <el-form-item label="优先权最大叠加次数">
          <el-input-number v-model="form.priorityMaxStack" :min="0" :max="10" />
          <span class="field-hint">连续未匹配最多累计多少次。</span>
        </el-form-item>

        <el-divider content-position="left">自动匹配</el-divider>

        <el-form-item label="自动匹配">
          <el-switch v-model="form.autoMatchEnabled" />
          <span class="field-hint">到达设定时间后自动截止报名并触发匹配。</span>
        </el-form-item>

        <el-form-item label="自动匹配日期">
          <el-select v-model="form.autoMatchDayOfWeek" style="width: 180px">
            <el-option
              v-for="item in dayOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <span class="field-hint">建议固定在每周同一时间，便于用户形成预期。</span>
        </el-form-item>

        <el-form-item label="自动匹配时间">
          <el-time-select
            v-model="form.autoMatchTime"
            start="00:00"
            step="00:30"
            end="23:30"
            format="HH:mm"
            placeholder="选择时间"
          />
          <span class="field-hint">使用 24 小时制，系统到点自动截止报名并执行匹配。</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMomentMatchConfig, updateMomentMatchConfig } from '@/api/adminApi'

const loading = ref(false)
const saving = ref(false)
const dayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]
const form = reactive({
  baseThreshold: 60,
  prioritizeOffset: 10,
  priorityOffset: 5,
  priorityMaxStack: 2,
  autoMatchEnabled: false,
  autoMatchDayOfWeek: 5,
  autoMatchTime: '20:00',
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getMomentMatchConfig()
    const data = res.data.data
    if (data) {
      form.baseThreshold = data.baseThreshold
      form.prioritizeOffset = data.prioritizeOffset
      form.priorityOffset = data.priorityOffset
      form.priorityMaxStack = data.priorityMaxStack
      form.autoMatchEnabled = data.autoMatchEnabled
      form.autoMatchDayOfWeek = data.autoMatchDayOfWeek
      form.autoMatchTime = data.autoMatchTime
    }
  } catch {
    ElMessage.error('加载匹配配置失败')
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    await updateMomentMatchConfig({ ...form })
    ElMessage.success('匹配配置已保存')
    await loadConfig()
  } catch {
    ElMessage.error('保存匹配配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style lang="scss" scoped>
.moment-config {
  max-width: 820px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.config-form {
  padding-top: 8px;
}

.field-hint {
  margin-left: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
