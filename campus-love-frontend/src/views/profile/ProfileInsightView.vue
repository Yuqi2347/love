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
          <svg viewBox="0 0 300 300" class="radar-svg">
            <defs>
              <linearGradient id="radarFill" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#a78bfa" stop-opacity="0.4" />
                <stop offset="100%" stop-color="#6366f1" stop-opacity="0.15" />
              </linearGradient>
            </defs>
            <!-- 背景网格 -->
            <g v-for="i in 5" :key="'grid-' + i">
              <polygon
                :points="gridPoints(i)"
                fill="none"
                stroke="rgba(99,102,241,0.2)"
                stroke-width="0.5"
              />
            </g>
            <!-- 数据区域 -->
            <polygon
              v-if="radarPoints"
              :points="radarPoints"
              fill="url(#radarFill)"
              stroke="#6366f1"
              stroke-width="2"
            />
            <!-- 顶点标签 -->
            <g v-for="(pos, idx) in labelPositions" :key="'label-' + idx">
              <text
                :x="pos.x"
                :y="pos.y"
                class="radar-label"
                text-anchor="middle"
              >
                {{ oceanLabels[idx] ?? '' }}
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

const hasOcean = computed(() => {
  const p = profile.value
  if (!p) return false
  const vals = [p.oceanO, p.oceanC, p.oceanE, p.oceanA, p.oceanN]
  return vals.some(v => v != null && !Number.isNaN(v))
})

// 雷达图中心 150,150，半径 100
function gridPoints(level: number): string {
  const cx = 150
  const cy = 150
  const r = (100 / 5) * level
  const pts: string[] = []
  for (let i = 0; i < 5; i++) {
    const angle = (Math.PI * 2 * i) / 5 - Math.PI / 2
    pts.push(`${cx + r * Math.cos(angle)},${cy + r * Math.sin(angle)}`)
  }
  return pts.join(' ')
}

const radarPoints = computed(() => {
  const p = profile.value
  if (!p || !hasOcean.value) return ''
  const vals = [
    p.oceanO ?? 5,
    p.oceanC ?? 5,
    p.oceanE ?? 5,
    p.oceanA ?? 5,
    p.oceanN ?? 5,
  ].map(v => Math.max(0, Math.min(10, Number(v))))
  const cx = 150
  const cy = 150
  const r = 90
  const pts: string[] = []
  for (let i = 0; i < 5; i++) {
    const angle = (Math.PI * 2 * i) / 5 - Math.PI / 2
    const vr = ((vals[i] ?? 5) / 10) * r
    pts.push(`${cx + vr * Math.cos(angle)},${cy + vr * Math.sin(angle)}`)
  }
  return pts.join(' ')
})

const labelPositions = [
  { x: 150, y: 45 },
  { x: 245, y: 165 },
  { x: 195, y: 275 },
  { x: 105, y: 275 },
  { x: 55, y: 165 },
]

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
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 17px;
  font-weight: 700;
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
  width: 260px;
  height: 260px;
}

.radar-label {
  font-size: 12px;
  fill: #475569;
  font-weight: 500;
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
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #0369a1;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}
</style>
