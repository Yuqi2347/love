<template>
  <div class="welcome-page">
    <!-- Photo background carousel -->
    <div class="bg-slideshow">
      <!-- Gradient base: always visible until images loaded -->
      <div class="bg-slide active" :style="{ background: theme.gradient }"></div>
      <!-- Image slides: fade in only after preload completes -->
      <div
        v-for="(img, i) in theme.images"
        :key="img"
        class="bg-slide"
        :class="{ active: imagesReady && currentSlide === i }"
        :style="{ backgroundImage: `url(${img})` }"
      ></div>
    </div>

    <!-- Dark overlay with gradient -->
    <div class="bg-overlay"></div>

    <!-- Subtle particles -->
    <div class="particles">
      <span v-for="i in 15" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>

    <!-- Main content -->
    <div class="welcome-content">
      <!-- Phase 1: School name entrance -->
      <div class="school-intro" :class="{ show: phase >= 1 }">
        <span class="mascot">{{ theme.mascotEmoji }}</span>
        <h1 class="school-name">{{ theme.name }}</h1>
        <div class="motto-divider">
          <span class="divider-line"></span>
          <span class="motto-text">{{ theme.motto }}</span>
          <span class="divider-line"></span>
        </div>
      </div>

      <!-- Phase 2: Rotating captions synced with photos -->
      <div class="caption-area" :class="{ show: phase >= 2 }">
        <transition name="caption-fade" mode="out-in">
          <p class="caption" :key="currentSlide">{{ currentCaption }}</p>
        </transition>
      </div>

      <!-- Phase 3: Mission + landmarks -->
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

    <!-- Bottom area -->
    <div class="bottom-area">
      <!-- Slide indicators -->
      <div v-if="theme.images.length > 1" class="slide-dots">
        <span
          v-for="(_, i) in theme.images"
          :key="i"
          class="dot"
          :class="{ active: currentSlide === i }"
        ></span>
      </div>

      <!-- Progress bar -->
      <div class="progress-bar">
        <div class="progress-fill" :style="{ background: theme.secondaryColor }"></div>
      </div>
    </div>

    <!-- Skip button -->
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
const imagesReady = ref(false)
let timers: ReturnType<typeof setTimeout>[] = []
let countdownInterval: ReturnType<typeof setInterval> | null = null
let slideInterval: ReturnType<typeof setInterval> | null = null

async function preloadImages(urls: string[]): Promise<void> {
  if (!urls.length) return
  await Promise.all(
    urls.map(src => new Promise<void>(resolve => {
      const img = new Image()
      img.onload = img.onerror = () => resolve()
      img.src = src
    }))
  )
}

const currentCaption = computed(() => {
  const captions = theme.value.captions
  if (!captions.length) return theme.value.mission
  return captions[currentSlide.value % captions.length]
})

function startAnimation() {
  // Countdown
  countdownInterval = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && countdownInterval) clearInterval(countdownInterval)
  }, 1000)

  // Phase 1: school name (0.3s)
  timers.push(setTimeout(() => { phase.value = 1 }, 300))

  // Phase 2: captions start (1.5s)
  timers.push(setTimeout(() => { phase.value = 2 }, 1500))

  // Start photo carousel (every 3s) — only once images are ready
  if (theme.value.images.length > 1) {
    slideInterval = setInterval(() => {
      currentSlide.value = (currentSlide.value + 1) % theme.value.images.length
    }, 3000)
  }

  // Phase 3: mission + landmarks (5s)
  timers.push(setTimeout(() => { phase.value = 3 }, 5000))

  // Navigate at 8s — Vue's page transition (App.vue) handles the fade,
  // no manual opacity manipulation needed, which eliminated the old 600ms white flash.
  timers.push(setTimeout(() => goHome(), 8000))
}

function goHome() {
  cleanup()
  sessionStorage.removeItem('register_school')
  router.replace('/discover')
}

function cleanup() {
  timers.forEach(clearTimeout)
  if (countdownInterval) clearInterval(countdownInterval)
  if (slideInterval) clearInterval(slideInterval)
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

onMounted(async () => {
  // Preload images before starting animation; fall back after 2 s if network is slow
  if (theme.value.images.length) {
    await Promise.race([
      preloadImages(theme.value.images),
      new Promise<void>(resolve => setTimeout(resolve, 2000)),
    ])
  }
  imagesReady.value = true
  startAnimation()
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

// Photo slideshow
.bg-slideshow {
  position: absolute;
  inset: 0;
}

.bg-slide {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0;
  transform: scale(1.05);
  transition: opacity 1.2s ease, transform 6s ease;

  &.active {
    opacity: 1;
    transform: scale(1);
  }
}

// Dark overlay — gradient from bottom for text readability
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
  z-index: 1;
}

// Particles
.particles {
  position: absolute;
  inset: 0;
  z-index: 2;
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

// Content
.welcome-content {
  position: relative;
  z-index: 3;
  text-align: center;
  padding: 0 32px;
  max-width: 700px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

// Phase 1: School intro
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

// Phase 2: Captions
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

// Caption transition
.caption-fade-enter-active { transition: all 0.6s ease; }
.caption-fade-leave-active { transition: all 0.4s ease; }
.caption-fade-enter-from { opacity: 0; transform: translateY(15px); }
.caption-fade-leave-to { opacity: 0; transform: translateY(-10px); }

// Phase 3: Mission + landmarks
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

// Bottom area
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

// Skip button
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

// Mobile
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
