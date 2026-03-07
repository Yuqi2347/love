<template>
  <div class="question-block">
    <div class="question-title">{{ question.title }}</div>
    <div class="option-list" :class="{ 'option-grid-2': question.options.length === 4 }">
      <template v-if="question.multi">
        <div
          v-for="opt in question.options" :key="opt.value"
          class="option-card"
          :class="{ selected: multiSelected.includes(opt.value) }"
          @click="toggleMulti(opt.value)"
        >
          <span v-if="opt.emoji" class="option-emoji">{{ opt.emoji }}</span>
          <span class="option-text">{{ opt.label }}</span>
          <span v-if="multiSelected.includes(opt.value)" class="check-mark">✓</span>
        </div>
      </template>
      <template v-else>
        <div
          v-for="opt in question.options" :key="opt.value"
          class="option-card"
          :class="{ selected: modelValue === opt.value }"
          @click="emit('update:modelValue', opt.value)"
        >
          <span v-if="opt.emoji" class="option-emoji">{{ opt.emoji }}</span>
          <span class="option-text">{{ opt.label }}</span>
        </div>
      </template>
    </div>
    <slot name="hint" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Question } from '@/constants/momentConst'

const props = defineProps<{
  question: Question
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const multiSelected = computed(() => props.modelValue ? props.modelValue.split(',') : [])

function toggleMulti(value: string) {
  const current = multiSelected.value
  let next: string[]
  if (current.includes(value)) {
    next = current.filter(v => v !== value)
  } else {
    next = [...current, value]
  }
  emit('update:modelValue', next.join(','))
}
</script>

<style lang="scss" scoped>
.question-block {
  margin-bottom: 24px;
}

.question-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 12px;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
}

.option-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: $bg-secondary;
  border: 2px solid transparent;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-base;
  position: relative;

  &:hover {
    background: rgba($primary, 0.05);
    border-color: $primary-light;
  }

  &.selected {
    background: rgba($primary, 0.08);
    border-color: $primary;

    .option-text { color: $primary-dark; font-weight: 600; }
  }

  .option-emoji {
    font-size: 20px;
    flex-shrink: 0;
  }

  .option-text {
    font-size: 14px;
    color: $text-primary;
    line-height: 1.4;
  }

  .check-mark {
    position: absolute;
    right: 12px;
    color: $primary;
    font-weight: 700;
    font-size: 16px;
  }
}
</style>
