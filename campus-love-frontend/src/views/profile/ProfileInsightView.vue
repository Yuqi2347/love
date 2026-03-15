<template>
  <div class="profile-insight-page">
    <div class="insight-header">
      <button class="back-btn" @click="$router.push('/profile')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <h1 class="insight-title">我的性格画像</h1>
    </div>

    <div v-if="loading" class="insight-loading">
      <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
      <p>正在加载画像...</p>
    </div>

    <div v-else-if="error" class="insight-error">
      <el-icon class="error-icon" :size="48"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="loadProfile">重试</button>
    </div>

    <div v-else class="insight-content">
      <!-- OCEAN 雷达图 -->
      <div v-if="hasOcean" class="ocean-card">
        <h3 class="card-title">OCEAN 五维人格</h3>
        <p class="card-hint">开放性 · 尽责性 · 外向性 · 宜人性 · 神经质</p>
        <div class="radar-wrap">
          <svg viewBox="0 0 320 320" class="radar-svg">
            <defs>
              <linearGradient id="radarFill" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#8b5cf6" stop-opacity="0.5" />
                <stop offset="100%" stop-color="#3b82f6" stop-opacity="0.2" />
              </linearGradient>
              <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                <feGaussianBlur stdDeviation="4" result="blur" />
                <feComposite in="SourceGraphic" in2="blur" operator="over" />
              </filter>
            </defs>
            
            <!-- 背景网格 -->
            <g v-for="i in 5" :key="'grid-' + i">
              <polygon
                :points="gridPoints(i)"
                fill="none"
                stroke="rgba(148, 163, 184, 0.2)"
                stroke-width="1"
                stroke-dasharray="4 4"
              />
            </g>
            
            <!-- 数据区域 (带入场动画) -->
            <g class="radar-data-group">
              <polygon
                v-if="radarPointsStr"
                :points="radarPointsStr"
                fill="url(#radarFill)"
                stroke="#6366f1"
                stroke-width="2"
                filter="url(#glow)"
                class="radar-polygon"
              />
              <!-- 数据顶点圆点 -->
              <circle
                v-for="(pt, idx) in radarDataPoints"
                :key="'pt-' + idx"
                :cx="pt.x"
                :cy="pt.y"
                r="4"
                fill="#fff"
                stroke="#6366f1"
                stroke-width="2"
                class="radar-point"
                :style="{ animationDelay: `${idx * 0.1}s` }"
              />
            </g>
            
            <!-- 顶点标签 + 数值 -->
            <g v-for="(pos, idx) in labelPositions" :key="'label-' + idx">
              <text :x="pos.x" :y="pos.y" text-anchor="middle" class="radar-text-group">
                <tspan class="radar-label">{{ oceanLabels[idx] ?? '' }}</tspan>
                <tspan v-if="oceanValues[idx] != null" :x="pos.x" dy="20" class="radar-value">{{ oceanValues[idx] }}</tspan>
              </text>
            </g>
          </svg>
        </div>
      </div>

      <!-- 无 OCEAN 时提示 -->
      <div v-else class="ocean-empty">
        <el-icon class="empty-icon" :size="56"><DataAnalysis /></el-icon>
        <p>暂无 OCEAN 人格数据</p>
        <p class="empty-hint">完善资料、多发动态，系统将逐步生成你的性格画像</p>
      </div>

      <!-- AI 信息授权引导（有画像时提示） -->
      <div v-if="hasOcean || (profile?.naturalLanguageTags?.length)" class="disclosure-hint-card">
        <p class="disclosure-hint-text">您可设置哪些信息用于 AI 生成破冰话题</p>
        <button class="disclosure-hint-btn" @click="$router.push('/profile?openAiDisclosure=1')">
          去设置
        </button>
      </div>

      <!-- 自然语言标签 -->
      <div v-if="profile && profile.naturalLanguageTags && profile.naturalLanguageTags.length" class="tags-card">
        <h3 class="card-title">性格标签</h3>
        <div class="tag-list">
          <span v-for="tag in profile.naturalLanguageTags" :key="tag" class="tag-item">{{ tag }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, Loading, WarningFilled, DataAnalysis } from '@element-plus/icons-vue'
import { getMyAiProfile, type UserAiProfile } from '@/api/userApi'

const profile = ref<UserAiProfile | null>(null)
const loading = ref(true)
const error = ref('')

const oceanLabels = ['开放性', '尽责性', '外向性', '宜人性', '神经质']

const oceanValues = computed(() => {
  const p = profile.value
  if (!p) return [null, null, null, null, null]
  return [p.oceanO, p.oceanC, p.oceanE, p.oceanA, p.oceanN].map(v =>
    v != null && !Number.isNaN(Number(v)) ? Math.round(Number(v)) : null
  )
})

const hasOcean = computed(() => {
  const p = profile.value
  if (!p) return false
  const vals = [p.oceanO, p.oceanC, p.oceanE, p.oceanA, p.oceanN]
  return vals.some(v => v != null && !Number.isNaN(v))
})

// 雷达图中心 160,160，半径 100
function gridPoints(level: number): string {
  const cx = 160
  const cy = 160
  const r = (100 / 5) * level
  const pts: string[] = []
  for (let i = 0; i < 5; i++) {
    const angle = (Math.PI * 2 * i) / 5 - Math.PI / 2
    pts.push(`${cx + r * Math.cos(angle)},${cy + r * Math.sin(angle)}`)
  }
  return pts.join(' ')
}

const radarDataPoints = computed(() => {
  const p = profile.value
  if (!p || !hasOcean.value) return []
  const vals = [
    p.oceanO ?? 50,
    p.oceanC ?? 50,
    p.oceanE ?? 50,
    p.oceanA ?? 50,
    p.oceanN ?? 50,
  ].map(v => Math.max(0, Math.min(100, Number(v))))
  const cx = 160
  const cy = 160
  const r = 90
  const pts = []
  for (let i = 0; i < 5; i++) {
    const angle = (Math.PI * 2 * i) / 5 - Math.PI / 2
    const vr = ((vals[i] ?? 50) / 100) * r
    pts.push({
      x: cx + vr * Math.cos(angle),
      y: cy + vr * Math.sin(angle)
    })
  }
  return pts
})

const radarPointsStr = computed(() => radarDataPoints.value.map(p => `${p.x},${p.y}`).join(' '))

// 动态计算标签位置
const labelPositions = computed(() => {
  const cx = 160
  const cy = 160
  const r = 135 // 标签半径
  return Array.from({ length: 5 }).map((_, i) => {
    const angle = (Math.PI * 2 * i) / 5 - Math.PI / 2
    return {
      x: cx + r * Math.cos(angle),
      // 微调 Y 轴，让顶部标签稍微高一点，底部标签稍微低一点
      y: cy + r * Math.sin(angle) + (i === 0 ? -10 : (i === 2 || i === 3 ? 15 : 0))
    }
  })
})

async function loadProfile() {
  loading.value = true
  error.value = ''
  try {
    const res = await getMyAiProfile()
    profile.value = res.data.data ?? null
  } catch (e) {
    error.value = '加载失败，请稍后重试'
    profile.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<style lang="scss" scoped>
.profile-insight-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  padding-bottom: 40px;
}

.insight-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 12px;
  background: #f1f5f9;
  color: #475569;
  cursor: pointer;
  transition: background 0.2s;
  &:hover { background: #e2e8f0; }
}

.insight-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.insight-loading,
.insight-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: #64748b;
  .loading-icon { animation: spin 1s linear infinite; }
  .error-icon { color: #f59e0b; margin-bottom: 12px; }
  .retry-btn {
    margin-top: 16px;
    padding: 10px 24px;
    border: none;
    border-radius: 10px;
    background: #6366f1;
    color: #fff;
    font-weight: 500;
    cursor: pointer;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.insight-content {
  padding: 24px 20px;
  max-width: 420px;
  margin: 0 auto;
}

.ocean-card,
.ocean-empty,
.tags-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.8);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; height: 120px;
    background: linear-gradient(180deg, rgba(99, 102, 241, 0.03) 0%, rgba(255, 255, 255, 0) 100%);
    pointer-events: none;
  }
}

.card-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.card-hint {
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 20px 0;
}

.radar-wrap {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.radar-svg {
  width: 100%;
  max-width: 320px;
  height: auto;
  overflow: visible;
}

.radar-data-group {
  transform-origin: 160px 160px;
  animation: radar-enter 1s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.radar-polygon {
  transition: all 0.5s ease;
}

.radar-point {
  opacity: 0;
  animation: point-enter 0.4s ease forwards;
}

@keyframes radar-enter {
  0% { transform: scale(0.5); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes point-enter {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.radar-label {
  font-size: 13px;
  fill: #64748b;
  font-weight: 600;
}

.radar-value {
  font-size: 14px;
  fill: #6366f1;
  font-weight: 800;
}

.radar-text-group {
  animation: fade-in 0.8s ease forwards;
  animation-delay: 0.5s;
  opacity: 0;
}

@keyframes fade-in {
  to { opacity: 1; }
}

.ocean-empty {
  text-align: center;
  padding: 48px 24px;
  .empty-icon { color: #cbd5e1; margin-bottom: 16px; }
  p { margin: 0; color: #64748b; }
  .empty-hint { font-size: 13px; margin-top: 8px; color: #94a3b8; }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.disclosure-hint-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
  border-radius: 16px;
  border: 1px solid rgba($primary, 0.12);
}

.disclosure-hint-text {
  margin: 0;
  font-size: 14px;
  color: $text-secondary;
}

.disclosure-hint-btn {
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: $primary;
  background: rgba($primary, 0.1);
  border: 1px solid $primary;
  border-radius: 10px;
  cursor: pointer;

  &:hover {
    background: rgba($primary, 0.15);
  }
}

.tag-item {
  padding: 8px 14px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #334155;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}
</style>
