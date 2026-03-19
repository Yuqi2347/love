<template>
  <div class="landing-page">
    <!-- Nav -->
    <nav class="landing-nav" :class="{ scrolled: navScrolled }">
      <div class="nav-inner">
        <div class="nav-brand">
          <span class="nav-logo-text">Campal</span>
        </div>
        <div class="nav-actions">
          <router-link to="/login" class="nav-link">登录</router-link>
          <router-link to="/register" class="nav-btn">注册</router-link>
        </div>
      </div>
    </nav>

    <!-- Hero -->
    <section class="hero">
      <!-- Animated background -->
      <div class="hero-bg">
        <div class="float-shape shape-1"></div>
        <div class="float-shape shape-2"></div>
        <div class="float-shape shape-3"></div>
        <div class="float-shape shape-4"></div>
        <div class="float-shape shape-5"></div>
        <!-- Particle field -->
        <span v-for="i in 20" :key="'p'+i" class="hero-particle" :style="particleStyle(i)"></span>
        <!-- Glow orbs -->
        <div class="glow-orb orb-1"></div>
        <div class="glow-orb orb-2"></div>
      </div>

      <div class="hero-content">
        <div class="hero-badge" :class="{ show: heroReady }">
          <span class="badge-dot"></span>
          AI 驱动校园社交
        </div>
        <h1 class="hero-brand" :class="{ show: heroReady }">Campal</h1>
        <h2 class="hero-title" :class="{ show: heroReady }">在校园里，遇见对的人</h2>
        <p class="hero-subtitle" :class="{ show: heroReady }">{{ displayedSubtitle }}<span class="typing-cursor">|</span></p>
        <!-- SZU landmarks -->
        <div class="hero-landmarks" :class="{ show: heroReady }">
          <span v-for="(lm, i) in landmarks" :key="lm" class="landmark-pill" :style="{ animationDelay: `${1.2 + i * 0.15}s` }">{{ lm }}</span>
        </div>
        <router-link to="/register" class="hero-cta" :class="{ show: heroReady }">
          <span>开始探索</span>
          <span class="cta-arrow">→</span>
        </router-link>
        <p class="hero-motto" :class="{ show: heroReady }">遇见TA · 从校园开始</p>
        <!-- Scroll indicator -->
        <div class="scroll-hint" :class="{ show: heroReady }">
          <div class="mouse">
            <div class="mouse-wheel"></div>
          </div>
          <span>向下滚动</span>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features" ref="featuresRef">
      <h2 class="section-title">为什么选择 <span class="title-brand">Campal</span></h2>
      <div class="features-grid">
        <div v-for="(f, i) in features" :key="i" class="feature-card" :class="{ visible: featuresVisible }">
          <div class="feature-icon-wrap">
            <span class="feature-icon">{{ f.icon }}</span>
          </div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- How It Works -->
    <section class="how-it-works" ref="howRef">
      <h2 class="section-title">三步开始探索</h2>
      <div class="steps">
        <div v-for="(s, i) in steps" :key="i" class="step-item" :class="{ visible: howVisible }">
          <div class="step-number">{{ i + 1 }}</div>
          <h3 class="step-title">{{ s.title }}</h3>
          <p class="step-desc">{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <!-- CTA Banner -->
    <section class="cta-banner" ref="ctaRef">
      <div class="cta-inner" :class="{ visible: ctaVisible }">
        <h2>让每一段校园时光，都有温暖的陪伴</h2>
        <router-link to="/register" class="cta-btn">立即加入</router-link>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <span class="footer-brand">Campal</span>
      <p>&copy; {{ new Date().getFullYear() }} Campal — 校园交友平台</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const landmarks = ['校园发现', '心动时刻', '校园邀约', '真实互动']

const features = [
  { icon: '🎯', title: 'AI 智能推荐', desc: '结合画像与互动偏好，帮你更快发现值得认识的人' },
  { icon: '💕', title: '心动时刻', desc: '每周限定匹配窗口，AI 生成专属约会方案' },
  { icon: '📅', title: '校园邀约', desc: '聚餐、运动、学习、剧本杀，一键发起校园活动' },
  { icon: '🔒', title: '安全可信', desc: '校园邮箱认证，渐进式信任社交，保护你的隐私' },
]

const steps = [
  { title: '注册验证', desc: '使用邮箱快速注册，完成身份认证' },
  { title: '完善画像', desc: '填写兴趣与资料，让系统更懂你的社交偏好' },
  { title: '开始互动', desc: '浏览校园动态、参与心动时刻或发起邀约' },
]

const featuresRef = ref<HTMLElement>()
const howRef = ref<HTMLElement>()
const ctaRef = ref<HTMLElement>()
const featuresVisible = ref(false)
const howVisible = ref(false)
const ctaVisible = ref(false)
const heroReady = ref(false)
const navScrolled = ref(false)

// Typewriter subtitle
const fullSubtitle = '用虚拟 AI 推动真实社交，让每一次相遇都有意义'
const displayedSubtitle = ref('')
let typeTimer: ReturnType<typeof setTimeout> | null = null

function typeSubtitle() {
  let i = 0
  const tick = () => {
    if (i <= fullSubtitle.length) {
      displayedSubtitle.value = fullSubtitle.slice(0, i)
      i++
      typeTimer = setTimeout(tick, 60)
    }
  }
  typeTimer = setTimeout(tick, 800)
}

// Particle positions
function particleStyle(i: number) {
  const size = 2 + (i % 4) * 2
  const left = (i * 13 + 5) % 100
  const top = (i * 19 + 7) % 100
  const delay = (i * 0.4) % 6
  const dur = 4 + (i % 5)
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${dur}s`,
  }
}

let observer: IntersectionObserver | null = null

function onScroll() {
  navScrolled.value = window.scrollY > 40
}

onMounted(() => {
  // Hero entrance
  setTimeout(() => { heroReady.value = true }, 200)
  typeSubtitle()

  window.addEventListener('scroll', onScroll, { passive: true })

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          if (entry.target === featuresRef.value) featuresVisible.value = true
          if (entry.target === howRef.value) howVisible.value = true
          if (entry.target === ctaRef.value) ctaVisible.value = true
        }
      })
    },
    { threshold: 0.15 }
  )
  if (featuresRef.value) observer.observe(featuresRef.value)
  if (howRef.value) observer.observe(howRef.value)
  if (ctaRef.value) observer.observe(ctaRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('scroll', onScroll)
  if (typeTimer) clearTimeout(typeTimer)
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

// Landing page accent color (blue, not red)
$landing-accent: #4f8cff;
$landing-accent-dark: #0f3460;

// Art font for brand
.nav-logo-text, .hero-brand, .footer-brand, .title-brand {
  font-family: 'Pacifico', cursive;
}

.landing-page {
  min-height: 100vh;
  background: $bg-body;
  overflow-x: hidden;
}

// Nav
.landing-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: transparent;
  transition: all 0.3s ease;

  &.scrolled {
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(20px);
    box-shadow: 0 2px 20px rgba(0, 0, 0, 0.06);

    .nav-logo-text { color: $landing-accent; }
    .nav-link { color: $text-secondary; }
  }
}

.nav-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-brand {
  display: flex;
  align-items: center;
}

.nav-logo-text {
  font-size: 28px;
  color: white;
  transition: color 0.3s;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-link {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  transition: color $transition-fast;

  &:hover { color: white; }
}

.landing-nav.scrolled .nav-link:hover { color: $landing-accent; }

.nav-btn {
  padding: 8px 24px;
  background: white;
  color: $landing-accent-dark;
  border-radius: $radius-full;
  font-size: 14px;
  font-weight: 600;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  }
}

.landing-nav.scrolled .nav-btn {
  background: $landing-accent;
  color: white;

  &:hover { box-shadow: 0 4px 16px rgba($landing-accent, 0.3); }
}

// Hero
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  // 柔和渐变，不刺眼
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 35%, #0f3460 65%, #533483 100%);
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

// Floating shapes — organic blobs
.float-shape {
  position: absolute;
  opacity: 0.08;
  background: white;
  animation: float-drift ease-in-out infinite;
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -10%;
  left: -8%;
  border-radius: 40% 60% 55% 45% / 55% 40% 60% 45%;
  animation-duration: 12s;
}

.shape-2 {
  width: 250px;
  height: 250px;
  top: 55%;
  right: -5%;
  border-radius: 55% 45% 50% 50% / 45% 55% 45% 55%;
  animation-duration: 10s;
  animation-delay: -3s;
}

.shape-3 {
  width: 180px;
  height: 180px;
  bottom: 10%;
  left: 15%;
  border-radius: 50% 50% 40% 60% / 60% 40% 60% 40%;
  animation-duration: 14s;
  animation-delay: -6s;
}

.shape-4 {
  width: 120px;
  height: 120px;
  top: 15%;
  right: 18%;
  border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
  animation-duration: 9s;
  animation-delay: -2s;
}

.shape-5 {
  width: 80px;
  height: 80px;
  top: 40%;
  left: 40%;
  border-radius: 50%;
  animation-duration: 8s;
  animation-delay: -4s;
  opacity: 0.05;
}

@keyframes float-drift {
  0%, 100% { transform: translate(0, 0) rotate(0deg) scale(1); }
  25% { transform: translate(15px, -25px) rotate(5deg) scale(1.03); }
  50% { transform: translate(-10px, 15px) rotate(-3deg) scale(0.97); }
  75% { transform: translate(20px, 10px) rotate(4deg) scale(1.02); }
}

// Glow orbs
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: glow-pulse 6s ease-in-out infinite;
}

.orb-1 {
  width: 300px;
  height: 300px;
  top: 20%;
  left: 10%;
  background: rgba($landing-accent, 0.15);
}

.orb-2 {
  width: 250px;
  height: 250px;
  bottom: 15%;
  right: 10%;
  background: rgba(#a78bfa, 0.12);
  animation-delay: -3s;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.15); }
}

// Particles
.hero-particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  animation: particle-rise linear infinite;
}

@keyframes particle-rise {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: 0.6; }
  90% { opacity: 0.3; }
  100% { transform: translateY(-120px) scale(0.5); opacity: 0; }
}

// Hero content
.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 0 24px;
  max-width: 700px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 20px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: $radius-full;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease;

  &.show { opacity: 1; transform: translateY(0); }
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: dot-pulse 2s ease-in-out infinite;
  box-shadow: 0 0 6px rgba(#22c55e, 0.5);
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.4); }
}

.hero-brand {
  font-size: 72px;
  color: white;
  margin-bottom: 8px;
  opacity: 0;
  transform: translateY(30px) scale(0.9);
  transition: all 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
  text-shadow: 0 4px 30px rgba(0, 0, 0, 0.15);

  &.show { opacity: 1; transform: translateY(0) scale(1); transition-delay: 0.1s; }
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  color: white;
  letter-spacing: 2px;
  margin-bottom: 16px;
  line-height: 1.3;
  opacity: 0;
  transform: translateY(25px);
  transition: all 0.7s ease;

  &.show { opacity: 1; transform: translateY(0); transition-delay: 0.3s; }
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 28px;
  font-weight: 400;
  min-height: 28px;
  opacity: 0;
  transition: opacity 0.5s ease;

  &.show { opacity: 1; transition-delay: 0.5s; }
}

.typing-cursor {
  animation: blink 0.8s step-end infinite;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.6);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

// Landmarks
.hero-landmarks {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 32px;
  opacity: 0;
  transition: opacity 0.5s ease;

  &.show { opacity: 1; transition-delay: 0.7s; }
}

.landmark-pill {
  padding: 5px 16px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: $radius-full;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  opacity: 0;
  animation: pill-in 0.5s ease forwards;
}

@keyframes pill-in {
  from { opacity: 0; transform: translateY(10px) scale(0.9); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

// CTA button
.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 16px 48px;
  background: white;
  color: #533483;
  border-radius: $radius-full;
  font-size: 18px;
  font-weight: 700;
  transition: all $transition-base;
  opacity: 0;
  transform: translateY(20px);
  position: relative;
  overflow: hidden;

  &.show { opacity: 1; transform: translateY(0); transition-delay: 0.9s; }

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba($landing-accent, 0.15), transparent);
    transform: translateX(-100%);
    transition: transform 0.6s;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);

    &::before { transform: translateX(100%); }
    .cta-arrow { transform: translateX(4px); }
  }
}

.cta-arrow {
  transition: transform $transition-fast;
  font-size: 20px;
}

.hero-motto {
  margin-top: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 6px;
  opacity: 0;
  transition: opacity 0.6s ease;

  &.show { opacity: 1; transition-delay: 1.2s; }
}

// Scroll indicator
.scroll-hint {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.6s ease;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;

  &.show { opacity: 1; transition-delay: 1.6s; }
}

.mouse {
  width: 22px;
  height: 34px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-radius: 11px;
  position: relative;
}

.mouse-wheel {
  width: 3px;
  height: 8px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 2px;
  position: absolute;
  top: 6px;
  left: 50%;
  transform: translateX(-50%);
  animation: wheel-scroll 1.8s ease-in-out infinite;
}

@keyframes wheel-scroll {
  0% { transform: translateX(-50%) translateY(0); opacity: 1; }
  100% { transform: translateX(-50%) translateY(12px); opacity: 0; }
}

// Features
.features {
  max-width: 1100px;
  margin: 0 auto;
  padding: 100px 24px;
}

.section-title {
  text-align: center;
  font-size: 32px;
  font-weight: 800;
  color: $text-primary;
  margin-bottom: 48px;
  letter-spacing: -0.5px;
}

.title-brand {
  color: $landing-accent;
  font-size: 36px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.feature-card {
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-xl;
  padding: 32px 24px;
  text-align: center;
  transition: all 0.4s ease;
  opacity: 0;
  transform: translateY(30px);
  position: relative;

  // Animated gradient border on hover
  &::before {
    content: '';
    position: absolute;
    inset: -1px;
    border-radius: inherit;
    background: linear-gradient(135deg, $landing-accent, #a78bfa, $landing-accent);
    background-size: 200% 200%;
    z-index: -1;
    opacity: 0;
    transition: opacity 0.4s ease;
    animation: gradient-rotate 3s linear infinite;
  }

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }

  @for $i from 1 through 4 {
    &:nth-child(#{$i}).visible {
      transition-delay: #{($i - 1) * 0.12}s;
    }
  }

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 40px rgba($landing-accent, 0.1);
    border-color: transparent;

    &::before { opacity: 1; }
  }
}

@keyframes gradient-rotate {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.feature-icon-wrap {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, rgba($landing-accent, 0.08), rgba(#a78bfa, 0.08));
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-icon {
  font-size: 32px;
}

.feature-title {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.6;
}

// How It Works
.how-it-works {
  background: white;
  padding: 100px 24px;
}

.steps {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  gap: 40px;
  justify-content: center;
}

.step-item {
  flex: 1;
  text-align: center;
  position: relative;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.5s ease;

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }

  @for $i from 1 through 3 {
    &:nth-child(#{$i}).visible {
      transition-delay: #{($i - 1) * 0.15}s;
    }
  }

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    top: 24px;
    right: -20px;
    width: 40px;
    height: 2px;
    background: linear-gradient(90deg, $landing-accent, rgba($landing-accent, 0.2));
  }
}

.step-number {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #533483, #0f3460);
  color: white;
  font-size: 20px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 4px 16px rgba(#533483, 0.25);
}

.step-title {
  font-size: 18px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.step-desc {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.5;
}

// CTA Banner
.cta-banner {
  background: linear-gradient(135deg, #1a1a2e, #533483);
  padding: 80px 24px;
  text-align: center;
}

.cta-inner {
  max-width: 600px;
  margin: 0 auto;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease;

  &.visible { opacity: 1; transform: translateY(0); }

  h2 {
    font-size: 28px;
    font-weight: 700;
    color: white;
    margin-bottom: 28px;
    line-height: 1.4;
  }
}

.cta-btn {
  display: inline-block;
  padding: 14px 44px;
  background: white;
  color: #533483;
  border-radius: $radius-full;
  font-size: 17px;
  font-weight: 700;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  }
}

// Footer
.landing-footer {
  text-align: center;
  padding: 40px 24px;
  color: $text-muted;
  font-size: 13px;
  border-top: 1px solid $border-light;
}

.footer-brand {
  display: block;
  font-size: 24px;
  color: $landing-accent;
  margin-bottom: 8px;
}

// Mobile
@media (max-width: $bp-mobile) {
  .hero-brand { font-size: 48px; }
  .hero-title { font-size: 28px; letter-spacing: 1px; }
  .hero-subtitle { font-size: 15px; }
  .hero-cta { padding: 14px 36px; font-size: 16px; }
  .hero-landmarks { gap: 6px; }
  .landmark-pill { font-size: 12px; padding: 4px 12px; }

  .features-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .steps {
    flex-direction: column;
    gap: 32px;
  }

  .step-item:not(:last-child)::after { display: none; }
  .section-title { font-size: 24px; }
  .cta-inner h2 { font-size: 22px; }
}

@media (max-width: $bp-tablet) and (min-width: $bp-mobile) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
