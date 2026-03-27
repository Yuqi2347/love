<template>
  <section class="question-card">
    <header class="question-card__head">
      <div class="question-index">{{ displayIndex }}</div>
      <div class="question-copy">
        <h3 class="question-title">{{ question.title }}</h3>
        <p v-if="question.hint" class="question-hint">{{ question.hint }}</p>
      </div>
    </header>

    <div class="option-list">
      <template v-if="question.multi">
        <button
          v-for="opt in question.options"
          :key="opt.value"
          type="button"
          class="option-card"
          :class="{ selected: multiSelected.includes(opt.value) }"
          @click="toggleMulti(opt.value)"
        >
          <span class="option-card__marker">{{ opt.value }}</span>
          <span v-if="opt.emoji" class="option-emoji">{{ opt.emoji }}</span>
          <span class="option-text">{{ opt.label }}</span>
          <span class="check-mark" :class="{ active: multiSelected.includes(opt.value) }" />
        </button>
      </template>

      <template v-else>
        <button
          v-for="opt in question.options"
          :key="opt.value"
          type="button"
          class="option-card"
          :class="{ selected: modelValue === opt.value }"
          @click="emit('update:modelValue', opt.value)"
        >
          <span class="option-card__marker">{{ opt.value }}</span>
          <span v-if="opt.emoji" class="option-emoji">{{ opt.emoji }}</span>
          <span class="option-text">{{ opt.label }}</span>
          <span class="check-mark" :class="{ active: modelValue === opt.value }" />
        </button>
      </template>
    </div>

    <slot name="hint" />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Question } from '@/constants/momentConst'

const props = defineProps<{
  question: Question
  modelValue: string
  index?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const multiSelected = computed(() => props.modelValue ? props.modelValue.split(',') : [])
const displayIndex = computed(() => String(props.index ?? 1).padStart(2, '0'))

function toggleMulti(value: string) {
  const current = multiSelected.value
  let next: string[]
  if (current.includes(value)) {
    next = current.filter((v) => v !== value)
  } else {
    next = [...current, value]
  }
  emit('update:modelValue', next.join(','))
}
</script>

<style lang="scss" scoped>
$moment-pink: #e9a8be;
$moment-pink-strong: #d781a2;
$moment-pink-soft: #fff4f8;
$moment-pink-border: rgba(215, 129, 162, 0.2);

.question-card {
  margin-bottom: 20px;
  padding: 24px 22px 20px;
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(255, 248, 251, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 22px 44px rgba(227, 191, 205, 0.12);
}

.question-card__head {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: start;
  gap: 14px;
  margin-bottom: 16px;
}

.question-index {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 13px;
  background: $moment-pink-soft;
  border: 1px solid $moment-pink-border;
  color: $moment-pink-strong;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  flex-shrink: 0;
}

.question-title {
  margin: 0;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 21px;
  line-height: 1.45;
  font-weight: 700;
  color: #3f2f36;
}

.question-hint {
  margin: 8px 0 0;
  color: #9b7a87;
  font-size: 13px;
  line-height: 1.65;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-card {
  width: 100%;
  display: grid;
  grid-template-columns: auto auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(233, 168, 190, 0.12);
  text-align: left;
  transition: transform $transition-base, box-shadow $transition-base, border-color $transition-base, background $transition-base;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(215, 129, 162, 0.24);
    box-shadow: 0 14px 28px rgba(227, 191, 205, 0.1);
  }

  &.selected {
    background: linear-gradient(135deg, rgba(255, 247, 250, 0.99), rgba(255, 237, 243, 0.96));
    border-color: rgba(215, 129, 162, 0.36);
    box-shadow: 0 16px 28px rgba(227, 191, 205, 0.14);

    .option-card__marker {
      background: linear-gradient(135deg, rgba(215, 129, 162, 0.18), rgba(233, 168, 190, 0.24));
      border-color: rgba(215, 129, 162, 0.28);
      color: #b96084;
    }

    .option-text {
      color: #513a44;
      font-weight: 600;
    }
  }
}

.option-card__marker {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: $moment-pink-soft;
  border: 1px solid $moment-pink-border;
  color: #bf708f;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.option-emoji {
  font-size: 16px;
  line-height: 1;
}

.option-text {
  min-width: 0;
  color: #6b5962;
  font-size: 14px;
  line-height: 1.55;
}

.check-mark {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid rgba(233, 168, 190, 0.24);
  background: rgba(249, 243, 246, 0.92);
  position: relative;
  flex-shrink: 0;

  &::after {
    content: '';
    position: absolute;
    inset: 3px;
    border-radius: 50%;
    background: transparent;
    transition: background $transition-base;
  }

  &.active {
    border-color: rgba(215, 129, 162, 0.38);
    background: rgba(255, 242, 247, 0.96);

    &::after {
      background: #cb7295;
    }
  }
}

@media (max-width: 720px) {
  .question-card {
    padding: 18px 16px 16px;
    border-radius: 20px;
  }

  .question-card__head {
    gap: 12px;
  }

  .question-title {
    font-size: 18px;
  }

  .option-card {
    grid-template-columns: auto auto 1fr;
    padding: 12px 14px;
  }

  .check-mark {
    display: none;
  }
}
</style>
