<template>
  <div class="result-page">
    <div v-if="loading" class="loading-state">
      <div class="loading-orbit">
        <span class="loading-orbit__ring ring-a" />
        <span class="loading-orbit__ring ring-b" />
        <span class="loading-orbit__heart heartbeat-anim">♡</span>
      </div>
      <p class="loading-title">正在解密心动档案</p>
      <p class="loading-desc">引力场正在为你拉近缘分的距离...</p>
    </div>

    <div v-else-if="!result?.matched" class="empty-state">
      <div class="empty-state__icon float-anim">✨</div>
      <h2>本周星轨暂未交汇</h2>
      <p>你的心动档案已保留，下周继续帮你寻找。</p>
      <button class="glass-btn mt-6" @click="$router.replace('/moment')">返回引力场</button>
    </div>

    <div v-else class="result-shell">
      <section class="result-hero glass-panel panel-entrance">
        <div class="hero-badges">
          <span class="hero-badge glass-pill"><span class="pulse-dot"></span> 心动解密</span>
          <span class="hero-week glass-pill">本周结果</span>
        </div>

        <div class="hero-content">
          <p class="hero-kicker">MOMENT REVEAL</p>
          <h1 class="hero-title text-gradient-warm">{{ result.yuanfenTitle || '刚好同频' }}</h1>
          <p class="hero-desc">不需要数字证明什么。先看看是什么把你们轻轻推到了同一个地方。</p>
        </div>

        <nav class="tuner-capsule glass-pill-light mt-6" role="tablist">
          <button
            v-for="(label, index) in screenLabels"
            :key="label"
            type="button"
            role="tab"
            class="tuner-btn"
            :class="{ active: activeScreen === index + 1, locked: index === 3 && !result.datePrepUnlocked }"
            :disabled="index === 3 && !result.datePrepUnlocked"
            :aria-selected="activeScreen === index + 1"
            @click="goScreen(index + 1)"
          >
            {{ label }}
            <el-icon v-if="index === 3 && !result.datePrepUnlocked" class="ml-1"><Lock /></el-icon>
          </button>
        </nav>
      </section>

      <section class="content-sheet glass-panel panel-entrance">
        <template v-if="activeScreen === 1">
          <header class="sheet-header">
            <span class="sheet-badge glass-pill">SCREEN 01</span>
            <h2>心动揭晓</h2>
            <p>先认识这个让你产生好感的人，再慢慢打开后面的故事。</p>
          </header>

          <div class="content-stack">
            <button type="button" class="match-card match-card--hero glass-card-light" @click="goProfile">
              <span class="match-card__glow" />
              <span class="match-card__spark spark-a" />
              <span class="match-card__spark spark-b" />
              
              <div class="match-card__main">
                <div class="avatar-glow-wrap" style="--glow-color: rgba(255,51,102,0.4)">
                  <img :src="avatarSrc" class="match-avatar" alt="" />
                </div>
                <div class="match-info">
                  <p class="match-card__label">心动对象</p>
                  <div class="match-title-row">
                    <h3>{{ result.nickname || '这位同学' }}</h3>
                  </div>
                  <div class="match-meta">
                    <span v-if="result.age">{{ result.age }}岁</span>
                    <span v-if="result.school">{{ result.school }}</span>
                    <span v-if="result.major">{{ result.major }}</span>
                    <span v-if="result.grade">{{ result.grade }}</span>
                  </div>
                </div>
                <span class="match-card__arrow">›</span>
              </div>

              <div v-if="showMatchScore" class="match-score-block glass-pill-light" aria-label="综合匹配度">
                <div class="match-score-block__row">
                  <span class="match-score-block__label">综合匹配度：</span>
                  <span class="match-score-block__value text-gradient-warm">{{ result.matchScorePercent }}%</span>
                </div>
              </div>

              <div v-if="result.bio" class="match-bio">{{ result.bio }}</div>
              
              <div v-if="(result.mbti || result.zodiac || (result.complementaryModes?.length))" class="match-tags">
                <span v-if="result.mbti" class="info-tag">{{ result.mbti }}</span>
                <span v-if="result.zodiac" class="info-tag">{{ result.zodiac }}</span>
                <span v-for="mode in result.complementaryModes || []" :key="mode" class="info-tag info-tag--soft">
                  {{ mode }}
                </span>
              </div>
              <p class="match-card__hint">点击卡片查看 TA 的引力主页</p>
            </button>

            <article v-if="result.complementaryModes?.length" class="lux-card glass-card-light">
              <h3 class="lux-card__title">你们的默契</h3>
              <div class="tag-row">
                <span v-for="mode in result.complementaryModes" :key="mode" class="glass-pill text-accent-pink px-3 py-1 text-sm font-bold">{{ mode }}</span>
              </div>
            </article>
          </div>

          <div class="sheet-actions">
            <button type="button" class="glow-btn-warm w-full" @click="goScreen(2)">查看你们的缘分</button>
          </div>
        </template>

        <template v-else-if="activeScreen === 2">
          <header class="sheet-header">
            <span class="sheet-badge glass-pill">SCREEN 02</span>
            <h2>你们之间</h2>
            <p>不是一瞬间的巧合，而是很多细小偏好在同一个方向上靠拢。</p>
          </header>

          <div class="content-stack">
            <article v-for="(text, index) in insightCards" :key="index" class="insight-card glass-card-light">
              <div class="insight-card__index">0{{ index + 1 }}</div>
              <div class="insight-card__body">
                <h3>{{ insightTitles[index] }}</h3>
                <p>{{ text }}</p>
              </div>
            </article>

            <article v-if="result.dimensionLabels?.length" class="lux-card glass-card-light">
              <h3 class="lux-card__title">关键词</h3>
              <div class="tag-row">
                <span v-for="item in result.dimensionLabels" :key="item" class="glass-pill-light text-main px-3 py-1 text-sm font-bold">{{ item }}</span>
              </div>
            </article>

            <article v-if="result.goldenSentence" class="quote-card glass-card-light">
              <h3 class="quote-card__title">专属金句</h3>
              <p class="quote-card__text text-gradient-warm">{{ result.goldenSentence }}</p>
            </article>
          </div>

          <div class="sheet-actions two-btn-layout">
            <button type="button" class="glass-btn" @click="goScreen(1)">回到揭晓</button>
            <button type="button" class="glow-btn-warm" @click="goScreen(3)">继续了解 TA</button>
          </div>
        </template>

        <template v-else-if="activeScreen === 3">
          <header class="sheet-header">
            <span class="sheet-badge glass-pill">SCREEN 03</span>
            <h2>关于 TA</h2>
            <p>现在你可以决定，是立刻往前一步，还是留一点时间慢慢靠近。</p>
          </header>

          <div class="content-stack">
            <article class="lux-card glass-card-light">
              <h3 class="lux-card__title">对 TA 的印象</h3>
              <p class="lux-card__paragraph">{{ result.aboutMatchedUser || 'TA身上有一种适合慢慢了解的气质。' }}</p>
            </article>

            <article class="decision-card glass-card-light">
              <p class="decision-card__title text-gradient-warm">{{ decisionHint }}</p>
              <p v-if="decisionDetail" class="decision-card__detail">{{ decisionDetail }}</p>

              <div class="decision-card__buttons">
                <button class="glow-btn-warm" :disabled="choiceLocked || confirming" @click="handleChoice('YUE')">
                  {{ confirming && pendingChoice === 'YUE' ? '提交中...' : '心动，约起来' }}
                </button>
                <button class="glass-btn" :disabled="choiceLocked || confirming" @click="handleChoice('GUANZHU')">
                  {{ confirming && pendingChoice === 'GUANZHU' ? '提交中...' : '先关注，慢慢来' }}
                </button>
              </div>

              <div class="decision-card__links">
                <button class="text-action" @click="goProfile">查看 TA 的主页</button>
                <button class="text-action" @click="goChat">去打招呼</button>
              </div>
              
              <div v-if="result.confirmStatus === 'BOTH_YUE' && result.datePrepUnlocked" class="mt-4 p-3 glass-pill-light border-accent-blue">
                <p class="text-xs text-main m-0">🎉 第四屏已解锁：进入后将加载 AI 约会建议，并开启「三步约会协商」。</p>
              </div>
            </article>
          </div>

          <div class="sheet-actions two-btn-layout">
            <button type="button" class="glass-btn" @click="goScreen(2)">回看缘分</button>
            <button type="button" class="glow-btn-warm" :disabled="!result.datePrepUnlocked" @click="openDatePrep">
              {{ result.datePrepUnlocked ? '查看约会准备' : '等待双方确认' }}
            </button>
          </div>
        </template>

        <template v-else>
          <header class="sheet-header">
            <span class="sheet-badge glass-pill">SCREEN 04</span>
            <h2>去见 TA</h2>
            <p>下方先展示 AI 约会建议，同步可进行三步协商；你可先独立完成选项，无需等对方。</p>
          </header>

          <div v-if="!result.datePrepUnlocked" class="locked-card glass-card-light">
            <div class="text-4xl mb-2">🔒</div>
            <p class="locked-card__title text-gradient-warm">星轨尚未完全重合</p>
            <p>双方都选择“心动，约起来”后，这里会出现三步协商与专属约会建议。</p>
          </div>

          <div v-if="result.datePrepUnlocked && prepLoading" class="prep-loading">
            <div class="prep-loading__spinner pulse-ring" />
            <p class="text-main font-bold">正在为你生成第一次见面的完美方案...</p>
          </div>

          <div v-else-if="result.datePrepUnlocked && datePrep" class="content-stack">
            <article class="lux-card glass-card-light">
              <h3 class="lux-card__title">约会方式推荐</h3>
              <h3 class="text-gradient-warm">{{ datePrep.dateSceneType }}</h3>
              <p>{{ datePrep.dateSuggestion }}</p>
            </article>

            <article class="lux-card glass-card-light">
              <h3 class="lux-card__title">破冰话题</h3>
              <div class="topic-list">
                <div v-for="item in datePrep.iceBreakTopics" :key="item.title" class="topic-item glass-pill-light">
                  <h3 class="text-main">{{ item.title }}</h3>
                  <p>{{ item.opener }}</p>
                </div>
              </div>
            </article>

            <article class="lux-card glass-card-light">
              <h3 class="lux-card__title">约会小惊喜</h3>
              <p>{{ datePrep.surpriseIdea }}</p>
            </article>

            <article class="two-col">
              <div class="lux-card glass-card-light">
                <h3 class="lux-card__title">打扮建议</h3>
                <p>{{ datePrep.outfitAdvice }}</p>
              </div>
              <div class="lux-card glass-card-light">
                <h3 class="lux-card__title">心理准备</h3>
                <p>{{ datePrep.mindsetAdvice }}</p>
              </div>
            </article>

            <article v-if="datePrep.nearbyShops?.length" class="lux-card glass-card-light">
              <h3 class="lux-card__title">附近推荐</h3>
              <div class="shop-list">
                <div v-for="shop in datePrep.nearbyShops" :key="shop.name" class="shop-item glass-pill-light">
                  <strong class="text-main">{{ shop.name }}</strong>
                  <span v-if="shop.typeName" class="text-xs text-sub">{{ shop.typeName }}</span>
                  <span v-if="shop.distance" class="text-xs text-accent-blue">{{ shop.distance }}m</span>
                </div>
              </div>
            </article>

            <PairDateNegotiationCore
              v-if="pairNegotiationEmbedVisible"
              embed
              :match-result-id="result.matchResultId!"
              :target-user-id="result.matchedUserId!"
            />
          </div>

          <div class="sheet-actions two-btn-layout">
            <button type="button" class="glass-btn" @click="goScreen(3)">回到上一屏</button>
            <button type="button" class="glow-btn-warm" @click="goChat">去打招呼</button>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 原封不动
// ==========================================
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import {
  confirmMomentChoice, getMomentDatePrep, getMomentResult,
  type MomentDatePrepResponse, type MomentResultResponse,
} from '@/api/momentApi'
import PairDateNegotiationCore from '@/views/moment/components/PairDateNegotiationCore.vue'
import { DEFAULT_AVATAR, getMediaUrl } from '@/utils/shared'

const router = useRouter()
const loading = ref(true)
const confirming = ref(false)
const prepLoading = ref(false)
const result = ref<MomentResultResponse | null>(null)
const datePrep = ref<MomentDatePrepResponse | null>(null)
const activeScreen = ref(1)
const pendingChoice = ref<'YUE' | 'GUANZHU' | null>(null)

const screenLabels = ['心动揭晓', '你们之间', '关于TA', '去见 TA']
const insightTitles = ['是什么让你们靠近', '在一起大概是什么感觉', '有一件事你要提前知道']

const avatarSrc = computed(() => getMediaUrl(result.value?.avatarUrl || null) || DEFAULT_AVATAR)

const showMatchScore = computed(() => {
  const p = result.value?.matchScorePercent
  return p != null && p >= 0
})

const insightCards = computed(() => {
  const cards = result.value?.insightCards || []
  return [cards[0], cards[1], cards[2]].filter((item): item is string => !!item)
})

const choiceLocked = computed(() => {
  if (!result.value) return false
  return !!result.value.myChoice
    || result.value.confirmStatus === 'BOTH_YUE'
    || result.value.confirmStatus === 'ANY_GUANZHU'
    || result.value.confirmStatus === 'TIMEOUT_GUANZHU'
})

const pairNegotiationEmbedVisible = computed(() => {
  const r = result.value
  if (!r?.datePrepUnlocked) return false
  if (!r.matchResultId || !r.matchedUserId) return false
  return true
})

const decisionHint = computed(() => {
  if (!result.value) return '选择你的节奏，缘分自有安排。'
  if (result.value.confirmStatus === 'BOTH_YUE') return '缘分已经给了答案。'
  if (result.value.confirmStatus === 'ANY_GUANZHU' || result.value.confirmStatus === 'TIMEOUT_GUANZHU') return '这次会先从朋友开始。'
  return '选择你的节奏，缘分自有安排。'
})

const decisionDetail = computed(() => {
  if (!result.value) return ''
  if (result.value.confirmStatus === 'BOTH_YUE') return '双方都选择了“约一次”，第四屏已经解锁。'
  if (result.value.confirmStatus === 'TIMEOUT_GUANZHU') return '48 小时内没有等到双方都确认，系统已经为你们自动互相关注。'
  if (result.value.confirmStatus === 'ANY_GUANZHU') return '这次会先从关注开始，系统已经帮你们完成互关。'
  if (result.value.myChoice === 'YUE') return '你已经发出了心动信号，等 TA 给出选择后，就会看到下一步安排。'
  if (result.value.myChoice === 'GUANZHU') return '你已经选择先关注，接下来可以慢慢来。'
  return ''
})

function goProfile() {
  if (result.value?.matchedUserId) {
    router.push(`/profile/${result.value.matchedUserId}`)
  }
}

function goChat() {
  if (result.value?.matchedUserId) {
    router.push(`/chat/${result.value.matchedUserId}`)
  }
}

function goScreen(screen: number) {
  if (screen === 4) {
    if (!result.value?.datePrepUnlocked) return
    openDatePrep()
    return
  }
  activeScreen.value = screen
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadResult() {
  loading.value = true
  try {
    const res = await getMomentResult()
    result.value = res.data.data
  } catch {
    result.value = { matched: false, weekTag: '' }
  } finally {
    loading.value = false
  }
}

async function handleChoice(choice: 'YUE' | 'GUANZHU') {
  if (choiceLocked.value) return
  confirming.value = true
  pendingChoice.value = choice
  try {
    const res = await confirmMomentChoice(choice)
    result.value = res.data.data
    if (choice === 'GUANZHU') {
      ElMessage.success('已为你们自动互相关注')
    } else if (result.value?.datePrepUnlocked) {
      ElMessage.success('双方确认成功，第四屏已解锁')
      await openDatePrep()
      return
    } else {
      ElMessage.success('已发送你的选择')
    }
  } catch (error: unknown) {
    const msg = error && typeof error === 'object' && 'response' in error
      ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
      : null
    ElMessage.error(msg || '提交失败')
  } finally {
    confirming.value = false
    pendingChoice.value = null
  }
}

async function openDatePrep() {
  if (!result.value?.datePrepUnlocked) return
  activeScreen.value = 4
  window.scrollTo({ top: 0, behavior: 'smooth' })
  if (datePrep.value || prepLoading.value) return
  prepLoading.value = true
  try {
    const res = await getMomentDatePrep()
    datePrep.value = res.data.data
  } catch (error: unknown) {
    const msg = error && typeof error === 'object' && 'response' in error
      ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
      : null
    ElMessage.error(msg || '约会准备加载失败')
  } finally {
    prepLoading.value = false
  }
}

onMounted(loadResult)
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 结果页 UI
   ========================================== */
$bg-aurora: #f8fafc;
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$border-light: rgba(255, 255, 255, 0.8);
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
$max-width: 600px;

.result-page {
  min-height: 100vh;
  padding: 24px 16px 64px;
  background: $bg-aurora;
  position: relative;
  
  &::before {
    content: ''; position: fixed; inset: 0; pointer-events: none;
    background: 
      radial-gradient(circle at 10% 10%, rgba(79, 140, 255, 0.1), transparent 45%),
      radial-gradient(circle at 90% 40%, rgba(255, 51, 102, 0.08), transparent 45%),
      radial-gradient(circle at 50% 90%, rgba(255, 123, 84, 0.06), transparent 50%);
    z-index: 0;
  }
}

// 布局壳
.loading-state, .empty-state, .result-shell {
  width: 100%; max-width: $max-width; margin: 0 auto; position: relative; z-index: 1;
}

/* ================= 极光玻璃态核心类 ================= */
.glass-panel {
  background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light; box-shadow: 0 10px 40px rgba(31, 38, 135, 0.05); border-radius: 28px;
}
.glass-card-light {
  background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(12px); 
  border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}
.glass-pill {
  background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 999px;
}
.glass-pill-light {
  background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 16px;
}
.text-gradient-warm {
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800;
}
.text-accent-pink { color: $accent-pink; }
.text-accent-blue { color: $accent-blue; }
.text-main { color: $text-main; }
.text-sub { color: $text-sub; }
.font-bold { font-weight: 700; }
.text-sm { font-size: 14px; }
.text-xs { font-size: 12px; }
.text-4xl { font-size: 36px; }
.m-0 { margin: 0; }
.mt-4 { margin-top: 16px; }
.mt-6 { margin-top: 24px; }
.mb-2 { margin-bottom: 8px; }
.p-3 { padding: 12px; }
.px-3 { padding-left: 12px; padding-right: 12px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.ml-1 { margin-left: 4px; }
.border-accent-blue { border: 1px solid rgba(79, 140, 255, 0.3); }

// ================= 按钮体系 =================
.glow-btn-warm {
  height: 52px; border-radius: 999px; border: none; display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white;
  font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 1px;
  box-shadow: 0 8px 25px rgba(255, 51, 102, 0.3); transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  &:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 12px 30px rgba(255, 51, 102, 0.4); }
  &:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.8; color: #fff; }
}
.glass-btn {
  height: 52px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  color: $text-sub; font-size: 15px; font-weight: 700; cursor: pointer; transition: all 0.3s;
  display: inline-flex; align-items: center; justify-content: center;
  &:hover:not(:disabled) { background: #fff; color: $text-main; transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.05); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.w-full { width: 100%; }

// ================= 动画与特效 =================
.pulse-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: $accent-pink; animation: pulse 2s infinite; margin-right: 4px;}
.float-anim { animation: float 3s ease-in-out infinite; }
.heartbeat-anim { animation: heartbeat 1.5s ease-in-out infinite; }
.panel-entrance { animation: rise-in 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
.content-sheet { animation-delay: 0.1s; }

// ================= 状态页 =================
.loading-state, .empty-state { padding-top: 120px; text-align: center; }
.loading-orbit { position: relative; width: 120px; height: 120px; margin: 0 auto 24px; }
.loading-orbit__ring { position: absolute; inset: 0; border-radius: 50%; border: 2px solid transparent; }
.ring-a { border-top-color: $accent-pink; border-right-color: $accent-pink; animation: spin 2s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite; }
.ring-b { inset: 15px; border-bottom-color: $accent-blue; border-left-color: $accent-blue; animation: spin 3s cubic-bezier(0.68, -0.55, 0.265, 1.55) infinite reverse; }
.loading-orbit__heart { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: $accent-pink; font-size: 40px; }

.loading-title, .empty-state h2 { color: $text-main; font-family: $serif; font-size: 32px; font-weight: 700; margin-bottom: 12px; }
.loading-desc, .empty-state p { color: $text-sub; font-size: 15px; line-height: 1.6; }
.empty-state__icon { margin-bottom: 20px; font-size: 48px; }

// ================= Hero 区 =================
.result-hero { padding: 24px 28px; display: flex; flex-direction: column; align-items: center;}
.hero-badges { display: flex; gap: 12px; margin-bottom: 20px; }
.hero-badge, .hero-week, .sheet-badge { padding: 6px 14px; font-size: 11px; font-weight: 800; color: $accent-pink; letter-spacing: 1px; }

.hero-content { text-align: center; display: flex; flex-direction: column; gap: 12px; }
.hero-kicker { color: $accent-blue; font-size: 12px; font-weight: 800; letter-spacing: 0.15em; }
.hero-title { font-size: 36px; line-height: 1.2; font-family: $serif; margin: 0; }
.hero-desc { color: $text-sub; font-size: 14px; max-width: 400px; margin: 0; line-height: 1.6;}

// 胶囊导航
.tuner-capsule { display: flex; width: 100%; padding: 6px; gap: 4px; }
.tuner-btn {
  flex: 1; padding: 10px 0; border-radius: 999px; text-align: center; font-size: 13px; font-weight: 700;
  color: $text-sub; transition: all 0.3s; border: none; background: transparent; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  &.active {
    color: $accent-pink; background: linear-gradient(135deg, rgba(79, 140, 255, 0.08), rgba(255, 51, 102, 0.08));
    box-shadow: 0 2px 8px rgba(255, 51, 102, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  }
  &.locked { color: #cbd5e1; }
}

// ================= 内容面板区 =================
.content-sheet { padding: 32px 24px 24px; margin-top: 16px; }
.sheet-header { text-align: center; margin-bottom: 28px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.sheet-header h2 { margin: 0; color: $text-main; font-size: 28px; font-family: $serif; font-weight: 700; }
.sheet-header p { margin: 0; color: $text-sub; font-size: 14px; max-width: 360px; line-height: 1.6;}

.content-stack { display: flex; flex-direction: column; gap: 16px; }

// --- 第一屏：心动卡片 ---
.match-card {
  padding: 24px; border-radius: 24px; text-align: left; cursor: pointer;
  position: relative; overflow: hidden; border: none; transition: all 0.3s;
}
.match-card--hero {
  border: 1px solid rgba(255, 255, 255, 0.9);
  background: radial-gradient(circle at top left, rgba(255, 255, 255, 0.95), rgba(255, 245, 248, 0.8));
  box-shadow: 0 15px 35px rgba(255, 51, 102, 0.08);
  &:hover { transform: translateY(-4px); box-shadow: 0 20px 40px rgba(255, 51, 102, 0.12); border-color: rgba(255, 51, 102, 0.2);}
}

.match-card__glow { position: absolute; top: -50px; right: -50px; width: 150px; height: 150px; border-radius: 50%; background: radial-gradient(circle, rgba(255,51,102,0.15) 0%, transparent 70%); }
.match-card__spark { position: absolute; width: 6px; height: 6px; border-radius: 50%; background: #fff; box-shadow: 0 0 10px #fff; }
.spark-a { top: 20px; right: 30px; animation: pulse 2s infinite; }
.spark-b { top: 50px; right: 15px; width: 4px; height: 4px; animation: pulse 3s infinite reverse; }

.match-card__main { display: flex; align-items: center; gap: 16px; position: relative; z-index: 1; }
.avatar-glow-wrap {
  position: relative; border-radius: 50%; padding: 3px; background: var(--glow-color); 
  box-shadow: 0 4px 15px var(--glow-color); flex-shrink: 0;
  .match-avatar { width: 72px; height: 72px; border-radius: 50%; border: 2px solid #fff; object-fit: cover;}
}
.match-info { flex: 1; }
.match-card__label { margin: 0 0 4px; color: $accent-pink; font-size: 12px; font-weight: 800; letter-spacing: 1px; }
.match-title-row h3 { margin: 0; color: $text-main; font-size: 24px; font-weight: 800; font-family: $serif; }
.match-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; color: $text-sub; font-size: 13px; font-weight: 600; }
.match-card__arrow { color: $accent-pink; font-size: 28px; font-weight: 300; opacity: 0.5; transition: transform 0.3s; }
.match-card--hero:hover .match-card__arrow { transform: translateX(5px); opacity: 1; }

.match-score-block { margin-top: 20px; padding: 12px 16px; text-align: left; }
.match-score-block__row { display: flex; align-items: baseline; gap: 8px; }
.match-score-block__label { font-size: 14px; font-weight: 700; color: $text-main; }
.match-score-block__value { font-size: 28px; }

.match-bio { margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(0,0,0,0.05); color: $text-sub; font-size: 14px; line-height: 1.6; }
.match-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.info-tag { padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; background: rgba(255,51,102,0.1); color: $accent-pink; }
.info-tag--soft { background: #fff; color: $text-sub; border: 1px solid rgba(0,0,0,0.05); }
.match-card__hint { margin-top: 16px; color: $accent-blue; font-size: 12px; font-weight: 700; }

// --- 第二至四屏卡片 ---
.lux-card, .quote-card, .decision-card, .locked-card { padding: 24px; text-align: center; }
.lux-card { text-align: left; }
.lux-card__title { margin: 0 0 16px; color: $text-main; font-size: 20px; font-weight: 800; font-family: $serif;}
.lux-card p, .lux-card__paragraph { margin: 0; color: $text-sub; font-size: 14px; line-height: 1.8; }
.tag-row { display: flex; flex-wrap: wrap; gap: 8px; }

.insight-card { display: flex; gap: 16px; padding: 20px; align-items: flex-start; }
.insight-card__index { width: 36px; height: 36px; border-radius: 12px; background: rgba(255,51,102,0.1); color: $accent-pink; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 14px; flex-shrink: 0; }
.insight-card__body h3 { margin: 0 0 6px; color: $text-main; font-size: 18px; font-weight: 700; font-family: $serif;}
.insight-card__body p { margin: 0; color: $text-sub; font-size: 14px; line-height: 1.6; }

.quote-card__title { margin: 0 0 12px; font-size: 13px; color: $accent-pink; font-weight: 800; letter-spacing: 1px; }
.quote-card__text { margin: 0; font-size: 24px; font-family: $serif; line-height: 1.5; }

.decision-card__title { margin: 0 0 8px; font-size: 24px; font-family: $serif;}
.decision-card__detail { margin: 0; color: $text-sub; font-size: 14px; line-height: 1.6; }
.decision-card__buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 24px; }
.decision-card__links { display: flex; justify-content: center; gap: 24px; margin-top: 20px; }
.text-action { background: none; border: none; color: $accent-blue; font-size: 14px; font-weight: 700; cursor: pointer; transition: color 0.2s; &:hover { color: $text-main; } }

.locked-card__title { margin: 0 0 12px; font-size: 24px; font-family: $serif; }
.prep-loading { padding: 40px 20px; display: flex; flex-direction: column; align-items: center;}
.pulse-ring { width: 48px; height: 48px; border-radius: 50%; border: 3px solid rgba(255,51,102,0.2); border-top-color: $accent-pink; animation: spin 1s linear infinite; margin-bottom: 16px;}

.topic-list, .shop-list { display: flex; flex-direction: column; gap: 12px; }
.topic-item { padding: 16px; border-radius: 16px; text-align: left; }
.topic-item h3 { margin: 0 0 6px; font-size: 16px; font-weight: 700; }
.topic-item p { margin: 0; font-size: 14px; color: $text-sub; line-height: 1.6;}
.shop-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

// --- 底部动作区 ---
.sheet-actions { margin-top: 32px; padding-top: 24px; border-top: 1px solid rgba(0,0,0,0.05); }
.two-btn-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

// --- Keyframes ---
@keyframes spin { 100% { transform: rotate(360deg); } }
@keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.1); opacity: 0.7; } }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
@keyframes heartbeat { 0%, 30%, 60%, 100% { transform: scale(1); } 15%, 45% { transform: scale(1.15); } }
@keyframes rise-in { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

// --- 移动端适配 ---
@media (max-width: 640px) {
  .result-page { padding: 16px 12px 48px; }
  .result-hero { padding: 24px 16px; border-radius: 20px; }
  .content-sheet { padding: 24px 16px 20px; border-radius: 20px; }
  .hero-title { font-size: 30px; }
  .match-avatar { width: 64px; height: 64px; }
  .match-title-row h3 { font-size: 20px; }
  .match-score-block__value { font-size: 24px; }
  .decision-card__buttons, .two-btn-layout, .two-col { grid-template-columns: 1fr; }
  .tuner-btn { padding: 8px 0; font-size: 12px; }
}
</style>