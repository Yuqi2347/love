<template>
  <el-popover
    :visible="visible"
    placement="top-start"
    :width="280"
    trigger="manual"
    @update:visible="(v: boolean) => visible = v"
  >
    <template #reference>
      <button type="button" class="emoji-trigger" title="插入表情" @click="toggle">
        <el-icon :size="20"><Sunny /></el-icon>
      </button>
    </template>
    <div class="emoji-grid">
      <button
        v-for="e in emojiList"
        :key="e"
        type="button"
        class="emoji-item"
        @click="insert(e)"
      >
        {{ e }}
      </button>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Sunny } from '@element-plus/icons-vue'

const visible = ref(false)

const emojiList = [
  '😀', '😊', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉',
  '😍', '😘', '🥰', '😎', '🤔', '😢', '😭', '😤', '😡', '🤬',
  '👍', '👎', '👏', '🙌', '🤝', '🙏', '❤️', '🧡', '💛', '💚',
  '💙', '💜', '🖤', '🤍', '🤎', '💕', '💖', '💗', '💘', '💝',
  '😺', '😸', '😹', '😻', '😼', '😽', '🙀', '😿', '😾', '🐶',
  '⭐', '🌟', '✨', '💫', '🔥', '💯', '✅', '❌', '❗', '❓',
]

const emit = defineEmits<{ insert: [emoji: string] }>()

function toggle() {
  visible.value = !visible.value
}

function insert(emoji: string) {
  emit('insert', emoji)
  visible.value = false
}
</script>

<style lang="scss" scoped>
.emoji-trigger {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: transparent;
  color: var(--el-text-color-secondary);
  transition: background 0.2s, color 0.2s;
  flex-shrink: 0;
  &:hover {
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
  }
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
  max-height: 180px;
  overflow-y: auto;
}

.emoji-item {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
  &:hover {
    background: var(--el-fill-color-light);
  }
}
</style>
