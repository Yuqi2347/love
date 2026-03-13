<template>
  <el-drawer
    v-model="visible"
    title="破冰功能 · 信息公开设置"
    direction="btt"
    size="auto"
    :close-on-click-modal="true"
  >
    <div class="privacy-sheet">
      <p class="sheet-desc">按画像三层架构，仅开放部分可配置项。聊天内容、行为数据、OCEAN 原始分数等永不进入 AI。</p>

      <div class="settings-group">
        <div class="group-title">第一层 · 硬属性（默认开启，可关闭）</div>
        <label class="setting-row">
          <span class="setting-label">MBTI（{{ profile?.mbti || '未填写' }}）</span>
          <el-switch v-model="local.mbti" />
        </label>
        <label class="setting-row">
          <span class="setting-label">星座（{{ profile?.zodiac || '未填写' }}）</span>
          <el-switch v-model="local.zodiac" />
        </label>
        <label class="setting-row">
          <span class="setting-label">专业大类</span>
          <el-switch v-model="local.majorCategory" />
        </label>
        <label class="setting-row">
          <span class="setting-label">兴趣标签</span>
          <el-switch v-model="local.interestTags" />
        </label>
      </div>

      <div class="settings-group">
        <div class="group-title">第二层 · 动态标签中的敏感项（默认关闭，需主动开启）</div>
        <label class="setting-row">
          <span class="setting-label">我的性格标签（如「需要安全感」）</span>
          <el-switch v-model="local.naturalLangTags" />
        </label>
        <label class="setting-row">
          <span class="setting-label">八字信息</span>
          <el-switch v-model="local.baziInfo" />
        </label>
        <label class="setting-row">
          <span class="setting-label">恋爱问卷偏好摘要</span>
          <el-switch v-model="local.questionnaireHints" />
        </label>
      </div>

      <div class="settings-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>第三层行为统计、聊天内容、OCEAN 原始分数、依恋类型、摩擦点等永不进入 AI，无需设置</span>
      </div>

      <button class="btn-save" @click="save">保存设置</button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import { updateAiDisclosureSettings, type UserProfile, type AiDisclosureSettings } from '@/api/userApi'
import { ElMessage } from 'element-plus'

const DEFAULT: AiDisclosureSettings = {
  mbti: true,
  zodiac: true,
  majorCategory: true,
  interestTags: true,
  naturalLangTags: false,
  baziInfo: false,
  questionnaireHints: false,
}

const props = defineProps<{
  modelValue: boolean
  profile: UserProfile | null
  currentSettings: AiDisclosureSettings | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved', settings: AiDisclosureSettings): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const local = ref<AiDisclosureSettings>({ ...DEFAULT })

watch(
  () => [props.modelValue, props.currentSettings],
  () => {
    if (props.modelValue) {
      local.value = props.currentSettings
        ? { ...DEFAULT, ...props.currentSettings }
        : { ...DEFAULT }
    }
  },
  { immediate: true }
)

async function save() {
  try {
    await updateAiDisclosureSettings(local.value)
    emit('saved', { ...local.value })
    ElMessage.success('已保存')
    visible.value = false
  } catch {
    // 错误已由 request 拦截器展示
  }
}
</script>

<style lang="scss" scoped>
.privacy-sheet {
  padding: 20px 24px 40px;
  max-height: 80vh;
  overflow-y: auto;
}

.sheet-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 24px;
}

.settings-group {
  margin-bottom: 20px;

  .group-title {
    font-size: 13px;
    font-weight: 600;
    color: $text-secondary;
    margin-bottom: 12px;
  }
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid $border-light;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }

  .setting-label {
    font-size: 15px;
    color: $text-primary;
  }
}

.settings-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 16px;
  margin-top: 20px;
  background: rgba($primary, 0.06);
  border-radius: $radius-md;
  font-size: 13px;
  color: $text-secondary;

  .el-icon {
    flex-shrink: 0;
    margin-top: 2px;
  }
}

.btn-save {
  width: 100%;
  margin-top: 24px;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background: $primary;
  border: none;
  border-radius: $radius-lg;
  cursor: pointer;

  &:hover {
    opacity: 0.9;
  }
}
</style>
