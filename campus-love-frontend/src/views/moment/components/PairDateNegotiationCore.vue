<template>
  <div class="pd-core" :class="{ 'pd-core--embed': embed }">
    <template v-if="embed && !effectiveNegotiationId">
      <div v-if="loading" class="pd-loading pulse-anim">感知引力场状态中...</div>
      <div v-else class="pd-card pd-intro glass-card-light">
        <h2 class="pd-intro__title text-gradient-warm">约会三步协商</h2>
        <p class="pd-muted">
          第一步：划掉最不想去的选项 · 第二步：勾选空闲时段 · 第三步：决定谁定地点。<br/>
          双方完成后，系统将自动为你们对齐最优解。
        </p>
        <p v-if="waitingPartnerYue" class="pd-wait-yue glass-pill-light">
          <span class="pulse-dot"></span> 已记下你的心意，等对方也进入本页第四屏后，即可开启协商。
        </p>
        <button
          v-else
          type="button"
          class="pd-primary glow-btn-warm w-full mt-4"
          :disabled="yueBusy"
          @click="onStartYue"
        >
          {{ yueBusy ? '构建协商通道中...' : '开启三步协商' }}
        </button>
      </div>
    </template>

    <div v-else-if="!embed && !effectiveNegotiationId && !loading" class="pd-empty">星轨记录不存在或链接已失效</div>

    <div v-else-if="loading && !detail" class="pd-loading pulse-anim">同步协商进度中...</div>

    <div v-else-if="!embed && !detail" class="pd-empty">记录不存在或已失效</div>

    <template v-else-if="detail">
      <section v-if="isTerminal" class="pd-terminal">
        <div class="terminal-header">
          <span class="pd-terminal__badge glass-pill">
            {{ detail.timeMismatch ? '时段未重合' : '协商已达成' }}
          </span>
        </div>
        
        <p v-if="detail.timeMismatch" class="pd-muted pd-terminal__lead">
          这周空闲没有完全对上，约会方式与地点决定者如下；具体时间请私下商量哦。
        </p>
        <p v-else class="pd-muted pd-terminal__lead">
          以下为系统根据双方选择生成的专属邀约摘要；请先启封邀请函，再查看详情。
        </p>

        <div class="pd-invite-shell">
          <Transition name="pd-invite-switch" mode="out-in">
            <button
              v-if="!inviteRevealed"
              key="env"
              type="button"
              class="pd-invite-envelope glass-card-light"
              aria-label="启封 Campus Love 专属邀约"
              @click="inviteRevealed = true"
            >
              <div class="pd-invite-envelope__frame" />
              <div class="pd-invite-envelope__wax glow-bg-warm" aria-hidden="true">CL</div>
              <p class="pd-invite-envelope__kicker">Campus Love</p>
              <p class="pd-invite-envelope__title text-gradient-warm">专属心动邀约</p>
              <p class="pd-invite-envelope__hint">轻触启封</p>
            </button>

            <div
              v-else
              key="card"
              class="pd-invite-card glass-panel"
              :class="{ 'pd-invite-card--clickable': !!detail.pairInviteId }"
              role="button"
              :tabindex="detail.pairInviteId ? 0 : -1"
              @click="onInviteCardClick"
              @keyup.enter="onInviteCardClick"
            >
              <div class="pd-invite-card__ribbon">🌸 专属约会卡片</div>

              <div class="pd-invite-row">
                <span class="pd-invite-row__label">发起人</span>
                <div class="pd-invite-row__user">
                  <div class="avatar-glow-wrap" style="--glow-color: rgba(79, 140, 255, 0.3)">
                    <AppAvatar :src="initiatorAvatarSrc" :name="displayInitiatorName" :size="40" class="pd-invite-avatar" />
                  </div>
                  <span class="pd-invite-nick">{{ displayInitiatorName }}</span>
                </div>
              </div>

              <div class="pd-invite-row">
                <span class="pd-invite-row__label">受邀人</span>
                <div class="pd-invite-row__user">
                  <div class="avatar-glow-wrap" style="--glow-color: rgba(255, 51, 102, 0.3)">
                    <AppAvatar :src="guestAvatarSrc" :name="displayGuestName" :size="40" class="pd-invite-avatar" />
                  </div>
                  <span class="pd-invite-nick">{{ displayGuestName }}</span>
                </div>
              </div>

              <div class="pd-invite-divider" />

              <div class="pd-invite-field">
                <span class="pd-invite-field__k">约会方式</span>
                <span class="pd-invite-field__v text-gradient-warm">{{ detail.finalDateOption?.title || '心动专属约会' }}</span>
              </div>
              <p v-if="detail.finalDateOption?.description" class="pd-invite-desc">{{ detail.finalDateOption.description }}</p>

              <div class="pd-invite-field">
                <span class="pd-invite-field__k">约会时间</span>
                <span class="pd-invite-field__v">{{ timeLineText }}</span>
              </div>

              <div class="pd-invite-field">
                <span class="pd-invite-field__k">地点</span>
                <span class="pd-invite-field__v">由 <strong class="text-accent-pink">{{ displayInitiatorName }}</strong> 决定</span>
              </div>
              <p class="pd-location-hint glass-pill-light">💡 请尽量提前一天通知 TA 具体地点哦~</p>

              <p v-if="detail.deciderReasonKey" class="pd-invite-reason glass-pill-light">{{ reasonHint }}</p>

              <div v-if="showCountdown" class="pd-countdown pd-countdown--in-card glass-pill-light">
                <p class="pd-countdown__label">距离约定时段还有</p>
                <p class="pd-countdown__nums" :class="{ urgent: countdownUrgent }">{{ countdownText }}</p>
              </div>

              <div v-if="detail.pairInviteId" class="pd-invite-tap-hint">
                查看一对一邀约详情
                <span class="pd-invite-tap-hint__arrow">→</span>
              </div>
              <div v-else-if="detail.status === 'EXPIRED'" class="pd-invite-tap-hint pd-invite-tap-hint--muted">
                本轮协商已结束，未生成可跳转的一对一邀约
              </div>
              <div v-else class="pd-invite-tap-hint pd-invite-tap-hint--muted">邀约记录同步中，请稍后下拉刷新本页</div>
            </div>
          </Transition>
        </div>
      </section>

      <section v-else-if="waitingPartner" class="pd-card glass-card-light text-center">
        <div class="pulse-ring mx-auto mb-4"></div>
        <h2 class="text-xl font-bold text-main mb-2">你已完成选择</h2>
        <p class="pd-muted mb-2">等待对方完成三步选择... 可以先去聊聊天。</p>
        <p class="pd-poll-hint glass-pill-light inline-block px-3 py-1 mt-2">页面将自动刷新状态</p>
      </section>

      <section v-else class="pd-wizard">
        <div class="pd-steps glass-pill-light">
          <span :class="{ on: wizardStep >= 1, done: !!detail.myExcludedRank }">1 排除方式</span>
          <span :class="{ on: wizardStep >= 2, done: (detail.myTimeSlots?.length || 0) > 0 }">2 空闲时间</span>
          <span :class="{ on: wizardStep >= 3, done: !!detail.myLocationChoice }">3 地点决定</span>
        </div>

        <div v-show="wizardStep === 1" class="pd-card glass-card-light panel-entrance">
          <h2 class="text-xl font-bold text-main mb-2">哪个约会方式你<span class="text-accent-pink">最不想去</span>？</h2>
          <p class="pd-muted mb-4">系统将结合双方选择，排除掉不喜欢的，留下最合适的一种。</p>
          <div class="pd-option-list">
            <button
              v-for="opt in dateOptions"
              :key="opt.rank"
              type="button"
              class="pd-opt glass-pill-light"
              :disabled="submitting"
              @click="submitExclude(opt.rank)"
            >
              <span class="pd-opt__rank text-gradient-warm">0{{ opt.rank }}</span>
              <span class="pd-opt__body">
                <span class="pd-opt__title text-main">{{ opt.title }}</span>
                <span class="pd-opt__desc">{{ opt.description }}</span>
              </span>
            </button>
          </div>
        </div>

        <div v-show="wizardStep === 2" class="pd-card glass-card-light panel-entrance">
          <h2 class="text-xl font-bold text-main mb-2">这周末到下周四，何时有空？</h2>
          <p class="pd-muted mb-4">可多选；至少选一个时段以寻找交集。</p>
          <div class="pd-grid-wrap glass-pill-light p-2">
            <table class="pd-grid">
              <thead>
                <tr>
                  <th />
                  <th v-for="d in dayDefs" :key="d.code">{{ d.label }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in periodDefs" :key="p.code">
                  <th>{{ p.label }}</th>
                  <td v-for="d in dayDefs" :key="d.code + p.code">
                    <button
                      type="button"
                      class="pd-cell"
                      :class="{ on: pickedSlots.has(slotCode(d.code, p.code)) }"
                      @click="toggleSlot(d.code, p.code)"
                    >
                      <span v-if="pickedSlots.has(slotCode(d.code, p.code))" class="check-mark">✓</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <button type="button" class="pd-primary glow-btn-warm w-full mt-4" :disabled="submitting || pickedSlots.size === 0" @click="submitTimes">
            确认时段，下一步
          </button>
        </div>

        <div v-show="wizardStep === 3" class="pd-card glass-card-light panel-entrance">
          <h2 class="text-xl font-bold text-main mb-4">约会地点，由谁来定？</h2>
          <div class="pd-loc-btns">
            <button type="button" class="pd-secondary glass-btn" :disabled="submitting" @click="submitLoc('SELF')">我来决定</button>
            <button type="button" class="pd-secondary glass-btn" :disabled="submitting" @click="submitLoc('PARTNER')">Ta 来决定</button>
            <button type="button" class="pd-secondary glass-btn" :disabled="submitting" @click="submitLoc('EITHER')">都可以</button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppAvatar from '@/components/AppAvatar.vue'
import {
  getPairDate,
  getPairDateByTarget,
  getPairDateTime,
  postPairDateYue,
  submitPairDateStep,
  type PairDateNegotiationVO,
} from '@/api/pairDateApi'
import { useUserStore } from '@/store/userStore'

const props = withDefaults(
  defineProps<{
    embed?: boolean
    matchResultId?: number
    targetUserId?: number
    autoStartYue?: boolean
  }>(),
  { embed: false, autoStartYue: false },
)

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const yueBusy = ref(false)
const detail = ref<PairDateNegotiationVO | null>(null)
const pickedSlots = ref<Set<string>>(new Set())
const wizardStep = ref(1)

const resolvedNegotiationId = ref<number | null>(null)
const waitingPartnerYue = ref(false)

const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const embedPollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const countdownTimer = ref<ReturnType<typeof setInterval> | null>(null)
const countdownText = ref('—')
const countdownUrgent = ref(false)
const clockOffset = ref(0)

const inviteRevealed = ref(false)

const routeNegotiationId = computed(() => Number(route.params.negotiationId))

const effectiveNegotiationId = computed(() => {
  if (props.embed) {
    return resolvedNegotiationId.value && resolvedNegotiationId.value > 0 ? resolvedNegotiationId.value : 0
  }
  const n = routeNegotiationId.value
  return Number.isFinite(n) && n > 0 ? n : 0
})

const dayDefs = [
  { code: 'SAT', label: '周六' },
  { code: 'SUN', label: '周日' },
  { code: 'MON', label: '周一' },
  { code: 'TUE', label: '周二' },
  { code: 'WED', label: '周三' },
  { code: 'THU', label: '周四' },
]
const periodDefs = [
  { code: 'AM', label: '上午' },
  { code: 'PM', label: '下午' },
  { code: 'EVE', label: '晚上' },
]

const dateOptions = computed(() => detail.value?.dateOptions?.options || [])

const isTerminal = computed(() => {
  const s = detail.value?.status
  return s === 'COMPLETED' || s === 'TIME_MISMATCH' || s === 'EXPIRED'
})

const waitingPartner = computed(() => {
  const d = detail.value
  if (!d || isTerminal.value) return false
  const mineDone = !!(d.myExcludedRank && (d.myTimeSlots?.length || 0) > 0 && d.myLocationChoice)
  return mineDone && !d.partnerFinishedAll
})

const showCountdown = computed(
  () => detail.value?.status === 'COMPLETED' && !!detail.value?.meetingTimestamp,
)

const selfId = computed(() => userStore.user?.id ?? null)

const displayInitiatorName = computed(() => {
  const id = detail.value?.locationDeciderId
  if (!id) return '—'
  if (selfId.value && id === selfId.value) return '你'
  return detail.value?.initiatorNickname || 'Ta'
})

const displayGuestName = computed(() => {
  const id = detail.value?.guestUserId
  if (!id) return '—'
  if (selfId.value && id === selfId.value) return '你'
  return detail.value?.guestNickname || 'Ta'
})

const initiatorAvatarSrc = computed(() => detail.value?.initiatorAvatarUrl ?? null)
const guestAvatarSrc = computed(() => detail.value?.guestAvatarUrl ?? null)

const timeLineText = computed(() => {
  const d = detail.value
  if (!d) return '—'
  if (d.timeMismatch) return '请私下商议具体时段'
  if (d.meetingTimeSlot) return formatSlotLabel(d.meetingTimeSlot)
  return '待定'
})

function onInviteCardClick() {
  const raw = detail.value?.pairInviteId
  const id = raw != null ? Number(raw) : NaN
  if (!Number.isFinite(id) || id <= 0) {
    ElMessage.warning('一对一邀约尚未同步完成，请稍后下拉刷新或重新进入本页')
    return
  }
  router.push(`/invite/${id}`)
}

const reasonHint = computed(() => {
  const k = detail.value?.deciderReasonKey
  if (!k) return ''
  const ini = displayInitiatorName.value
  const guest = displayGuestName.value
  const map: Record<string, string> = {
    MUTUAL_BOTH_WANT_A: `你们不谋而合，都觉得应该由 ${ini} 来定地点 🎯`,
    MUTUAL_BOTH_WANT_B: `你们不谋而合，都觉得应该由 ${ini} 来定地点 🎯`,
    A_DECIDES_B_FLEX: `${guest} 表示都可以，所以交给 ${ini} 来定啦 ✨`,
    B_DECIDES_A_FLEX: `${guest} 表示都可以，所以交给 ${ini} 来定啦 ✨`,
    A_DECIDES_B_PICKED_A: `一方愿意交出决定权，另一方表示都可以，最终由 ${ini} 拍板 🌟`,
    B_DECIDES_A_PICKED_B: `一方愿意交出决定权，另一方表示都可以，最终由 ${ini} 拍板 🌟`,
    DICE_ROLL: '你们的选择没有达成一致，引力场帮你们掷了个骰子 🎲',
  }
  return map[k] || ''
})

function slotCode(day: string, period: string) { return `${day}_${period}` }

function formatSlotLabel(code: string) {
  const [d, p] = code.split('_')
  const day = dayDefs.find((x) => x.code === d)?.label || d
  const per = periodDefs.find((x) => x.code === p)?.label || p
  return `${day} ${per}`
}

function syncWizardFromDetail() {
  const d = detail.value
  if (!d || isTerminal.value) return
  if (!d.myExcludedRank) { wizardStep.value = 1; return }
  if (!(d.myTimeSlots?.length || 0)) { wizardStep.value = 2; pickedSlots.value = new Set(); return }
  if (!d.myLocationChoice) { wizardStep.value = 3; pickedSlots.value = new Set(d.myTimeSlots || []); return }
  wizardStep.value = 3
  pickedSlots.value = new Set(d.myTimeSlots || [])
}

function toggleSlot(day: string, period: string) {
  const c = slotCode(day, period)
  const next = new Set(pickedSlots.value)
  if (next.has(c)) next.delete(c)
  else next.add(c)
  pickedSlots.value = next
}

async function loadDetailById(id: number) {
  const res = await getPairDate(id)
  detail.value = res.data.data
  syncWizardFromDetail()
}

async function loadDetail() {
  const id = effectiveNegotiationId.value
  if (!id) {
    if (!props.embed) detail.value = null
    loading.value = false
    return
  }
  loading.value = true
  try { await loadDetailById(id) } 
  catch { if (!props.embed) { ElMessage.error('加载失败'); detail.value = null } } 
  finally { loading.value = false }
}

async function embedResolveExisting() {
  if (!props.embed || !props.targetUserId) return
  loading.value = true
  waitingPartnerYue.value = false
  try {
    const res = await getPairDateByTarget(props.targetUserId)
    const d = res.data.data
    if (d?.id) { resolvedNegotiationId.value = d.id; await loadDetailById(d.id) } 
    else { resolvedNegotiationId.value = null; detail.value = null }
  } catch { detail.value = null } 
  finally { loading.value = false }
}

async function onStartYue() {
  if (!props.matchResultId) return
  yueBusy.value = true
  try {
    const res = await postPairDateYue(props.matchResultId)
    const d = res.data.data
    if (d.id) {
      resolvedNegotiationId.value = d.id
      waitingPartnerYue.value = false
      await loadDetailById(d.id)
    } else { waitingPartnerYue.value = true }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally { yueBusy.value = false }
}

async function submitExclude(rank: number) {
  const id = effectiveNegotiationId.value
  if (!id) return
  submitting.value = true
  try {
    const res = await submitPairDateStep(id, { step: 1, excludedRank: rank })
    detail.value = res.data.data
    wizardStep.value = 2
    pickedSlots.value = new Set()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '提交失败') } 
  finally { submitting.value = false }
}

async function submitTimes() {
  const id = effectiveNegotiationId.value
  if (!id || pickedSlots.value.size === 0) return
  submitting.value = true
  try {
    const res = await submitPairDateStep(id, { step: 2, timeSlots: Array.from(pickedSlots.value) })
    detail.value = res.data.data
    wizardStep.value = 3
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '提交失败') } 
  finally { submitting.value = false }
}

async function submitLoc(choice: 'SELF' | 'PARTNER' | 'EITHER') {
  const id = effectiveNegotiationId.value
  if (!id) return
  submitting.value = true
  try {
    const res = await submitPairDateStep(id, { step: 3, locationChoice: choice })
    detail.value = res.data.data
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '提交失败') } 
  finally { submitting.value = false }
}

async function syncServerClock() {
  try {
    const id = effectiveNegotiationId.value
    if (!id) return
    const res = await getPairDateTime(id)
    const st = res.data.data.serverTime
    clockOffset.value = st - Date.now()
  } catch { clockOffset.value = 0 }
}

function tickCountdown() {
  const ts = detail.value?.meetingTimestamp
  if (!ts) { countdownText.value = '—'; return }
  const now = Date.now() + clockOffset.value
  const diff = ts - now
  if (diff <= 0) {
    countdownText.value = '已到约定时段'
    countdownUrgent.value = false
    return
  }
  countdownUrgent.value = diff < 86400000
  const d = Math.floor(diff / 86400000)
  const h = Math.floor((diff % 86400000) / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  countdownText.value = `${d}天 ${h}小时 ${m}分`
}

function startPolling() {
  stopPolling()
  pollTimer.value = setInterval(() => {
    if (waitingPartner.value || detail.value?.status === 'CALCULATING') loadDetail().catch(() => {})
  }, 10000)
}
function stopPolling() { if (pollTimer.value) { clearInterval(pollTimer.value); pollTimer.value = null } }

function startEmbedPolling() {
  stopEmbedPolling()
  if (!props.embed || !props.targetUserId) return
  embedPollTimer.value = setInterval(async () => {
    try {
      if (!resolvedNegotiationId.value || waitingPartnerYue.value) {
        const res = await getPairDateByTarget(props.targetUserId!)
        const d = res.data.data
        if (d?.id) {
          resolvedNegotiationId.value = d.id
          waitingPartnerYue.value = false
          await loadDetailById(d.id)
        }
      } else { await loadDetail().catch(() => {}) }
    } catch { /* ignore */ }
  }, 10000)
}
function stopEmbedPolling() { if (embedPollTimer.value) { clearInterval(embedPollTimer.value); embedPollTimer.value = null } }

function startCountdown() {
  stopCountdown()
  if (!showCountdown.value) return
  syncServerClock().finally(() => {
    tickCountdown()
    countdownTimer.value = setInterval(tickCountdown, 1000)
  })
}
function stopCountdown() { if (countdownTimer.value) { clearInterval(countdownTimer.value); countdownTimer.value = null } }

watch(() => detail.value?.status, () => { if (showCountdown.value) startCountdown(); else stopCountdown() })
watch(waitingPartner, (w) => { if (w) startPolling(); else stopPolling() })
watch(() => detail.value?.id, () => { inviteRevealed.value = false })

onMounted(async () => {
  if (!userStore.user) await userStore.fetchProfile()
  if (props.embed) {
    await embedResolveExisting()
    if (props.autoStartYue && props.matchResultId && !resolvedNegotiationId.value && !waitingPartnerYue.value) {
      await onStartYue()
    }
    if (waitingPartnerYue.value || !effectiveNegotiationId.value) startEmbedPolling()
  } else { await loadDetail() }
  if (waitingPartner.value) startPolling()
  if (showCountdown.value) startCountdown()
})

onUnmounted(() => { stopPolling(); stopEmbedPolling(); stopCountdown() })
</script>

<style scoped lang="scss">
/* ==========================================
   晨曦极光 (Light Glassmorphism) 核心协商组件
   ========================================== */
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$border-light: rgba(255, 255, 255, 0.8);
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;

/* 嵌入模式重写基础样式 */
.pd-core {
  width: 100%;
  &.pd-core--embed {
    /* 嵌入时自身背景全透，融入外部卡片 */
    background: transparent;
    .pd-card { box-shadow: none; background: transparent; border: none; padding: 0;}
  }
}

/* ================= 极光玻璃态组件 ================= */
.glass-panel {
  background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light; box-shadow: 0 10px 40px rgba(31, 38, 135, 0.05); border-radius: 24px;
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

// 文本及按钮
.text-gradient-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800; }
.glow-bg-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); box-shadow: 0 0 12px rgba(255, 51, 102, 0.4); }
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

// 通用工具类
.text-main { color: $text-main; }
.text-sub { color: $text-sub; }
.text-accent-pink { color: $accent-pink; }
.text-accent-blue { color: $accent-blue; }
.font-bold { font-weight: 700; }
.text-xl { font-size: 20px; }
.text-center { text-align: center; }
.w-full { width: 100%; }
.mt-2 { margin-top: 8px; }
.mt-4 { margin-top: 16px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.p-2 { padding: 8px; }
.px-3 { padding-left: 12px; padding-right: 12px; }
.py-1 { padding-top: 4px; padding-bottom: 4px; }
.mx-auto { margin-left: auto; margin-right: auto; }
.inline-block { display: inline-block; }

/* 动画特效 */
.panel-entrance { animation: rise-in 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) both; }
.pulse-anim { animation: opacity-pulse 2s ease-in-out infinite; }
.pulse-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: $accent-blue; animation: pulse-shadow 2s infinite; margin-right: 6px; }
.pulse-ring { width: 48px; height: 48px; border-radius: 50%; border: 3px solid rgba(79,140,255,0.2); border-top-color: $accent-blue; animation: spin 1s linear infinite; }
@keyframes rise-in { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
@keyframes opacity-pulse { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }
@keyframes pulse-shadow { 0% { box-shadow: 0 0 0 0 rgba(79,140,255,0.4); } 70% { box-shadow: 0 0 0 6px rgba(79,140,255,0); } 100% { box-shadow: 0 0 0 0 rgba(79,140,255,0); } }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* ================= 基础结构 ================= */
.pd-card { padding: 24px; margin-bottom: 16px; text-align: center; }
.pd-intro__title { font-family: $serif; font-size: 28px; margin: 0 0 12px; letter-spacing: 1px; }
.pd-muted { color: $text-sub; font-size: 14px; line-height: 1.6; margin: 0; }
.pd-wait-yue { display: inline-flex; align-items: center; padding: 12px 16px; color: $text-main; font-size: 13px; margin-top: 16px; }
.pd-loading, .pd-empty { padding: 40px 0; text-align: center; color: $text-sub; font-weight: 600; }

/* ================= 终端态/邀请函 ================= */
.pd-terminal { text-align: center; }
.terminal-header { margin-bottom: 16px; }
.pd-terminal__badge { display: inline-block; padding: 6px 16px; font-size: 13px; font-weight: 800; color: $accent-pink; }
.pd-terminal__lead { margin-bottom: 24px; max-width: 400px; margin-left: auto; margin-right: auto; }

.pd-invite-shell { min-height: 160px; perspective: 1000px; }
.pd-invite-switch-enter-active, .pd-invite-switch-leave-active { transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1); }
.pd-invite-switch-enter-from { opacity: 0; transform: rotateX(-10deg) translateY(20px); }
.pd-invite-switch-leave-to { opacity: 0; transform: scale(0.95) translateY(-10px); }

/* 水晶信封 */
.pd-invite-envelope {
  position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 100%; min-height: 240px; padding: 32px 20px; border: none; cursor: pointer; text-align: center;
  background: linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(255,245,248,0.8) 100%);
  box-shadow: 0 15px 35px rgba(255, 51, 102, 0.1), inset 0 2px 5px #fff;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  &:hover { transform: translateY(-4px); box-shadow: 0 20px 45px rgba(255, 51, 102, 0.15); }
}
.pd-invite-envelope__frame { position: absolute; inset: 12px; border-radius: 12px; border: 1px dashed rgba(255,51,102,0.3); pointer-events: none; }
.pd-invite-envelope__wax {
  width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 800; color: #fff; margin-bottom: 16px; z-index: 1;
}
.pd-invite-envelope__kicker { margin: 0; font-size: 12px; letter-spacing: 0.3em; text-transform: uppercase; color: $text-sub; z-index: 1; }
.pd-invite-envelope__title { margin: 8px 0 4px; font-size: 26px; font-family: $serif; z-index: 1; }
.pd-invite-envelope__hint { margin: 12px 0 0; font-size: 13px; color: $accent-pink; font-weight: 700; z-index: 1; animation: opacity-pulse 2s infinite; }

/* 展开后的邀约卡片 */
.pd-invite-card {
  padding: 24px 20px; text-align: left; position: relative;
  &--clickable { cursor: pointer; transition: all 0.3s; &:hover { transform: translateY(-3px); box-shadow: 0 15px 40px rgba(255, 51, 102, 0.15); border-color: rgba(255, 51, 102, 0.4); } }
}
.pd-invite-card__ribbon { text-align: center; font-weight: 800; font-size: 14px; color: $accent-pink; padding-bottom: 16px; margin-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,0.05); }

.pd-invite-row { display: flex; align-items: center; gap: 16px; margin-top: 16px; }
.pd-invite-row__label { flex: 0 0 60px; font-size: 13px; color: $text-sub; font-weight: 600; }
.pd-invite-row__user { display: flex; align-items: center; gap: 12px; }
.avatar-glow-wrap { position: relative; border-radius: 50%; padding: 2px; background: var(--glow-color); box-shadow: 0 0 10px var(--glow-color); }
.pd-invite-avatar { border-radius: 50%; border: 2px solid #fff; }
.pd-invite-nick { font-weight: 700; color: $text-main; font-size: 16px; }

.pd-invite-divider { height: 1px; margin: 20px 0; background: linear-gradient(90deg, transparent, rgba(255,51,102,0.2), transparent); }

.pd-invite-field { display: flex; flex-direction: column; gap: 6px; margin-top: 16px; }
.pd-invite-field__k { font-size: 12px; letter-spacing: 1px; color: $text-sub; font-weight: 700; text-transform: uppercase;}
.pd-invite-field__v { font-size: 18px; font-weight: 800; color: $text-main; }
.pd-invite-desc { margin: 6px 0 0; font-size: 14px; color: $text-sub; line-height: 1.6; }
.pd-location-hint { display: inline-block; margin: 12px 0 0; padding: 6px 12px; font-size: 12px; color: $accent-pink; font-weight: 600; }
.pd-invite-reason { margin: 16px 0 0; font-size: 13px; color: $text-main; padding: 12px 16px; font-weight: 500;}

.pd-countdown { margin-top: 20px; padding: 16px; text-align: center; }
.pd-countdown__label { margin: 0 0 8px; font-size: 12px; color: $text-sub; font-weight: 700; }
.pd-countdown__nums { margin: 0; font-size: 24px; font-weight: 800; color: $accent-pink; font-variant-numeric: tabular-nums; }
.pd-countdown__nums.urgent { color: #f56c6c; animation: opacity-pulse 1s infinite; }

.pd-invite-tap-hint { margin-top: 20px; padding-top: 16px; border-top: 1px dashed rgba(255,51,102,0.3); text-align: center; font-size: 14px; font-weight: 800; color: $accent-pink; display: flex; align-items: center; justify-content: center; gap: 8px; }
.pd-invite-tap-hint__arrow { font-size: 18px; transition: transform 0.3s; }
.pd-invite-card--clickable:hover .pd-invite-tap-hint__arrow { transform: translateX(4px); }
.pd-invite-tap-hint--muted { color: $text-sub; font-weight: 600; }

/* ================= 协商向导 (Wizard) ================= */
.pd-steps { display: flex; padding: 6px; margin: 0 0 24px; border-radius: 999px; }
.pd-steps span {
  flex: 1; text-align: center; padding: 8px 4px; border-radius: 999px; font-size: 13px; font-weight: 600; color: $text-sub; transition: all 0.3s;
  &.on { color: $accent-pink; background: rgba(255,51,102,0.1); }
  &.done { color: $text-main; background: transparent; }
}

/* Step 1: 选项 */
.pd-option-list { display: flex; flex-direction: column; gap: 12px; }
.pd-opt {
  display: flex; gap: 16px; align-items: center; text-align: left; padding: 16px; border: 1px solid rgba(255,255,255,0.6);
  cursor: pointer; transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
  &:hover:not(:disabled) { transform: translateY(-2px); background: rgba(255,255,255,0.8); border-color: rgba(255,51,102,0.3); box-shadow: 0 8px 20px rgba(255,51,102,0.08); }
}
.pd-opt__rank { font-size: 24px; font-weight: 800; font-family: $serif; opacity: 0.8; }
.pd-opt__body { display: flex; flex-direction: column; gap: 4px; }
.pd-opt__title { font-weight: 700; font-size: 16px; }
.pd-opt__desc { font-size: 13px; color: $text-sub; }

/* Step 2: 时段网格 */
.pd-grid-wrap { overflow-x: auto; border-radius: 20px; }
.pd-grid { width: 100%; border-collapse: separate; border-spacing: 4px; font-size: 13px; }
.pd-grid th { padding: 8px 4px; font-weight: 700; color: $text-sub; }
.pd-cell {
  width: 100%; height: 40px; border-radius: 12px; background: rgba(255,255,255,0.6); border: 1px solid #fff;
  cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center;
  &:hover { background: #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }
  &.on { background: linear-gradient(135deg, $accent-pink, $accent-orange); border-color: transparent; box-shadow: 0 4px 15px rgba(255,51,102,0.3); }
  .check-mark { color: #fff; font-weight: 800; font-size: 16px; }
}

/* Step 3: 地点决定 */
.pd-loc-btns { display: flex; flex-direction: column; gap: 12px; margin-top: 24px; }
</style>