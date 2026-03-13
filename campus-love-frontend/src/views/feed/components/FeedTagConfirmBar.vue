<template>
  <Transition name="tag-bar">
    <div v-if="visible" class="feed-tag-bar">
      <div class="tag-bar-content">
        <span class="tag-bar-label">AI 标签：</span>
        <span v-if="!editing" class="tag-bar-tags">{{ displayTags }}</span>
        <input
          v-else
          ref="editInput"
          v-model="editValue"
          class="tag-bar-input"
          placeholder="逗号分隔的标签"
          @blur="saveEdit"
          @keydown.enter="saveEdit"
        />
        <button v-if="!editing && isOwner" class="tag-bar-edit" @click="startEdit">修改</button>
      </div>
      <button v-if="visible" class="tag-bar-close" @click="dismiss" aria-label="关闭">
        <el-icon :size="14"><Close /></el-icon>
      </button>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { updatePostAiTags } from '@/api/feedApi'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  postId: number
  aiTags: string | null | undefined
  isOwner: boolean
  /** 自动消失秒数，0 表示不自动消失 */
  autoDismissMs?: number
}>()

const emit = defineEmits<{
  (e: 'update:aiTags', value: string): void
}>()

const visible = ref(true)
const editing = ref(false)
const editValue = ref('')
const editInput = ref<HTMLInputElement | null>(null)

const displayTags = computed(() => {
  const t = props.aiTags
  if (!t || !t.trim()) return '未生成'
  return t.replace(/[,，]/g, '、')
})

const autoDismissMs = computed(() => props.autoDismissMs ?? 5000)

let dismissTimer: ReturnType<typeof setTimeout> | null = null

function startAutoDismiss() {
  if (dismissTimer) clearTimeout(dismissTimer)
  if (autoDismissMs.value <= 0) return
  dismissTimer = setTimeout(() => {
    visible.value = false
  }, autoDismissMs.value)
}

function dismiss() {
  visible.value = false
  if (dismissTimer) {
    clearTimeout(dismissTimer)
    dismissTimer = null
  }
}

function startEdit() {
  editing.value = true
  editValue.value = (props.aiTags || '').replace(/[,，]/g, ',')
  if (dismissTimer) {
    clearTimeout(dismissTimer)
    dismissTimer = null
  }
  setTimeout(() => editInput.value?.focus(), 50)
}

async function saveEdit() {
  if (!editing.value) return
  editing.value = false
  const val = editValue.value.trim()
  if (val === (props.aiTags || '').trim()) {
    startAutoDismiss()
    return
  }
  try {
    await updatePostAiTags(props.postId, val)
    emit('update:aiTags', val)
    ElMessage.success('标签已更新')
  } catch {
    // 错误已由 request 拦截器展示
  }
  startAutoDismiss()
}

watch(() => props.aiTags, (v) => {
  if (v && visible.value) startAutoDismiss()
})

onMounted(() => {
  if (props.aiTags && visible.value) startAutoDismiss()
})

defineExpose({ dismiss })
</script>

<style lang="scss" scoped>
.feed-tag-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 14px;
  margin-top: 12px;
  background: linear-gradient(135deg, rgba($primary, 0.08), rgba($primary, 0.04));
  border-radius: $radius-md;
  border: 1px solid rgba($primary, 0.15);
}

.tag-bar-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.tag-bar-label {
  font-size: 13px;
  color: $text-secondary;
  flex-shrink: 0;
}

.tag-bar-tags {
  font-size: 13px;
  color: $primary;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-bar-input {
  flex: 1;
  margin: 0;
  padding: 4px 12px;
  font-size: 13px;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  outline: none;

  &:focus {
    border-color: $primary;
  }
}

.tag-bar-edit {
  padding: 2px 10px;
  font-size: 12px;
  color: $primary;
  background: transparent;
  border: 1px solid $primary;
  border-radius: $radius-md;
  cursor: pointer;
  flex-shrink: 0;

  &:hover {
    background: rgba($primary, 0.08);
  }
}

.tag-bar-close {
  padding: 4px;
  color: $text-muted;
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: $radius-sm;

  &:hover {
    color: $text-primary;
  }
}

.tag-bar-enter-active,
.tag-bar-leave-active {
  transition: opacity 0.25s, transform 0.25s;
}

.tag-bar-enter-from,
.tag-bar-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
