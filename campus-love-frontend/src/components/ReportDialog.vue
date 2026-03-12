<template>
  <el-dialog
    :model-value="modelValue"
    title="举报"
    width="400px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="report-form">
      <div class="report-section">
        <div class="report-label">违规类型 <span class="required">*</span></div>
        <div class="violation-options">
          <label v-for="opt in VIOLATION_TYPES" :key="opt.value" class="violation-option">
            <input v-model="selectedTypes" type="checkbox" :value="opt.value" />
            <span>{{ opt.label }}</span>
          </label>
        </div>
      </div>
      <div class="report-section">
        <div class="report-label">举报理由（选填）</div>
        <el-input
          v-model="reason"
          type="textarea"
          :rows="3"
          placeholder="补充说明..."
          maxlength="500"
          show-word-limit
        />
      </div>
    </div>
    <template #footer>
      <button class="btn-outline" @click="$emit('update:modelValue', false)">取消</button>
      <button
        class="btn-primary"
        :disabled="!selectedTypes.length || submitting"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '提交举报' }}
      </button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { VIOLATION_TYPES } from '@/api/reportApi'
import { submitReport } from '@/api/reportApi'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  targetType: string
  targetId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [v: boolean]
  success: []
}>()

const selectedTypes = ref<string[]>([])
const reason = ref('')
const submitting = ref(false)

watch(() => props.modelValue, (v) => {
  if (v) {
    selectedTypes.value = []
    reason.value = ''
  }
})

async function handleSubmit() {
  if (!selectedTypes.value.length) return
  submitting.value = true
  try {
    await submitReport({
      targetType: props.targetType,
      targetId: props.targetId,
      violationTypes: selectedTypes.value,
      reason: reason.value.trim() || undefined,
    })
    ElMessage.success('举报已提交')
    emit('update:modelValue', false)
    emit('success')
  } catch {
    ElMessage.error('举报失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.report-form {
  padding: 8px 0;
}

.report-section {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.report-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 10px;

  .required {
    color: var(--el-color-danger);
  }
}

.violation-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.violation-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-regular);

  input {
    width: 16px;
    height: 16px;
  }
}
</style>
