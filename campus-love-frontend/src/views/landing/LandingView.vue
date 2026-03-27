<template>
  <div class="landing-page">
    <nav class="landing-nav" :class="{ scrolled: navScrolled }">
      <div class="nav-inner">
        <div class="nav-brand">
          <span class="nav-logo-text">Campal</span>
        </div>
        <div class="nav-actions">
          <router-link to="/login" class="nav-link">登录</router-link>
          <router-link to="/register" class="nav-btn glow-btn">注册</router-link>
        </div>
      </div>
    </nav>

    <section class="hero">
      <div class="hero-bg">
        <canvas ref="canvasRef" class="interactive-canvas"></canvas>
        <div class="ambient-glow glow-blue"></div>
        <div class="ambient-glow glow-pink"></div>
      </div>

      <div class="hero-content">
        <div class="hero-badge glass-pill" :class="{ show: heroReady }">
          <span class="badge-dot"></span>
          AI 驱动校园社交
        </div>
        <h1 class="hero-brand galaxy-brand" :class="{ show: heroReady }">
          <span class="galaxy-brand-inner">Campal</span>
        </h1>
        <h2 class="hero-title" :class="{ show: heroReady }">在校园里，遇见对的人</h2>
        <p class="hero-subtitle" :class="{ show: heroReady }">
          <span class="subtitle-text">{{ displayedSubtitle }}</span>
          <span class="typing-cursor">|</span>
          <transition name="micro-fade">
            <span
              v-if="microIconVisible"
              class="micro-icon"
              :class="microIconType"
              aria-hidden="true"
            >
              <svg v-if="microIconType === 'heart'" viewBox="0 0 24 24" class="micro-icon-svg">
                <path
                  fill="currentColor"
                  d="M12 21s-6.716-4.432-9.33-8.227C.79 10.403 2.17 6.736 6.03 6.03 8.548 5.57 11 7.075 12 9.5c1-2.425 3.452-3.93 5.97-3.47 3.86.706 5.24 4.374 2.33 8.773C18.716 16.568 12 21 12 21z"
                />
              </svg>
              <svg v-else viewBox="0 0 24 24" class="micro-icon-svg">
                <path
                  fill="currentColor"
                  d="M12 2l1.09 3.26L16 6.5l-2.91 1.24L12 11l-1.09-3.26L8 6.5l2.91-1.24L12 2zm7 8l.91 2.73L22 14l-2.73.91L17 17l-.91-2.73L13.5 14l2.73-.91L19 10zm-14 0l.91 2.73L8 14l-2.73.91L3 17l-.91-2.73L.5 14l2.73-.91L5 10z"
                />
              </svg>
            </span>
          </transition>
        </p>

        <div class="hero-landmarks" :class="{ show: heroReady }">
          <span
            v-for="(lm, i) in landmarks"
            :key="lm"
            class="landmark-pill glass-pill"
            :style="{ animationDelay: `${1.2 + i * 0.15}s` }"
          >
            {{ lm }}
          </span>
        </div>

        <div
          ref="heroCtaWrapRef"
          class="hero-cta-wrap"
          @mouseenter="onHeroCtaEnter"
          @mouseleave="onHeroCtaLeave"
        >
          <router-link to="/register" class="hero-cta cta-warm" :class="{ show: heroReady }">
            <span>开始探索</span>
            <span class="cta-arrow">→</span>
          </router-link>
        </div>
        <p class="hero-motto" :class="{ show: heroReady }">遇见TA · 从校园开始</p>

        <div class="scroll-hint" :class="{ show: heroReady }">
          <div class="mouse">
            <div class="mouse-wheel"></div>
          </div>
        </div>
      </div>
    </section>

    <section class="features" ref="featuresRef">
      <div class="section-header">
        <h2 class="section-title">为什么选择 <span class="title-brand">Campal</span></h2>
        <p class="section-subtitle">告别无聊的三点一线，让算法为你捕捉每一次心动的可能。</p>
      </div>

      <div class="features-grid">
        <div
          v-for="(f, i) in features"
          :key="i"
          class="feature-card glass-card"
          :class="{ visible: featuresVisible }"
        >
          <div class="feature-glow-hover"></div>
          <div class="feature-icon-wrap">
            <span class="feature-icon">{{ f.icon }}</span>
          </div>
          <h3 class="feature-title">{{ f.title }}</h3>
          <p class="feature-desc">{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <section class="how-it-works" ref="howRef">
      <h2 class="section-title">三步开启<span class="text-gradient">平行宇宙</span></h2>
      <div class="timeline-steps">
        <div class="timeline-line"></div>
        <div v-for="(s, i) in steps" :key="i" class="step-item" :class="{ visible: howVisible }">
          <div class="step-node glass-circle">{{ i + 1 }}</div>
          <div class="step-content glass-card">
            <h3 class="step-title">{{ s.title }}</h3>
            <p class="step-desc">{{ s.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="cta-banner" ref="ctaRef">
      <div class="cta-ambient"></div>
      <div class="cta-inner glass-card" :class="{ visible: ctaVisible }">
        <h2>让每一段校园时光，都有温暖的陪伴</h2>
        <p>已经有超过 2 名深大同学在这里相遇</p>
        <router-link to="/register" class="cta-btn cta-warm">立即加入</router-link>
      </div>
    </section>

    <footer class="landing-footer">
      <span class="footer-brand">Campal</span>
      <p>&copy; {{ new Date().getFullYear() }} Campal — 属于Gen Z的校园引力场</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

const landmarks = ['校园发现', '心动时刻', '校园邀约', '真实互动']

const features = [
  {
    icon: '🎯',
    title: 'AI 深度懂你',
    desc: '抛弃无效社交，基于性格图谱与互动偏好，精准捕捉同频共振的人。',
  },
  {
    icon: '💓',
    title: '限时心动',
    desc: '每周开启专属匹配窗口，AI 为你们定制破冰话题与浪漫约会方案。',
  },
  {
    icon: '🛹',
    title: '无缝同行',
    desc: '夜跑、探店、剧本杀...一键发起邀约，把虚拟的共鸣变成现实的相伴。',
  },
  {
    icon: '🛡️',
    title: '校园专属',
    desc: '深大邮箱硬核认证，拒绝校外海王，打造纯粹、安全的信任社交圈。',
  },
]

const steps = [
  {
    title: '身份认证，获取船票',
    desc: '使用深圳大学edu邮箱快速注册，这不仅是门票，更是安全的保障。',
  },
  {
    title: '建立你的灵魂图谱',
    desc: '完善MBTI、兴趣标签与生活碎片，给AI一点时间，让它比你更懂你。',
  },
  {
    title: '迎接未知的相遇',
    desc: '浏览引力场，发起一次邀约，或者静静等待周末的“心动盲盒”。',
  },
]

const featuresRef = ref<HTMLElement>()
const howRef = ref<HTMLElement>()
const ctaRef = ref<HTMLElement>()
const featuresVisible = ref(false)
const howVisible = ref(false)
const ctaVisible = ref(false)
const heroReady = ref(false)
const navScrolled = ref(false)

const heroCtaWrapRef = ref<HTMLElement | null>(null)
const singularityState = ref({ active: false, x: 0, y: 0 })

function updateSingularityTarget() {
  const el = heroCtaWrapRef.value
  if (!el) return
  const r = el.getBoundingClientRect()
  singularityState.value.x = r.left + r.width / 2
  singularityState.value.y = r.top + r.height / 2
}

function onHeroCtaEnter() {
  singularityState.value.active = true
  updateSingularityTarget()
}

function onHeroCtaLeave() {
  singularityState.value.active = false
}

function onSingularityLayout() {
  if (singularityState.value.active) updateSingularityTarget()
}

// Canvas Ref for Interactive Particles
const canvasRef = ref<HTMLCanvasElement | null>(null)
let animationFrameId: number

// Typewriter subtitle + 随机微光图标
const fullSubtitle = '用虚拟推演真实，让本该相遇的人不再擦肩而过。'
const displayedSubtitle = ref('')
let typeTimer: ReturnType<typeof setTimeout> | null = null
const microIconVisible = ref(false)
const microIconType = ref<'heart' | 'spark'>('heart')
let microIconLoopTimer: ReturnType<typeof setTimeout> | null = null
let microIconHideTimer: ReturnType<typeof setTimeout> | null = null

function clearMicroIconTimers() {
  if (microIconLoopTimer) {
    clearTimeout(microIconLoopTimer)
    microIconLoopTimer = null
  }
  if (microIconHideTimer) {
    clearTimeout(microIconHideTimer)
    microIconHideTimer = null
  }
}

function showMicroIconBurst() {
  microIconType.value = Math.random() > 0.5 ? 'heart' : 'spark'
  microIconVisible.value = true
  microIconHideTimer = setTimeout(() => {
    microIconVisible.value = false
  }, 2000)
}

function scheduleRandomMicroIcon() {
  const delay = 6000 + Math.random() * 9000
  microIconLoopTimer = setTimeout(() => {
    showMicroIconBurst()
    scheduleRandomMicroIcon()
  }, delay)
}

function onSubtitleTypedComplete() {
  setTimeout(() => {
    scheduleRandomMicroIcon()
  }, 2800)
}

function typeSubtitle() {
  let i = 0
  const tick = () => {
    if (i <= fullSubtitle.length) {
      displayedSubtitle.value = fullSubtitle.slice(0, i)
      i++
      typeTimer = setTimeout(tick, 80)
    } else {
      onSubtitleTypedComplete()
    }
  }
  typeTimer = setTimeout(tick, 1000)
}

function onScroll() {
  navScrolled.value = window.scrollY > 40
}

// Canvas Particle Logic
function initParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  let width = (canvas.width = window.innerWidth)
  let height = (canvas.height = window.innerHeight)

  class Particle {
    x: number
    y: number
    vx: number
    vy: number
    size: number
    baseColor: string
    constructor() {
      this.x = Math.random() * width
      this.y = Math.random() * height
      this.vx = (Math.random() - 0.5) * 0.5
      this.vy = (Math.random() - 0.5) * 0.5
      this.size = Math.random() * 2 + 1
      this.baseColor = Math.random() > 0.8 ? 'rgba(255, 51, 102, 0.8)' : 'rgba(79, 140, 255, 0.8)'
    }
    update() {
      const s = singularityState.value
      if (s.active) {
        const tx = s.x
        const ty = s.y
        const dx = tx - this.x
        const dy = ty - this.y
        const dist = Math.sqrt(dx * dx + dy * dy) || 0.001
        const pull = 0.17
        const step = Math.min(dist, 160) * pull
        this.x += (dx / dist) * step
        this.y += (dy / dist) * step
        this.vx *= 0.84
        this.vy *= 0.84
        if (dist < 110) {
          const orbit = 0.2
          this.vx += (-dy / dist) * orbit
          this.vy += (dx / dist) * orbit
        }
        return
      }

      this.x += this.vx
      this.y += this.vy
      if (this.x < 0 || this.x > width) this.vx *= -1
      if (this.y < 0 || this.y > height) this.vy *= -1
      const dx = mouse.x - this.x
      const dy = mouse.y - this.y
      const distance = Math.sqrt(dx * dx + dy * dy)
      if (distance < mouse.radius && distance > 0) {
        const forceDirectionX = dx / distance
        const forceDirectionY = dy / distance
        const force = (mouse.radius - distance) / mouse.radius
        this.x -= forceDirectionX * force * 2
        this.y -= forceDirectionY * force * 2
      }
    }
    draw() {
      if (!ctx) return
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
      ctx.fillStyle = this.baseColor
      ctx.fill()
    }
  }

  const particles: Particle[] = []
  const mouse = { x: width / 2, y: height / 2, radius: 150 }

  window.addEventListener('mousemove', (e) => {
    mouse.x = e.clientX
    mouse.y = e.clientY
  })

  window.addEventListener('resize', () => {
    width = canvas.width = window.innerWidth
    height = canvas.height = window.innerHeight
  })

  for (let i = 0; i < 100; i++) particles.push(new Particle())

  function animate() {
    if (!ctx) return
    ctx.clearRect(0, 0, width, height)

    for (let i = 0; i < particles.length; i++) {
      const pi = particles[i]
      if (!pi) continue
      pi.update()
      pi.draw()

      for (let j = i; j < particles.length; j++) {
        const pj = particles[j]
        if (!pj) continue
        const dx = pi.x - pj.x
        const dy = pi.y - pj.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 100) {
          ctx.beginPath()
          ctx.strokeStyle = `rgba(79, 140, 255, ${Math.max(0, 0.22 - dist / 100)})`
          ctx.lineWidth = 0.5
          ctx.moveTo(pi.x, pi.y)
          ctx.lineTo(pj.x, pj.y)
          ctx.stroke()
        }
      }
    }
    animationFrameId = requestAnimationFrame(animate)
  }
  animate()
}

onMounted(() => {
  setTimeout(() => {
    heroReady.value = true
  }, 200)
  typeSubtitle()
  initParticles()

  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('scroll', onSingularityLayout, true)
  window.addEventListener('resize', onSingularityLayout)

  const observer = new IntersectionObserver(
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
  void nextTick(() => {
    if (featuresRef.value) observer.observe(featuresRef.value)
    if (howRef.value) observer.observe(howRef.value)
    if (ctaRef.value) observer.observe(ctaRef.value)
  })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('scroll', onSingularityLayout, true)
  window.removeEventListener('resize', onSingularityLayout)
  if (typeTimer) clearTimeout(typeTimer)
  clearMicroIconTimers()
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
})
</script>

<style lang="scss" scoped>
// 内置变量设定：彻底暗黑+科技与心动的撞色
$bg-dark: #070b19; // 深邃宇宙蓝
$bg-card: rgba(20, 26, 45, 0.4);
$accent-blue: #4f8cff;
$accent-pink: #ff3366; // 增加心动粉，打破冷漠
$accent-orange: #ff7b54;
$text-main: #ffffff;
$text-sub: rgba(255, 255, 255, 0.65);

// 注意：overflow-x: hidden + overflow-y: visible 在规范中会把 y 当成 auto，从而纵向裁切内容。
// 用 clip 只裁横向，或显式 overflow-y: visible + clip（现代浏览器）。
.landing-page {
  min-height: 100vh;
  background: $bg-dark;
  color: $text-main;
  overflow-x: clip;
  overflow-y: visible;
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    'Helvetica Neue',
    Arial,
    sans-serif;
}

// 通用毛玻璃组件
.glass-card {
  background: $bg-card;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
}

.glass-pill {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
}

// 艺术字（导航 / 页脚 / 区块标题；首屏大 Logo 见 .galaxy-brand-inner）
.nav-logo-text,
.footer-brand,
.title-brand {
  font-family: 'Pacifico', cursive;
  background: linear-gradient(135deg, #fff, #a78bfa);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

// Nav
.landing-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 12px 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &.scrolled {
    background: rgba(7, 11, 25, 0.7);
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-logo-text {
  font-size: 26px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}
.nav-link {
  font-size: 15px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.82);
  text-decoration: none;
  transition: color 0.3s;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45), 0 0 18px rgba(255, 255, 255, 0.12);
  &:hover {
    color: $text-main;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35), 0 0 22px rgba(255, 255, 255, 0.2);
  }
}

.nav-btn {
  padding: 8px 24px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.34);
  transition: all 0.3s;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4), 0 0 14px rgba(255, 255, 255, 0.15);

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    border-color: rgba(255, 255, 255, 0.45);
    transform: translateY(-1px);
    text-shadow: 0 1px 3px rgba(0, 0, 0, 0.35), 0 0 18px rgba(255, 255, 255, 0.22);
  }
}

// Hero Section（仅裁切横向溢出；纵向 visible，避免 flex 居中 + 内容过长时裁掉 Logo 上沿）
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-x: hidden;
  overflow-y: visible;
}

.hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.interactive-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.4;
  z-index: -1;
  pointer-events: none;
}
.glow-blue {
  width: 60vw;
  height: 60vw;
  top: -10%;
  left: -10%;
  background: $accent-blue;
}
.glow-pink {
  width: 40vw;
  height: 40vw;
  bottom: -10%;
  right: -5%;
  background: $accent-pink;
  opacity: 0.2;
}

.hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  max-width: 800px;
  padding: 72px 24px 0;
  margin-top: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  font-size: 14px;
  color: $text-main;
  margin-bottom: 32px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s;
  &.show {
    opacity: 1;
    transform: translateY(0);
  }
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 10px #22c55e;
}

.hero-brand.galaxy-brand {
  margin: 0 0 14px;
  padding: 0.44em 0 0.2em;
  line-height: 1.48;
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  overflow: visible;
  transform-origin: center center;
  opacity: 0;
  transform: translateY(30px) scale(0.95);
  transition: all 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
  &.show {
    opacity: 1;
    transform: translateY(0) scale(1);
    transition-delay: 0.1s;
  }
}

// 星系微粒 + 悬停流光脉冲（不改变排版尺寸）
.galaxy-brand-inner {
  font-family: 'Pacifico', cursive;
  font-size: 84px;
  line-height: 1.48;
  display: inline-block;
  padding: 0.2em 0.05em 0.14em;
  box-decoration-break: clone;
  -webkit-box-decoration-break: clone;
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-image:
    linear-gradient(
      95deg,
      transparent 0%,
      rgba(255, 255, 255, 0.08) 42%,
      rgba(255, 255, 255, 0.55) 50%,
      rgba(255, 255, 255, 0.08) 58%,
      transparent 100%
    ),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.95) 0.35px, transparent 0.55px),
    linear-gradient(135deg, #ffffff 0%, #c4b5fd 48%, #a78bfa 100%);
  background-size:
    240% 100%,
    3px 3px,
    100% 100%;
  background-position:
    -60% 0,
    0 0,
    0 0;
  background-repeat: no-repeat, repeat, no-repeat;
  background-blend-mode: soft-light, overlay, normal;
  filter: drop-shadow(0 10px 32px rgba(79, 140, 255, 0.28));
  cursor: default;
}

.galaxy-brand:hover .galaxy-brand-inner {
  animation: logoStarSweep 1.15s cubic-bezier(0.25, 0.8, 0.2, 1) forwards;
}

@keyframes logoStarSweep {
  0% {
    background-position:
      -90% 0,
      0 0,
      0 0;
    filter: drop-shadow(0 10px 32px rgba(79, 140, 255, 0.28));
  }
  35% {
    filter: drop-shadow(0 14px 40px rgba(255, 255, 255, 0.42));
  }
  100% {
    background-position:
      190% 0,
      0 0,
      0 0;
    filter: drop-shadow(0 10px 36px rgba(167, 139, 250, 0.45));
  }
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 24px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s;
  &.show {
    opacity: 1;
    transform: translateY(0);
    transition-delay: 0.2s;
  }
}

.hero-subtitle {
  font-size: 20px;
  color: $text-sub;
  margin-bottom: 40px;
  min-height: 30px;
  opacity: 0;
  transition: opacity 0.5s;
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px 6px;
  max-width: 100%;
  &.show {
    opacity: 1;
    transition-delay: 0.4s;
  }
}

.subtitle-text {
  text-align: center;
}

.micro-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 2px;
  vertical-align: middle;
  filter: drop-shadow(0 0 6px rgba(255, 107, 140, 0.85)) drop-shadow(0 0 14px rgba(79, 140, 255, 0.45));
  &.heart {
    color: #ff6b8a;
  }
  &.spark {
    color: #a78bfa;
  }
}

.micro-icon-svg {
  width: 14px;
  height: 14px;
  display: block;
}

.micro-fade-enter-active,
.micro-fade-leave-active {
  transition: opacity 0.45s ease;
}
.micro-fade-enter-from,
.micro-fade-leave-to {
  opacity: 0;
}

.typing-cursor {
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% {
    opacity: 0;
  }
}

.hero-landmarks {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 48px;
}
.landmark-pill {
  padding: 6px 20px;
  font-size: 14px;
  opacity: 0;
  animation: fadeUp 0.6s forwards;
}

.hero-cta-wrap {
  display: inline-block;
  position: relative;
  z-index: 2;
}

.hero-cta.cta-warm {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 18px 56px;
  border-radius: 999px;
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  color: white;
  font-size: 18px;
  font-weight: 700;
  text-decoration: none;
  box-shadow:
    0 8px 28px rgba(255, 51, 102, 0.38),
    0 10px 40px rgba(255, 51, 102, 0.4),
    0 0 72px rgba(255, 123, 84, 0.14);
  transition: all 0.3s;
  opacity: 0;
  transform: translateY(20px);

  &.show {
    opacity: 1;
    transform: translateY(0);
    transition-delay: 0.8s;
  }
  &:hover {
    transform: translateY(-3px);
    box-shadow:
      0 12px 36px rgba(255, 51, 102, 0.45),
      0 18px 52px rgba(255, 51, 102, 0.42),
      0 0 88px rgba(255, 123, 84, 0.2);
    .cta-arrow {
      transform: translateX(5px);
    }
  }
}
.cta-arrow {
  transition: transform 0.3s;
}

.hero-motto {
  margin-top: 24px;
  font-size: 14px;
  color: $text-sub;
  letter-spacing: 4px;
  opacity: 0;
  transition: 0.5s;
  &.show {
    opacity: 1;
    transition-delay: 1s;
  }
}

.scroll-hint {
  margin-top: 60px;
  display: flex;
  justify-content: center;
  opacity: 0;
  &.show {
    opacity: 0.5;
    transition-delay: 1.2s;
  }
}
.mouse {
  width: 24px;
  height: 36px;
  border: 2px solid #fff;
  border-radius: 12px;
  position: relative;
}
.mouse-wheel {
  width: 4px;
  height: 8px;
  background: #fff;
  border-radius: 2px;
  position: absolute;
  top: 6px;
  left: 50%;
  transform: translateX(-50%);
  animation: scroll 1.5s infinite;
}
@keyframes scroll {
  0% {
    transform: translate(-50%, 0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, 12px);
    opacity: 0;
  }
}
@keyframes fadeUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Sections Common
.features,
.how-it-works,
.cta-banner {
  padding: 120px 24px;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
}
.section-header {
  text-align: center;
  margin-bottom: 64px;
}
.section-title {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 16px;
}
.section-subtitle {
  font-size: 16px;
  color: $text-sub;
}
.text-gradient {
  background: linear-gradient(90deg, $accent-blue, $accent-pink);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

// Features Grid
.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;
}
.feature-card {
  padding: 40px;
  position: relative;
  overflow: hidden;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.6s cubic-bezier(0.2, 0.8, 0.2, 1);
  &.visible {
    opacity: 1;
    transform: translateY(0);
  }
  @for $i from 1 through 4 {
    &:nth-child(#{$i}).visible {
      transition-delay: #{($i - 1) * 0.1}s;
    }
  }

  .feature-glow-hover {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at center, rgba(79, 140, 255, 0.15) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s;
    pointer-events: none;
    z-index: 0;
  }

  &:hover {
    border-color: rgba(255, 255, 255, 0.2);
    transform: translateY(-5px);
    .feature-glow-hover {
      opacity: 1;
    }
    // 第二个卡片特殊粉色高光
    &:nth-child(2) .feature-glow-hover {
      background: radial-gradient(circle at center, rgba(255, 51, 102, 0.15) 0%, transparent 70%);
    }
  }
}
.feature-icon-wrap {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}
.feature-icon {
  font-size: 28px;
}
.feature-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}
.feature-desc {
  font-size: 15px;
  color: $text-sub;
  line-height: 1.6;
  position: relative;
  z-index: 1;
}

// Timeline (How it works)
.timeline-steps {
  position: relative;
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 0;
}
.timeline-line {
  position: absolute;
  left: 32px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, transparent, rgba(79, 140, 255, 0.3), transparent);
}
.step-item {
  display: flex;
  gap: 40px;
  margin-bottom: 64px;
  opacity: 0;
  transform: translateX(-30px);
  transition: all 0.6s;
  &.visible {
    opacity: 1;
    transform: translateX(0);
  }
  @for $i from 1 through 3 {
    &:nth-child(#{$i + 1}).visible {
      transition-delay: #{($i - 1) * 0.2}s;
    }
  }
}
.step-node {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  background: $bg-dark;
  border: 2px solid $accent-blue;
  color: $accent-blue;
  position: relative;
  z-index: 2;
  box-shadow: 0 0 20px rgba(79, 140, 255, 0.2);
}
.glass-circle {
  border-radius: 50%;
}
.step-content {
  flex-grow: 1;
  padding: 32px;
  border-left: 4px solid transparent;
  transition: border-color 0.3s;
  &:hover {
    border-left-color: $accent-pink;
  }
}
.step-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
}
.step-desc {
  font-size: 15px;
  color: $text-sub;
  line-height: 1.6;
}

// CTA Banner
.cta-banner {
  padding-top: 40px;
}
.cta-inner {
  padding: 80px 40px;
  text-align: center;
  border: 1px solid rgba(255, 51, 102, 0.2);
  background: linear-gradient(180deg, rgba(20, 26, 45, 0.6) 0%, rgba(255, 51, 102, 0.05) 100%);
  opacity: 0;
  transform: scale(0.95);
  transition: all 0.8s;
  &.visible {
    opacity: 1;
    transform: scale(1);
  }

  h2 {
    font-size: 36px;
    font-weight: 800;
    margin-bottom: 16px;
  }
  p {
    color: $text-sub;
    margin-bottom: 40px;
    font-size: 18px;
  }
}

.cta-btn.cta-warm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 16px 48px;
  border-radius: 999px;
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  color: white;
  font-size: 16px;
  font-weight: 700;
  text-decoration: none;
  box-shadow:
    0 8px 28px rgba(255, 51, 102, 0.38),
    0 10px 40px rgba(255, 51, 102, 0.4),
    0 0 72px rgba(255, 123, 84, 0.14);
  transition: all 0.3s;
  &:hover {
    transform: translateY(-2px);
    box-shadow:
      0 12px 36px rgba(255, 51, 102, 0.45),
      0 18px 52px rgba(255, 51, 102, 0.42),
      0 0 88px rgba(255, 123, 84, 0.2);
  }
}

// Footer
.landing-footer {
  text-align: center;
  padding: 60px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  color: $text-sub;
}
.footer-brand {
  display: block;
  font-size: 28px;
  margin-bottom: 16px;
}

// Mobile Response
@media (max-width: 768px) {
  .hero-content {
    padding-top: calc(86px + env(safe-area-inset-top));
  }

  .galaxy-brand-inner {
    font-size: 56px;
  }
  .hero-title {
    font-size: 28px;
  }
  .features-grid {
    grid-template-columns: 1fr;
  }
  .step-item {
    gap: 20px;
  }
  .timeline-line {
    left: 24px;
  }
  .step-node {
    width: 48px;
    height: 48px;
    font-size: 18px;
  }
  .cta-inner h2 {
    font-size: 28px;
  }
}
</style>
