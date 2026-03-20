<template>
  <div class="welcome-page">
    <div class="bg-slideshow">
      <!-- 有配图时：图片未就绪前只显示渐变；就绪后由 <img> 全屏覆盖（比 background-image 更不易「半拉子」） -->
      <div
        class="bg-slide bg-fallback"
        :class="{ active: !theme.images.length || !photoLayerVisible }"
        :style="{ background: theme.gradient }"
      />
      <img
        v-for="(img, i) in theme.images"
        :key="img"
        class="bg-photo"
        :class="{ active: photoLayerVisible && currentSlide === i }"
        :src="img"
        alt=""
        decoding="async"
        :fetchpriority="i === 0 ? 'high' : 'low'"
        @load="onSlideImageLoad(i)"
        @error="onSlideImageError(i)"
      />
    </div>

    <div class="bg-overlay"></div>

    <div class="particles">
      <span v-for="i in 15" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>

    <div class="welcome-content">
      <div class="school-intro" :class="{ show: phase >= 1 }">
        <span class="mascot">{{ theme.mascotEmoji }}</span>
        <h1 class="school-name">{{ theme.name }}</h1>
        <div class="motto-divider">
          <span class="divider-line"></span>
          <span class="motto-text">{{ theme.motto }}</span>
          <span class="divider-line"></span>
        </div>
      </div>

      <div class="caption-area" :class="{ show: phase >= 2 }">
        <transition name="caption-fade" mode="out-in">
          <p class="caption" :key="currentSlide">{{ currentCaption }}</p>
        </transition>
      </div>

      <div class="mission-area" :class="{ show: phase >= 3 }">
        <p class="mission">{{ theme.mission }}</p>
        <div v-if="theme.landmarks.length" class="landmarks">
          <span
            v-for="(lm, i) in theme.landmarks"
            :key="lm"
            class="landmark"
            :style="{ animationDelay: `${i * 0.12}s` }"
          >{{ lm }}</span>
        </div>
      </div>
    </div>

    <div class="bottom-area">
      <div v-if="theme.images.length > 1" class="slide-dots">
        <span
          v-for="(_, i) in theme.images"
          :key="i"
          class="dot"
          :class="{ active: currentSlide === i }"
        ></span>
      </div>

      <div class="progress-bar">
        <div class="progress-fill" :style="{ background: theme.secondaryColor }"></div>
      </div>
    </div>

    <button class="skip-btn" @click="goHome">
      跳过 <span class="skip-countdown">{{ countdown }}s</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSchoolTheme } from '@/constants/schoolThemes'

const route = useRoute()
const router = useRouter()

const schoolName = computed(() => {
  const q = route.query.school as string | undefined
  return q ? decodeURIComponent(q) : undefined
})

const theme = computed(() => getSchoolTheme(schoolName.value))

const phase = ref(0)
const currentSlide = ref(0)
const countdown = ref(8)
/** 至少第一张图 load/error 后再盖住渐变，避免半解码背景图 */
const photoLayerVisible = ref(false)
const navigating = ref(false)
let timers: ReturnType<typeof setTimeout>[] = []
let countdownInterval: ReturnType<typeof setInterval> | null = null
let slideInterval: ReturnType<typeof setInterval> | null = null
let revealFallbackTimer: ReturnType<typeof setTimeout> | null = null

const slideDone = ref<boolean[]>([])

function resetSlideTracking() {
  const n = theme.value.images.length
  slideDone.value = n ? Array.from({ length: n }, () => false) : []
}

function markSlideReady(i: number) {
  if (i < 0 || !slideDone.value.length || i >= slideDone.value.length) return
  const next = [...slideDone.value]
  next[i] = true
  slideDone.value = next
  if (next[0]) photoLayerVisible.value = true
}

function onSlideImageLoad(i: number) {
  markSlideReady(i)
}

function onSlideImageError(i: number) {
  markSlideReady(i)
}

const currentCaption = computed(() => {
  const captions = theme.value.captions
  if (!captions.length) return theme.value.mission
  return captions[currentSlide.value % captions.length]
})

function startAnimation() {
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownInterval) clearInterval(countdownInterval)
      goHome()
    }
  }, 1000)

  timers.push(setTimeout(() => { phase.value = 1 }, 300))
  timers.push(setTimeout(() => { phase.value = 2 }, 1500))

  if (theme.value.images.length > 1) {
    slideInterval = setInterval(() => {
      currentSlide.value = (currentSlide.value + 1) % theme.value.images.length
    }, 3000)
  }

  timers.push(setTimeout(() => { phase.value = 3 }, 5000))
}

function goHome() {
  if (navigating.value) return
  navigating.value = true
  cleanup()
  sessionStorage.removeItem('register_school')
  router.replace('/discover').catch(() => {
    window.location.href = '/discover'
  })
}

function cleanup() {
  timers.forEach(clearTimeout)
  if (countdownInterval) clearInterval(countdownInterval)
  if (slideInterval) clearInterval(slideInterval)
  if (revealFallbackTimer) clearTimeout(revealFallbackTimer)
}

function particleStyle(i: number) {
  const size = 2 + (i % 3) * 2
  const left = (i * 17 + 5) % 100
  const top = (i * 23 + 11) % 100
  const delay = (i * 0.5) % 5
  const dur = 5 + (i % 4)
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${dur}s`,
  }
}

onMounted(() => {
  resetSlideTracking()
  startAnimation()

  if (!theme.value.images.length) {
    photoLayerVisible.value = true
    return
  }

  // 首张图极慢时也不要永远卡在纯渐变：最多等 4s 仍显示照片层（可能空白一切片，随后 onload 再显）
  revealFallbackTimer = setTimeout(() => {
    if (!photoLayerVisible.value) photoLayerVisible.value = true
  }, 4000)
})

onUnmounted(() => { cleanup() })
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.welcome-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  z-index: 9999;
}

.bg-slideshow {
  position: absolute;
  inset: 0;
}

.bg-slide,
.bg-photo {
  position: absolute;
  inset: 0;
}

.bg-fallback {
  z-index: 0;
  background-size: cover;
  background-position: center;
  opacity: 0;
  transform: scale(1.05);
  transition: opacity 0.8s ease, transform 6s ease;

  &.active {
    opacity: 1;
    transform: scale(1);
  }
}

.bg-photo {
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  opacity: 0;
  transform: scale(1.05);
  transition: opacity 1.2s ease, transform 6s ease;
  pointer-events: none;

  &.active {
    opacity: 1;
    transform: scale(1);
  }
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(to bottom,
      rgba(0, 0, 0, 0.35) 0%,
      rgba(0, 0, 0, 0.2) 30%,
      rgba(0, 0, 0, 0.3) 60%,
      rgba(0, 0, 0, 0.6) 100%
    );
  z-index: 2;
}

.particles {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  animation: particle-drift ease-in-out infinite;
}

@keyframes particle-drift {
  0%, 100% { transform: translateY(0) translateX(0); opacity: 0.15; }
  50% { transform: translateY(-30px) translateX(10px); opacity: 0.5; }
}

.welcome-content {
  position: relative;
  z-index: 4;
  text-align: center;
  padding: 0 32px;
  max-width: 700px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.school-intro {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.22, 1, 0.36, 1);

  &.show {
    opacity: 1;
    transform: translateY(0);
  }
}

.mascot {
  font-size: 56px;
  display: block;
  margin-bottom: 8px;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3));
}

.school-name {
  font-size: 52px;
  font-weight: 800;
  color: white;
  letter-spacing: 6px;
  text-shadow: 0 4px 30px rgba(0, 0, 0, 0.4);
  margin-bottom: 16px;
}

.motto-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.divider-line {
  width: 40px;
  height: 1px;
  background: rgba(255, 255, 255, 0.4);
}

.motto-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 4px;
  font-weight: 400;
}

.caption-area {
  margin-top: 40px;
  min-height: 48px;
  opacity: 0;
  transition: opacity 0.5s ease;

  &.show { opacity: 1; }
}

.caption {
  font-size: 28px;
  font-weight: 700;
  color: white;
  letter-spacing: 3px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.4);
}

.caption-fade-enter-active { transition: all 0.6s ease; }
.caption-fade-leave-active { transition: all 0.4s ease; }
.caption-fade-enter-from { opacity: 0; transform: translateY(15px); }
.caption-fade-leave-to { opacity: 0; transform: translateY(-10px); }

.mission-area {
  margin-top: 36px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.7s ease;

  &.show {
    opacity: 1;
    transform: translateY(0);
  }
}

.mission {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.7;
  margin-bottom: 20px;
}

.landmarks {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.landmark {
  padding: 5px 18px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: $radius-full;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  font-weight: 500;
  opacity: 0;
  animation: landmark-pop 0.4s ease forwards;
}

@keyframes landmark-pop {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}

.bottom-area {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 5;
}

.slide-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transition: all 0.4s ease;

  &.active {
    background: white;
    width: 20px;
    border-radius: 3px;
  }
}

.progress-bar {
  height: 3px;
  background: rgba(255, 255, 255, 0.1);
}

.progress-fill {
  height: 100%;
  width: 0;
  animation: progress-go 8s linear forwards;
  border-radius: 0 2px 2px 0;
}

@keyframes progress-go {
  from { width: 0; }
  to { width: 100%; }
}

.skip-btn {
  position: absolute;
  top: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: $radius-full;
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  cursor: pointer;
  transition: all $transition-fast;
  z-index: 10;

  &:hover {
    background: rgba(0, 0, 0, 0.4);
    color: white;
  }
}

.skip-countdown {
  font-size: 12px;
  opacity: 0.6;
}

@media (max-width: $bp-mobile) {
  .school-name { font-size: 36px; letter-spacing: 3px; }
  .mascot { font-size: 44px; }
  .caption { font-size: 22px; letter-spacing: 2px; }
  .motto-text { font-size: 14px; letter-spacing: 2px; }
  .mission { font-size: 14px; }
  .divider-line { width: 28px; }
  .skip-btn { top: 16px; right: 16px; }
}
</style>
