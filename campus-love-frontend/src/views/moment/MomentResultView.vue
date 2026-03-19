<template>
  <div class="result-page">
    <div v-if="loading" class="loading-state">
      <div class="loading-orbit">
        <span class="loading-orbit__ring ring-a" />
        <span class="loading-orbit__ring ring-b" />
        <span class="loading-orbit__heart">♡</span>
      </div>
      <p class="loading-title">正在揭晓你的心动解密</p>
      <p class="loading-desc">请稍等片刻，缘分正在抵达。</p>
    </div>

    <div v-else-if="!result?.matched" class="empty-state">
      <div class="empty-state__icon">✦</div>
      <h2>本周暂未找到最佳匹配</h2>
      <p>你的心动档案已保留，下周继续帮你寻找。</p>
      <button class="btn-back-home" @click="$router.replace('/moment')">返回心动时刻</button>
    </div>

    <div v-else class="result-shell">
      <section class="result-hero panel-entrance">
        <div class="hero-badges">
          <span class="hero-badge">心动解密</span>
          <span class="hero-week">本周结果</span>
        </div>

        <div class="hero-content">
          <p class="hero-kicker">缘分揭晓</p>
          <h1 class="hero-title">{{ result.yuanfenTitle || '刚好对频' }}</h1>
          <p class="hero-desc">不需要数字证明什么。先看看是什么把你们轻轻推到了同一个地方。</p>
        </div>

        <nav class="screen-tabs" role="tablist">
          <button
            v-for="(label, index) in screenLabels"
            :key="label"
            type="button"
            role="tab"
            class="screen-tab"
            :class="{ active: activeScreen === index + 1, locked: index === 3 && !result.datePrepUnlocked }"
            :disabled="index === 3 && !result.datePrepUnlocked"
            :aria-selected="activeScreen === index + 1"
            @click="goScreen(index + 1)"
          >
            <span class="screen-tab__index">0{{ index + 1 }}</span>
            <span class="screen-tab__label">{{ label }}</span>
          </button>
        </nav>
      </section>

      <section class="content-sheet panel-entrance">
        <template v-if="activeScreen === 1">
          <header class="sheet-header">
            <span class="sheet-badge">第一屏</span>
            <h2>心动揭晓</h2>
            <p>先认识这个让你产生好感的人，再慢慢打开后面的故事。</p>
          </header>

          <div class="content-stack">
            <button type="button" class="match-card match-card--hero" @click="goProfile">
              <span class="match-card__glow" />
              <span class="match-card__spark spark-a" />
              <span class="match-card__spark spark-b" />
              <div class="match-card__main">
                <img :src="avatarSrc" class="match-avatar" alt="" />
                <div class="match-info">
                  <p class="match-card__label">心动对象</p>
                  <h3>{{ result.nickname || '这位同学' }}</h3>
                  <div class="match-meta">
                    <span v-if="result.age">{{ result.age }}岁</span>
                    <span v-if="result.school">{{ result.school }}</span>
                    <span v-if="result.major">{{ result.major }}</span>
                    <span v-if="result.grade">{{ result.grade }}</span>
                  </div>
                </div>
                <span class="match-card__arrow">›</span>
              </div>
              <div v-if="result.bio" class="match-bio">{{ result.bio }}</div>
              <div v-if="(result.mbti || result.zodiac || (result.complementaryModes?.length))" class="match-tags">
                <span v-if="result.mbti" class="info-tag">{{ result.mbti }}</span>
                <span v-if="result.zodiac" class="info-tag">{{ result.zodiac }}</span>
                <span v-for="mode in result.complementaryModes || []" :key="mode" class="info-tag info-tag--soft">
                  {{ mode }}
                </span>
              </div>
              <p class="match-card__hint">点击卡片查看 TA 的主页</p>
            </button>

            <article v-if="result.complementaryModes?.length" class="lux-card">
              <h3 class="lux-card__title">你们的默契</h3>
              <div class="tag-row">
                <span v-for="mode in result.complementaryModes" :key="mode" class="pill-tag">{{ mode }}</span>
              </div>
            </article>
          </div>

          <div class="sheet-actions">
            <button type="button" class="btn-primary" @click="goScreen(2)">查看你们的缘分</button>
          </div>
        </template>

        <template v-else-if="activeScreen === 2">
          <header class="sheet-header">
            <span class="sheet-badge">第二屏</span>
            <h2>你们之间</h2>
            <p>不是一瞬间的巧合，而是很多细小偏好在同一个方向上靠拢。</p>
          </header>

          <div class="content-stack">
            <article v-for="(text, index) in insightCards" :key="index" class="insight-card">
              <div class="insight-card__index">0{{ index + 1 }}</div>
              <div class="insight-card__body">
                <h3>{{ insightTitles[index] }}</h3>
                <p>{{ text }}</p>
              </div>
            </article>

            <article v-if="result.dimensionLabels?.length" class="lux-card">
              <h3 class="lux-card__title">关键词</h3>
              <div class="tag-row">
                <span v-for="item in result.dimensionLabels" :key="item" class="pill-tag pill-tag--light">{{ item }}</span>
              </div>
            </article>

            <article v-if="result.goldenSentence" class="quote-card">
              <h3 class="quote-card__title">专属金句</h3>
              <p class="quote-card__text">{{ result.goldenSentence }}</p>
            </article>
          </div>

          <div class="sheet-actions">
            <button type="button" class="btn-secondary" @click="goScreen(1)">回到揭晓</button>
            <button type="button" class="btn-primary" @click="goScreen(3)">继续了解 TA</button>
          </div>
        </template>

        <template v-else-if="activeScreen === 3">
          <header class="sheet-header">
            <span class="sheet-badge">第三屏</span>
            <h2>关于 TA</h2>
            <p>现在你可以决定，是立刻往前一步，还是留一点时间慢慢靠近。</p>
          </header>

          <div class="content-stack">
            <article class="lux-card">
              <h3 class="lux-card__title">对 TA 的印象</h3>
              <p class="lux-card__paragraph">{{ result.aboutMatchedUser || 'TA身上有一种适合慢慢了解的气质。' }}</p>
            </article>

            <article class="decision-card">
              <p class="decision-card__title">{{ decisionHint }}</p>
              <p v-if="decisionDetail" class="decision-card__detail">{{ decisionDetail }}</p>

              <div class="decision-card__buttons">
                <button class="btn-primary" :disabled="choiceLocked || confirming" @click="handleChoice('YUE')">
                  {{ confirming && pendingChoice === 'YUE' ? '提交中...' : '心动，约起来' }}
                </button>
                <button class="btn-secondary" :disabled="choiceLocked || confirming" @click="handleChoice('GUANZHU')">
                  {{ confirming && pendingChoice === 'GUANZHU' ? '提交中...' : '先关注，慢慢来' }}
                </button>
              </div>

              <div class="decision-card__links">
                <button class="text-action" @click="goProfile">查看 TA 的主页</button>
                <button class="text-action" @click="goChat">去打招呼</button>
              </div>
            </article>
          </div>

          <div class="sheet-actions">
            <button type="button" class="btn-secondary" @click="goScreen(2)">回看缘分</button>
            <button type="button" class="btn-primary" :disabled="!result.datePrepUnlocked" @click="openDatePrep">
              {{ result.datePrepUnlocked ? '查看约会准备' : '等待双方确认' }}
            </button>
          </div>
        </template>

        <template v-else>
          <header class="sheet-header">
            <span class="sheet-badge">第四屏</span>
            <h2>去见 TA</h2>
            <p>把这份心动往现实里推一步，让第一次见面更轻松一点。</p>
          </header>

          <div v-if="!result.datePrepUnlocked" class="locked-card">
            <p class="locked-card__title">第四屏尚未解锁</p>
            <p>双方都选择“心动，约起来”后，这里会出现你们的约会准备内容。</p>
          </div>

          <div v-else-if="prepLoading" class="prep-loading">
            <div class="prep-loading__spinner" />
            <p>正在为你准备第一次见面的细节...</p>
          </div>

          <div v-else-if="datePrep" class="content-stack">
            <article class="lux-card">
              <h3 class="lux-card__title">约会方式推荐</h3>
              <h3>{{ datePrep.dateSceneType }}</h3>
              <p>{{ datePrep.dateSuggestion }}</p>
            </article>

            <article class="lux-card">
              <h3 class="lux-card__title">破冰话题</h3>
              <div class="topic-list">
                <div v-for="item in datePrep.iceBreakTopics" :key="item.title" class="topic-item">
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.opener }}</p>
                </div>
              </div>
            </article>

            <article class="lux-card">
              <h3 class="lux-card__title">约会小惊喜</h3>
              <p>{{ datePrep.surpriseIdea }}</p>
            </article>

            <article class="two-col">
              <div class="lux-card">
                <h3 class="lux-card__title">打扮建议</h3>
                <p>{{ datePrep.outfitAdvice }}</p>
              </div>
              <div class="lux-card">
                <h3 class="lux-card__title">心理准备</h3>
                <p>{{ datePrep.mindsetAdvice }}</p>
              </div>
            </article>

            <article v-if="datePrep.nearbyShops?.length" class="lux-card">
              <h3 class="lux-card__title">附近推荐</h3>
              <div class="shop-list">
                <div v-for="shop in datePrep.nearbyShops" :key="shop.name" class="shop-item">
                  <strong>{{ shop.name }}</strong>
                  <span v-if="shop.typeName">{{ shop.typeName }}</span>
                  <span v-if="shop.distance">{{ shop.distance }}m</span>
                </div>
              </div>
            </article>
          </div>

          <div class="sheet-actions">
            <button type="button" class="btn-secondary" @click="goScreen(3)">回到上一屏</button>
            <button type="button" class="btn-primary" @click="goChat">去打招呼</button>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  confirmMomentChoice,
  getMomentDatePrep,
  getMomentResult,
  type MomentDatePrepResponse,
  type MomentResultResponse,
} from '@/api/momentApi'
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
@use '@/styles/variables' as *;

$pink: #d77fa2;
$pink-soft: #fff3f8;
$pink-border: rgba(215, 127, 162, 0.18);
$pink-strong: rgba(215, 127, 162, 0.34);
$text-main: #4f3941;
$text-soft: #8f7480;
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
$max-width: 560px;

.result-page {
  min-height: 100vh;
  padding: 24px 16px 64px;
  background:
    radial-gradient(circle at top left, rgba(248, 206, 222, 0.2), transparent 28%),
    linear-gradient(180deg, #fffafc 0%, #fff5f8 44%, #ffffff 100%);
}

.loading-state,
.empty-state,
.result-shell {
  width: 100%;
  max-width: $max-width;
  margin: 0 auto;
}

.loading-state,
.empty-state {
  padding-top: 120px;
  text-align: center;
}

.loading-orbit {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 22px;
}

.loading-orbit__ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(215, 127, 162, 0.22);
}

.ring-a { animation: spin 6s linear infinite; }
.ring-b { inset: 18px; animation: spin 4s linear infinite reverse; }

.loading-orbit__heart {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c96f93;
  font-size: 34px;
  animation: pulse 1.6s ease-in-out infinite;
}

.loading-title,
.empty-state h2 {
  color: $text-main;
  font-family: $serif;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 12px;
}

.loading-desc,
.empty-state p {
  color: $text-soft;
  font-size: 14px;
  line-height: 1.9;
}

.empty-state__icon {
  margin-bottom: 18px;
  color: #c96f93;
  font-size: 40px;
}

.btn-back-home,
.btn-primary,
.btn-secondary {
  min-height: 48px;
  padding: 0 24px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  transition: transform $transition-base, box-shadow $transition-base, opacity $transition-base;
  cursor: pointer;
}

.btn-back-home,
.btn-secondary {
  background: rgba(255, 255, 255, 0.96);
  color: $text-soft;
  border: 1px solid $pink-border;
}

.btn-primary {
  border: none;
  color: #ffffff;
  background: linear-gradient(135deg, #efabc4 0%, #d77fa2 100%);
  box-shadow: 0 14px 28px rgba(215, 127, 162, 0.24);
}

.btn-back-home:hover,
.btn-primary:hover:not(:disabled),
.btn-secondary:hover:not(:disabled),
.screen-tab:hover:not(:disabled) {
  transform: translateY(-2px);
}

.btn-primary:disabled,
.btn-secondary:disabled,
.screen-tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.result-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-hero,
.content-sheet {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 248, 251, 0.95));
  border: 1px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 56px rgba(227, 191, 205, 0.14);
}

.result-hero {
  padding: 24px 20px 20px;
  border-radius: 24px;
}

.hero-badges {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.hero-badge,
.hero-week,
.sheet-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 246, 250, 0.96);
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.match-card__label,
.lux-card__title,
.quote-card__title {
  color: #b76587;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-align: center;
}

.hero-kicker {
  color: #b76587;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-title,
.sheet-header h2,
.match-info h2,
.lux-card h3,
.insight-card__body h3,
.topic-item h3,
.decision-card__title {
  font-family: $serif;
}

.hero-title {
  margin: 0;
  color: $text-main;
  font-size: 32px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.hero-desc,
.sheet-header p,
.match-bio,
.lux-card p,
.insight-card__body p,
.topic-item p,
.decision-card__detail,
.locked-card p,
.prep-loading p {
  color: $text-soft;
  font-size: 14px;
  line-height: 1.8;
}

.hero-desc {
  margin: 0;
  text-align: center;
}

.match-card,
.lux-card,
.quote-card,
.decision-card,
.locked-card,
.topic-item {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 246, 250, 0.94));
  border: 1px solid rgba(215, 127, 162, 0.12);
  box-shadow: 0 14px 30px rgba(227, 191, 205, 0.1);
}

.match-card {
  padding: 20px;
  border-radius: 20px;
  text-align: left;
  position: relative;
  overflow: hidden;
}

.match-card--hero {
  width: 100%;
  border: 1px solid rgba(215, 127, 162, 0.18);
  background:
    radial-gradient(circle at top left, rgba(255, 247, 250, 0.95), transparent 42%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.99), rgba(255, 241, 246, 0.96) 48%, rgba(249, 225, 233, 0.92));
  box-shadow:
    0 24px 48px rgba(215, 127, 162, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: transform $transition-base, box-shadow $transition-base, border-color $transition-base;
}

.match-card--hero:hover {
  transform: translateY(-3px);
  border-color: rgba(215, 127, 162, 0.3);
  box-shadow:
    0 28px 56px rgba(215, 127, 162, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.match-card__glow,
.match-card__spark {
  position: absolute;
  pointer-events: none;
}

.match-card__glow {
  inset: auto -20% -55% auto;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 215, 228, 0.42), transparent 62%);
}

.match-card__spark {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 18px rgba(255, 255, 255, 0.85);
}

.spark-a {
  top: 24px;
  right: 26px;
}

.spark-b {
  top: 52px;
  right: 56px;
  width: 6px;
  height: 6px;
}

.match-card__main {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.match-avatar {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 30px rgba(215, 127, 162, 0.18);
  flex-shrink: 0;
}

.match-card__label {
  margin-bottom: 6px;
}

.match-info h2,
.match-info h3 {
  margin: 0 0 6px;
  color: $text-main;
  font-size: 24px;
  font-weight: 700;
}

.match-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: $text-soft;
  font-size: 13px;
}

.match-bio {
  margin: 14px 0 0;
  padding-top: 14px;
  border-top: 1px solid rgba(215, 127, 162, 0.1);
  position: relative;
  z-index: 1;
}

.match-tags,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.match-tags {
  margin-top: 14px;
  position: relative;
  z-index: 1;
}

.info-tag,
.pill-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 242, 247, 0.96);
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 12px;
  font-weight: 700;
}

.info-tag--soft,
.pill-tag--light {
  color: $text-main;
  background: rgba(255, 248, 251, 0.96);
}

.match-card__arrow {
  margin-left: auto;
  color: #c47896;
  font-size: 34px;
  line-height: 1;
}

.match-card__hint {
  margin-top: 14px;
  color: #b76587;
  font-size: 13px;
  font-weight: 700;
  position: relative;
  z-index: 1;
}

.screen-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 20px;
}

.screen-tab {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(215, 127, 162, 0.12);
  transition: transform $transition-base, border-color $transition-base, box-shadow $transition-base;
}

.screen-tab.active {
  border-color: $pink-strong;
  background: linear-gradient(135deg, rgba(255, 247, 250, 0.98), rgba(255, 236, 242, 0.94));
  box-shadow: 0 10px 24px rgba(227, 191, 205, 0.14);
}

.screen-tab__index {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: $pink-soft;
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.screen-tab__label {
  color: $text-main;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.content-sheet {
  padding: 28px 20px 24px;
  border-radius: 24px;
}

.sheet-header {
  margin-bottom: 24px;
  text-align: center;
}

.sheet-header h2 {
  margin: 12px 0 8px;
  color: $text-main;
  font-size: 28px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.sheet-header p {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
}

.content-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lux-card,
.quote-card,
.decision-card,
.locked-card {
  padding: 20px;
  border-radius: 20px;
}

.lux-card--center {
  text-align: center;
}

.lux-card h3 {
  margin: 12px 0 8px;
  color: $text-main;
  font-size: 22px;
  font-weight: 700;
}

.lux-card__title {
  margin: 0 0 14px;
}

.lux-card__paragraph {
  color: $text-soft;
  font-size: 15px;
  line-height: 1.95;
}

.insight-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 245, 249, 0.94));
  border: 1px solid rgba(215, 127, 162, 0.12);
}

.insight-card__index {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: $pink-soft;
  border: 1px solid $pink-border;
  color: #b76587;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.insight-card__body h3,
.topic-item h3 {
  margin-bottom: 6px;
  color: $text-main;
  font-size: 18px;
  font-weight: 700;
}

.quote-card__title {
  margin: 0 0 16px;
  text-align: center;
}

.quote-card__text {
  color: $text-main;
  font-family: $serif;
  font-size: 22px;
  line-height: 1.7;
  text-align: center;
}

.decision-card {
  text-align: center;
}

.decision-card__title {
  color: $text-main;
  font-size: 22px;
  line-height: 1.5;
  font-weight: 700;
}

.decision-card__detail {
  margin-top: 10px;
}

.decision-card__buttons,
.sheet-actions__buttons,
.two-col {
  display: grid;
  gap: 12px;
}

.decision-card__buttons,
.sheet-actions__buttons {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20px;
}

.decision-card__links {
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 16px;
}

.text-action {
  color: #b76587;
  font-size: 14px;
  font-weight: 700;
}

.locked-card,
.prep-loading {
  text-align: center;
}

.locked-card__title {
  margin-bottom: 10px;
  color: $text-main;
  font-family: $serif;
  font-size: 28px;
  font-weight: 700;
}

.prep-loading {
  padding: 48px 24px;
}

.prep-loading__spinner {
  width: 44px;
  height: 44px;
  margin: 0 auto 14px;
  border-radius: 50%;
  border: 3px solid rgba(215, 127, 162, 0.18);
  border-top-color: #d77fa2;
  animation: spin 1s linear infinite;
}

.topic-list,
.shop-list {
  display: grid;
  gap: 12px;
}

.topic-item {
  padding: 18px;
  border-radius: 20px;
}

.two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.shop-item {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 251, 253, 0.98);
  border: 1px solid rgba(215, 127, 162, 0.1);
  color: $text-soft;
  font-size: 13px;
}

.shop-item strong {
  color: $text-main;
}

.sheet-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(215, 127, 162, 0.12);
}

.sheet-actions .btn-primary,
.sheet-actions .btn-secondary {
  flex: 1;
  min-width: 140px;
}

.sheet-actions__summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sheet-actions__summary span {
  color: $text-soft;
  font-size: 12px;
}

.sheet-actions__summary strong {
  color: $text-main;
  font-size: 15px;
  font-weight: 700;
}

.panel-entrance {
  animation: rise-in 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.content-sheet {
  animation-delay: 0.06s;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.08); opacity: 0.8; }
}

@keyframes rise-in {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 640px) {
  .result-page {
    padding: 16px 12px 48px;
  }

  .result-hero,
  .content-sheet {
    padding-left: 16px;
    padding-right: 16px;
    border-radius: 20px;
  }

  .hero-title {
    font-size: 28px;
  }

  .screen-tabs {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .screen-tab {
    min-height: 48px;
    flex-direction: column;
    gap: 4px;
  }

  .screen-tab__label {
    font-size: 12px;
  }

  .sheet-actions {
    flex-direction: column;
  }

  .sheet-actions .btn-primary,
  .sheet-actions .btn-secondary {
    min-width: 100%;
  }

  .sheet-header h2,
  .decision-card__title,
  .quote-card__text,
  .locked-card__title {
    font-size: 24px;
  }

  .lux-card h3,
  .insight-card__body h3 {
    font-size: 20px;
  }

  .decision-card__buttons,
  .sheet-actions__buttons {
    grid-template-columns: 1fr;
  }

  .two-col {
    grid-template-columns: 1fr;
  }

  .decision-card__links {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
