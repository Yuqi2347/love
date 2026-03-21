<template>
  <div class="avatar-studio-page">
    <header class="studio-header">
      <button type="button" class="back-btn" aria-label="返回" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <span class="header-title">AI 头像工作室</span>
    </header>

    <div class="studio-body">
      <section class="hero">
        <div class="hero-icon-wrap">
          <span class="hero-icon">👤</span>
        </div>
        <h1 class="hero-title">让真实的你，被看见</h1>
        <p class="hero-sub">我们知道上传自己的照片需要勇气。所以我们做了这个功能。</p>
        <div class="hero-quote">
          每个人都值得被真实地看见。一张有温度的头像，是你迈向真实社交的第一步。
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">选择一种风格 · 看看自己的另一面</h2>
        <div class="style-grid">
          <button
            v-for="key in AVATAR_STUDIO_STYLE_KEYS"
            :key="key"
            type="button"
            class="style-card"
            :class="{
              selected: selectedStyle === key,
              'light-bg': key === '高管工作照' || key === '柯达胶片',
            }"
            :style="{ background: STYLE_CARD_THEMES[key]?.gradient }"
            @click="selectedStyle = key"
          >
            <span class="style-check" v-if="selectedStyle === key">✓</span>
            <span class="style-emoji">{{ STYLE_CARD_THEMES[key]?.emoji }}</span>
            <span class="style-label">{{ key }}</span>
          </button>
        </div>
        <p v-if="selectedStyle && STYLE_PICK_ENCOURAGE[selectedStyle]" class="style-encourage">
          {{ STYLE_PICK_ENCOURAGE[selectedStyle] }}
        </p>
      </section>

      <section v-if="selectedStyle" class="section example-section">
        <div class="example-head">
          <span>{{ selectedStyle }} · 示例效果</span>
          <span class="example-sub">真实生成效果参考</span>
        </div>
        <div class="example-compare">
          <div class="example-pane">
            <span class="example-tag">原图</span>
            <img
              :src="exampleOriginalSrc"
              class="example-img"
              alt="示例原图"
              loading="lazy"
              decoding="async"
            />
          </div>
          <div class="example-pane">
            <span class="example-tag">{{ selectedStyle }}</span>
            <img
              v-if="exampleStyledSrc"
              :src="exampleStyledSrc"
              class="example-img"
              :alt="`${selectedStyle} 示例`"
              loading="lazy"
              decoding="async"
            />
          </div>
        </div>
      </section>

      <section class="section">
        <div class="upload-panel" :class="{ 'has-file': !!previewUrl, 'is-busy': generating }">
          <div class="upload-panel-inner">
            <div class="upload-panel-left" @click="!generating && triggerFilePick()">
              <input
                ref="fileInput"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                class="hidden-input"
                :disabled="generating"
                @change="onFileChange"
              />
              <template v-if="!previewUrl">
                <div class="upload-plus">+</div>
                <p class="upload-title">点击上传你的照片</p>
                <p class="upload-hint">支持 JPG / PNG · 仅用于 AI 风格生成，不展示原图</p>
                <p class="upload-hint upload-hint-sub">使用白底干净大头照效果最佳</p>
              </template>
              <template v-else>
                <img :src="previewUrl" alt="" class="upload-preview" />
              </template>
            </div>
            <div class="upload-panel-right">
              <template v-if="generating">
                <el-icon class="spin"><Loading /></el-icon>
                <p class="upload-generating-text">努力生成中，请稍后~</p>
              </template>
              <template v-else-if="resultObjectUrl">
                <img :src="resultObjectUrl" alt="" class="upload-panel-result-img" />
                <p
                  v-if="selectedStyle && RESULT_CLOSING_LINE[selectedStyle]"
                  class="upload-panel-closing"
                >
                  {{ RESULT_CLOSING_LINE[selectedStyle] }}
                </p>
              </template>
              <template v-else>
                <span class="upload-panel-placeholder">生成后显示于此</span>
              </template>
            </div>
          </div>
        </div>
        <p class="quota-line">
          剩余免费次数：<strong>{{ quota?.remaining ?? '—' }}</strong> / {{ quota?.limit ?? '—' }}
        </p>
      </section>

      <div class="action-row action-row-four">
        <button
          type="button"
          class="btn secondary"
          :disabled="generating"
          @click="reselectFile"
        >
          重新选择
        </button>
        <button
          type="button"
          class="btn secondary"
          :disabled="!rawFile || generating"
          @click="applyOriginalAvatar"
        >
          使用原图
        </button>
        <button
          type="button"
          class="btn primary"
          :disabled="!rawFile || !selectedStyle || generating || !!(quota && quota.remaining <= 0)"
          @click="startGenerate"
        >
          {{ generateButtonLabel }}
        </button>
        <button
          v-if="resultObjectUrl"
          type="button"
          class="btn primary"
          :disabled="generating"
          @click="applyAiAvatar"
        >
          设为头像
        </button>
      </div>

      <p class="footer-note">
        <el-icon class="info-ico"><InfoFilled /></el-icon>
        照片不会被存储或展示给他人，仅用于本次 AI 风格生成。每人免费生成 {{ quota?.limit ?? 2 }} 次。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Loading, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  AVATAR_STUDIO_STYLE_KEYS,
  STYLE_CARD_THEMES,
  STYLE_PICK_ENCOURAGE,
  RESULT_CLOSING_LINE,
} from '@/constants/avatarStudioCopy'
import type { AvatarStudioStyleKey } from '@/constants/avatarStudioCopy'
import { getAvatarStudioQuota, postAvatarStudioGenerate } from '@/api/avatarStudioApi'
import { uploadAvatar } from '@/api/userApi'
import { compressAvatarFile } from '@/utils/mediaCompress'
import { useUserStore } from '@/store/userStore'

const router = useRouter()
const userStore = useUserStore()

const selectedStyle = ref<AvatarStudioStyleKey | null>(AVATAR_STUDIO_STYLE_KEYS[0]!)
const fileInput = ref<HTMLInputElement | null>(null)
const rawFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const quota = ref<{ limit: number; used: number; remaining: number } | null>(null)
const generating = ref(false)
const resultBase64 = ref<string | null>(null)
const resultObjectUrl = ref<string | null>(null)

/** 静态示例图（已压缩 JPEG，见 public/images/head_portrait） */
const HEAD_PORTRAIT = '/images/head_portrait'
function headPortraitFile(name: string) {
  return `${HEAD_PORTRAIT}/${encodeURIComponent(name)}`
}
const exampleOriginalSrc = headPortraitFile('原图.jpg')
const exampleStyledSrc = computed(() =>
  selectedStyle.value ? headPortraitFile(`${selectedStyle.value}.jpg`) : '',
)

const generateButtonLabel = computed(() =>
  resultBase64.value ? '重新生成' : '选好了，开始生成',
)

watch(resultBase64, (b64) => {
  if (resultObjectUrl.value) {
    URL.revokeObjectURL(resultObjectUrl.value)
    resultObjectUrl.value = null
  }
  if (!b64) return
  const blob = base64ToBlob(b64, 'image/png')
  resultObjectUrl.value = URL.createObjectURL(blob)
})

onUnmounted(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  if (resultObjectUrl.value) URL.revokeObjectURL(resultObjectUrl.value)
})

function base64ToBlob(b64: string, mime: string): Blob {
  const bin = atob(b64)
  const u8 = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) u8[i] = bin.charCodeAt(i)
  return new Blob([u8], { type: mime })
}

onMounted(async () => {
  try {
    const res = await getAvatarStudioQuota()
    quota.value = res.data.data as typeof quota.value
  } catch {
    /* quota optional */
  }
})

function triggerFilePick() {
  fileInput.value?.click()
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 8MB')
    return
  }
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  rawFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  input.value = ''
}

function reselectFile() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  rawFile.value = null
  resultBase64.value = null
}

async function startGenerate() {
  if (!rawFile.value || !selectedStyle.value) return
  if ((quota.value?.remaining ?? 0) <= 0) {
    ElMessage.warning('免费生成次数已用完')
    return
  }
  generating.value = true
  try {
    const toSend = await compressAvatarFile(rawFile.value)
    const res = await postAvatarStudioGenerate(toSend, selectedStyle.value)
    const payload = res.data.data
    resultBase64.value = payload.imageBase64
    quota.value = {
      limit: quota.value?.limit ?? 2,
      used: (quota.value?.limit ?? 2) - payload.remaining,
      remaining: payload.remaining,
    }
    ElMessage.success('生成成功')
  } catch (e: unknown) {
    const err = e as { response?: { data?: { code?: number } } }
    if (err?.response?.data?.code === 7007) {
      ElMessage.error('免费生成次数已用完')
    }
  } finally {
    generating.value = false
  }
}

async function applyAiAvatar() {
  if (!resultBase64.value) return
  try {
    const blob = base64ToBlob(resultBase64.value, 'image/png')
    const file = new File([blob], 'avatar-ai.png', { type: 'image/png' })
    const toSend = await compressAvatarFile(file)
    ElMessage.info('上传中...')
    await uploadAvatar(toSend)
    await userStore.fetchProfile()
    ElMessage.success('头像已更新')
    router.back()
  } catch {
    ElMessage.error('上传失败')
  }
}

async function applyOriginalAvatar() {
  if (!rawFile.value) return
  try {
    const toSend = await compressAvatarFile(rawFile.value)
    ElMessage.info('上传中...')
    await uploadAvatar(toSend)
    await userStore.fetchProfile()
    ElMessage.success('头像已更新')
    router.back()
  } catch {
    ElMessage.error('上传失败')
  }
}

</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.avatar-studio-page {
  min-height: 100vh;
  background: $bg-body;
  padding-bottom: 32px;
}

.studio-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: $bg-primary;
  border-bottom: 1px solid $border-color;
  position: sticky;
  top: 0;
  z-index: 20;
  box-shadow: $shadow-sm;
}

.back-btn {
  padding: 8px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
  color: $text-primary;
}

.header-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
}

.studio-body {
  max-width: 560px;
  margin: 0 auto;
  padding: 20px 16px;
}

.hero {
  text-align: center;
  margin-bottom: 28px;
}

.hero-icon-wrap {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, $primary-light, $primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-icon {
  font-size: 28px;
}

.hero-title {
  font-size: $text-2xl;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 8px;
}

.hero-sub {
  font-size: $text-sm;
  color: $text-secondary;
  margin: 0 0 16px;
  line-height: 1.5;
}

.hero-quote {
  font-size: $text-sm;
  color: $text-secondary;
  background: $bg-secondary;
  border-radius: $radius-lg;
  padding: 14px 16px;
  line-height: 1.6;
  text-align: left;
  border: 1px solid $border-light;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: $text-lg;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 14px;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.style-card {
  position: relative;
  border: 2px solid transparent;
  border-radius: $radius-lg;
  min-height: 88px;
  padding: 12px;
  cursor: pointer;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: transform $transition-fast, box-shadow $transition-fast;

  &.selected {
    border-color: #4f8cff;
    box-shadow: 0 0 0 2px rgba(79, 140, 255, 0.35);
  }

  &.light-bg {
    color: #3d3d3d;
    text-shadow: none;
    .style-label {
      color: #3d3d3d;
    }
  }

  &:hover {
    transform: translateY(-2px);
  }
}

.style-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  background: #4f8cff;
  border-radius: 50%;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.style-emoji {
  font-size: 22px;
}

.style-label {
  font-size: $text-sm;
  font-weight: 600;
}

.style-encourage {
  margin-top: 12px;
  font-size: $text-sm;
  color: $text-secondary;
  line-height: 1.5;
}

.example-section {
  background: $bg-primary;
  border-radius: $radius-lg;
  padding: 16px;
  border: 1px solid $border-light;
}

.example-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: $text-sm;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 12px;
}

.example-sub {
  font-size: $text-xs;
  font-weight: 400;
  color: $text-muted;
}

.example-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.example-pane {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-tag {
  font-size: $text-xs;
  color: $text-muted;
}

.example-img {
  width: 100%;
  max-height: 220px;
  object-fit: contain;
  border-radius: $radius-md;
  background: $bg-tertiary;
}

.upload-panel {
  border: 2px dashed $border-color;
  border-radius: $radius-lg;
  background: $bg-primary;
  transition: border-color $transition-fast;
  overflow: hidden;

  &:hover,
  &.has-file {
    border-color: $primary;
  }

  &.is-busy .upload-panel-left {
    pointer-events: none;
    opacity: 0.92;
    cursor: not-allowed;
  }
}

.upload-panel-inner {
  display: flex;
  align-items: stretch;
  min-height: 200px;

  @media (max-width: 520px) {
    flex-direction: column;
  }
}

.upload-panel-left {
  flex: 1;
  min-width: 0;
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  border-right: 1px solid $border-light;

  @media (max-width: 520px) {
    border-right: none;
    border-bottom: 1px solid $border-light;
  }
}

.upload-panel-right {
  flex: 1;
  min-width: 0;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: $bg-secondary;

  .spin {
    font-size: 28px;
    color: $primary;
    animation: spin 1s linear infinite;
  }
}

.upload-panel-result-img {
  max-height: 200px;
  max-width: 100%;
  object-fit: contain;
  border-radius: $radius-md;
  background: $bg-tertiary;
}

.upload-panel-closing {
  margin: 0;
  font-size: $text-xs;
  color: $text-secondary;
  line-height: 1.45;
  text-align: center;
}

.upload-panel-placeholder {
  font-size: $text-sm;
  color: $text-muted;
  text-align: center;
  line-height: 1.4;
  padding: 8px;
}

.upload-generating-text {
  margin: 0;
  font-size: $text-sm;
  color: $text-secondary;
  text-align: center;
  line-height: 1.45;
}

.hidden-input {
  display: none;
}

.upload-plus {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: rgba(79, 140, 255, 0.15);
  color: #4f8cff;
  font-size: 28px;
  line-height: 48px;
  font-weight: 300;
}

.upload-title {
  font-size: $text-base;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 8px;
}

.upload-hint {
  font-size: $text-xs;
  color: $text-muted;
  margin: 0;
}

.upload-hint-sub {
  margin-top: 6px;
}

.upload-preview {
  max-height: 220px;
  max-width: 100%;
  border-radius: $radius-md;
  object-fit: contain;
}

.quota-line {
  margin-top: 12px;
  font-size: $text-sm;
  color: $text-secondary;
  text-align: center;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  &.vertical {
    flex-direction: column;
  }

  &.action-row-four {
    flex-wrap: wrap;
    gap: 10px;

    .btn {
      flex: 1 1 calc(25% - 10px);
      min-width: 72px;
      font-size: $text-sm;
      padding: 12px 8px;
    }

    @media (max-width: 420px) {
      .btn {
        flex: 1 1 calc(50% - 6px);
      }
    }
  }
}

.btn {
  flex: 1;
  padding: 14px 20px;
  border-radius: $radius-full;
  font-size: $text-base;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: opacity $transition-fast;

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  &.primary {
    background: $gradient-warm;
    color: $text-inverse;
  }

  &.secondary {
    background: $bg-tertiary;
    color: $text-primary;
    border: 1px solid $border-color;
  }

  &.text {
    background: transparent;
    color: $text-secondary;
    flex: none;
  }

  &.block {
    flex: none;
    width: 100%;
  }
}

.footer-note {
  font-size: $text-xs;
  color: $text-muted;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.info-ico {
  flex-shrink: 0;
  margin-top: 2px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
