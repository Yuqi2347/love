<template>
  <div class="pd-core" :class="{ 'pd-core--embed': embed }">
    <!-- 嵌入第四屏：尚无协商 id -->
    <template v-if="embed && !effectiveNegotiationId">
      <div v-if="loading" class="pd-loading">加载协商状态…</div>
      <div v-else class="pd-card pd-intro">
        <h2 class="pd-intro__title">约会三步协商</h2>
        <p class="pd-muted">
          第一步：划掉你最不想去的见面方式 · 第二步：勾选空闲时段 · 第三步：谁来定地点。双方完成后系统自动对齐结果。
        </p>
        <p v-if="waitingPartnerYue" class="pd-wait-yue">
          已记下你的心意，等对方也在<strong>本页第四屏</strong>进入协商后，即可一起选。
        </p>
        <button
          v-else
          type="button"
          class="pd-primary"
          :disabled="yueBusy"
          @click="onStartYue"
        >
          {{ yueBusy ? '处理中…' : '开始三步协商' }}
        </button>
      </div>
    </template>

    <!-- 全页模式：路由 id 无效（加载结束后仍无 id） -->
    <div v-else-if="!embed && !effectiveNegotiationId && !loading" class="pd-empty">记录不存在或链接无效</div>

    <div v-else-if="loading && !detail" class="pd-loading">加载中…</div>

    <div v-else-if="!embed && !detail" class="pd-empty">记录不存在或已失效</div>

    <template v-else-if="detail">
      <section v-if="isTerminal" class="pd-terminal">
        <p class="pd-terminal__badge">{{ detail.timeMismatch ? '时间未完全重合' : '协商完成' }}</p>
        <p v-if="detail.timeMismatch" class="pd-muted pd-terminal__lead">
          这周空闲没有完全对上，约会方式与地点决定者如下；具体时间请私下商量。
        </p>
        <p v-else class="pd-muted pd-terminal__lead">以下为系统根据双方选择生成的一对一邀约摘要；请先启封邀请函，再查看详情或进入完整邀约页。</p>

        <div class="pd-invite-shell">
          <Transition name="pd-invite-switch" mode="out-in">
            <button
              v-if="!inviteRevealed"
              key="env"
              type="button"
              class="pd-invite-envelope"
              aria-label="启封 Campus Love 专属邀约"
              @click="inviteRevealed = true"
            >
              <div class="pd-invite-envelope__frame" />
              <div class="pd-invite-envelope__wax" aria-hidden="true">CL</div>
              <p class="pd-invite-envelope__kicker">Campus Love</p>
              <p class="pd-invite-envelope__title">专属邀约</p>
              <p class="pd-invite-envelope__hint">轻触启封</p>
            </button>
            <div
              v-else
              key="card"
              class="pd-invite-card"
              :class="{ 'pd-invite-card--clickable': !!detail.pairInviteId }"
              role="button"
              :tabindex="detail.pairInviteId ? 0 : -1"
              @click="onInviteCardClick"
              @keyup.enter="onInviteCardClick"
            >
          <div class="pd-invite-card__ribbon">🌸 Campus Love 专属邀约</div>

          <div class="pd-invite-row">
            <span class="pd-invite-row__label">发起人</span>
            <div class="pd-invite-row__user">
              <AppAvatar
                :src="initiatorAvatarSrc"
                :name="displayInitiatorName"
                :size="40"
                class="pd-invite-avatar"
              />
              <span class="pd-invite-nick">{{ displayInitiatorName }}</span>
            </div>
          </div>
          <div class="pd-invite-row">
            <span class="pd-invite-row__label">受邀人</span>
            <div class="pd-invite-row__user">
              <AppAvatar
                :src="guestAvatarSrc"
                :name="displayGuestName"
                :size="40"
                class="pd-invite-avatar"
              />
              <span class="pd-invite-nick">{{ displayGuestName }}</span>
            </div>
          </div>

          <div class="pd-invite-divider" />

          <div class="pd-invite-field">
            <span class="pd-invite-field__k">约会方式</span>
            <span class="pd-invite-field__v">{{ detail.finalDateOption?.title || '心动专属约会' }}</span>
          </div>
          <p v-if="detail.finalDateOption?.description" class="pd-invite-desc">{{ detail.finalDateOption.description }}</p>

          <div class="pd-invite-field">
            <span class="pd-invite-field__k">约会时间</span>
            <span class="pd-invite-field__v">{{ timeLineText }}</span>
          </div>

          <div class="pd-invite-field">
            <span class="pd-invite-field__k">地点</span>
            <span class="pd-invite-field__v">由 {{ displayInitiatorName }} 决定</span>
          </div>
          <p class="pd-location-hint">请尽量提前一天通知 TA 具体地点哦~</p>

          <p v-if="detail.deciderReasonKey" class="pd-invite-reason">{{ reasonHint }}</p>

          <div v-if="showCountdown" class="pd-countdown pd-countdown--in-card">
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

      <section v-else-if="waitingPartner" class="pd-card">
        <h2>你已完成选择</h2>
        <p class="pd-muted">等待对方完成三步选择… 可以先去聊聊天。</p>
        <p class="pd-poll-hint">页面将自动刷新状态</p>
      </section>

      <section v-else class="pd-wizard">
        <div class="pd-steps">
          <span :class="{ on: wizardStep >= 1, done: !!detail.myExcludedRank }">1 方式</span>
          <span :class="{ on: wizardStep >= 2, done: (detail.myTimeSlots?.length || 0) > 0 }">2 时间</span>
          <span :class="{ on: wizardStep >= 3, done: !!detail.myLocationChoice }">3 地点</span>
        </div>

        <div v-show="wizardStep === 1" class="pd-card">
          <h2>哪个你最不想去？</h2>
          <p class="pd-muted">系统将结合双方选择，留下最合适的一种第一次见面方式。</p>
          <div class="pd-option-list">
            <button
              v-for="opt in dateOptions"
              :key="opt.rank"
              type="button"
              class="pd-opt"
              :disabled="submitting"
              @click="submitExclude(opt.rank)"
            >
              <span class="pd-opt__rank">0{{ opt.rank }}</span>
              <span class="pd-opt__body">
                <span class="pd-opt__title">{{ opt.title }}</span>
                <span class="pd-opt__desc">{{ opt.description }}</span>
              </span>
            </button>
          </div>
        </div>

        <div v-show="wizardStep === 2" class="pd-card">
          <h2>这周末到下周四，你什么时候有空？</h2>
          <p class="pd-muted">可多选；至少选一个时段。</p>
          <div class="pd-grid-wrap">
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
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <button type="button" class="pd-primary" :disabled="submitting || pickedSlots.size === 0" @click="submitTimes">
            下一步
          </button>
        </div>

        <div v-show="wizardStep === 3" class="pd-card">
          <h2>约会地点，由谁来定？</h2>
          <div class="pd-loc-btns">
            <button type="button" class="pd-secondary" :disabled="submitting" @click="submitLoc('SELF')">我来决定</button>
            <button type="button" class="pd-secondary" :disabled="submitting" @click="submitLoc('PARTNER')">Ta 来决定</button>
            <button type="button" class="pd-secondary" :disabled="submitting" @click="submitLoc('EITHER')">都可以</button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
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
    /** 为 true 时表示嵌在心动结果第四屏，通过 targetUserId 解析协商 id */
    embed?: boolean
    matchResultId?: number
    targetUserId?: number
    /** 与约会推荐同时展示时：自动发起 yue，无需先点「开始三步协商」 */
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

/** 嵌入模式下，双方点「约一下」后得到的协商 id */
const resolvedNegotiationId = ref<number | null>(null)
const waitingPartnerYue = ref(false)

const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const embedPollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const countdownTimer = ref<ReturnType<typeof setInterval> | null>(null)
const countdownText = ref('—')
const countdownUrgent = ref(false)
const clockOffset = ref(0)

/** 协商完成页：先展示邀请函封套，点击后再展示邀约卡片 */
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

/** 对齐 V1.2.0_INVITATION_MODULE §4.4（发起人=地点决定者，受邀人=另一方） */
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
    A_DECIDES_B_PICKED_A: `一方愿意交给对方决定，另一方表示都可以，由 ${ini} 来定地点 🌟`,
    B_DECIDES_A_PICKED_B: `一方愿意交给对方决定，另一方表示都可以，由 ${ini} 来定地点 🌟`,
    DICE_ROLL: '你们的选择没有达成一致，系统帮你们掷了个骰子 🎲',
  }
  return map[k] || ''
})

function slotCode(day: string, period: string) {
  return `${day}_${period}`
}

function formatSlotLabel(code: string) {
  const [d, p] = code.split('_')
  const day = dayDefs.find((x) => x.code === d)?.label || d
  const per = periodDefs.find((x) => x.code === p)?.label || p
  return `${day} ${per}`
}

function syncWizardFromDetail() {
  const d = detail.value
  if (!d || isTerminal.value) return
  if (!d.myExcludedRank) {
    wizardStep.value = 1
    return
  }
  if (!(d.myTimeSlots?.length || 0)) {
    wizardStep.value = 2
    pickedSlots.value = new Set()
    return
  }
  if (!d.myLocationChoice) {
    wizardStep.value = 3
    pickedSlots.value = new Set(d.myTimeSlots || [])
    return
  }
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
    if (!props.embed) {
      detail.value = null
    }
    loading.value = false
    return
  }
  loading.value = true
  try {
    await loadDetailById(id)
  } catch {
    if (!props.embed) {
      ElMessage.error('加载失败')
      detail.value = null
    }
  } finally {
    loading.value = false
  }
}

async function embedResolveExisting() {
  if (!props.embed || !props.targetUserId) return
  loading.value = true
  waitingPartnerYue.value = false
  try {
    const res = await getPairDateByTarget(props.targetUserId)
    const d = res.data.data
    if (d?.id) {
      resolvedNegotiationId.value = d.id
      await loadDetailById(d.id)
    } else {
      resolvedNegotiationId.value = null
      detail.value = null
    }
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
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
    } else {
      waitingPartnerYue.value = true
    }
  } catch (e: unknown) {
    const msg =
      e && typeof e === 'object' && 'response' in e
        ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
        : null
    ElMessage.error(msg || '提交失败')
  } finally {
    yueBusy.value = false
  }
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
    ElMessage.success('已记录')
  } catch (e: unknown) {
    const msg =
      e && typeof e === 'object' && 'response' in e
        ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
        : null
    ElMessage.error(msg || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function submitTimes() {
  const id = effectiveNegotiationId.value
  if (!id || pickedSlots.value.size === 0) return
  submitting.value = true
  try {
    const res = await submitPairDateStep(id, {
      step: 2,
      timeSlots: Array.from(pickedSlots.value),
    })
    detail.value = res.data.data
    wizardStep.value = 3
    ElMessage.success('已记录')
  } catch (e: unknown) {
    const msg =
      e && typeof e === 'object' && 'response' in e
        ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
        : null
    ElMessage.error(msg || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function submitLoc(choice: 'SELF' | 'PARTNER' | 'EITHER') {
  const id = effectiveNegotiationId.value
  if (!id) return
  submitting.value = true
  try {
    const res = await submitPairDateStep(id, { step: 3, locationChoice: choice })
    detail.value = res.data.data
    ElMessage.success('已提交')
  } catch (e: unknown) {
    const msg =
      e && typeof e === 'object' && 'response' in e
        ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
        : null
    ElMessage.error(msg || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function syncServerClock() {
  try {
    const id = effectiveNegotiationId.value
    if (!id) return
    const res = await getPairDateTime(id)
    const st = res.data.data.serverTime
    clockOffset.value = st - Date.now()
  } catch {
    clockOffset.value = 0
  }
}

function tickCountdown() {
  const ts = detail.value?.meetingTimestamp
  if (!ts) {
    countdownText.value = '—'
    return
  }
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
    if (waitingPartner.value || detail.value?.status === 'CALCULATING') {
      loadDetail().catch(() => {})
    }
  }, 10000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

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
      } else {
        await loadDetail().catch(() => {})
      }
    } catch {
      /* ignore */
    }
  }, 10000)
}

function stopEmbedPolling() {
  if (embedPollTimer.value) {
    clearInterval(embedPollTimer.value)
    embedPollTimer.value = null
  }
}

function startCountdown() {
  stopCountdown()
  if (!showCountdown.value) return
  syncServerClock().finally(() => {
    tickCountdown()
    countdownTimer.value = setInterval(tickCountdown, 1000)
  })
}

function stopCountdown() {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
}

watch(
  () => detail.value?.status,
  () => {
    if (showCountdown.value) startCountdown()
    else stopCountdown()
  },
)

watch(waitingPartner, (w) => {
  if (w) startPolling()
  else stopPolling()
})

watch(
  () => detail.value?.id,
  () => {
    inviteRevealed.value = false
  },
)

onMounted(async () => {
  if (!userStore.user) {
    await userStore.fetchProfile()
  }
  if (props.embed) {
    await embedResolveExisting()
    if (props.autoStartYue && props.matchResultId && !resolvedNegotiationId.value && !waitingPartnerYue.value) {
      await onStartYue()
    }
    if (waitingPartnerYue.value || !effectiveNegotiationId.value) {
      startEmbedPolling()
    }
  } else {
    await loadDetail()
  }
  if (waitingPartner.value) startPolling()
  if (showCountdown.value) startCountdown()
})

onUnmounted(() => {
  stopPolling()
  stopEmbedPolling()
  stopCountdown()
})
</script>

<style scoped lang="scss">
$pink: #d77fa2;
$text: #4f3941;
$muted: #8f7480;

.pd-core--embed {
  margin-bottom: 20px;
}

.pd-intro__title {
  margin-top: 0;
}

.pd-wait-yue {
  color: $text;
  line-height: 1.5;
}

.pd-loading,
.pd-empty {
  padding: 24px 0;
  text-align: center;
  color: $muted;
}

.pd-card {
  margin-top: 16px;
  padding: 18px 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 28px rgba(215, 127, 162, 0.12);
}

.pd-core--embed .pd-card:first-child {
  margin-top: 0;
}

.pd-muted {
  color: $muted;
  font-size: 0.9rem;
}

.pd-strong {
  font-weight: 600;
  margin: 0.25rem 0;
}

.pd-block h3 {
  font-size: 0.85rem;
  color: $muted;
  margin: 12px 0 4px;
}

.pd-terminal__badge {
  font-size: 0.75rem;
  font-weight: 600;
  color: $pink;
  letter-spacing: 0.04em;
  margin: 0 0 8px;
}

.pd-terminal__lead {
  margin: 0 0 16px;
  line-height: 1.55;
}

.pd-invite-shell {
  min-height: 120px;
}

.pd-invite-switch-enter-active,
.pd-invite-switch-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.pd-invite-switch-enter-from {
  opacity: 0;
  transform: scale(0.96) translateY(8px);
}
.pd-invite-switch-leave-to {
  opacity: 0;
  transform: scale(0.98) translateY(-6px);
}

.pd-invite-envelope {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 220px;
  padding: 28px 20px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  text-align: center;
  color: #4a2c34;
  background:
    linear-gradient(165deg, rgba(255, 252, 248, 0.98) 0%, rgba(250, 236, 240, 0.96) 45%, rgba(245, 228, 234, 0.98) 100%);
  box-shadow:
    0 10px 36px rgba(120, 60, 80, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.85),
    inset 0 -2px 0 rgba(180, 120, 140, 0.08);
  border: 1px solid rgba(180, 130, 150, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  font-family: inherit;
}

.pd-invite-envelope:hover {
  transform: translateY(-3px);
  box-shadow:
    0 16px 44px rgba(120, 60, 80, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.pd-invite-envelope:focus-visible {
  outline: 2px solid rgba(215, 127, 162, 0.85);
  outline-offset: 3px;
}

.pd-invite-envelope__frame {
  position: absolute;
  inset: 12px;
  border-radius: 14px;
  border: 1px solid rgba(200, 150, 165, 0.25);
  pointer-events: none;
}

.pd-invite-envelope__wax {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #f5e6dc;
  background: radial-gradient(circle at 30% 28%, #c76b7e 0%, #7d2f3f 55%, #4f1f2c 100%);
  box-shadow:
    0 4px 12px rgba(80, 30, 45, 0.35),
    inset 0 2px 0 rgba(255, 255, 255, 0.2);
  margin-bottom: 14px;
  z-index: 1;
}

.pd-invite-envelope__kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.35em;
  text-transform: uppercase;
  color: #8a5a66;
  z-index: 1;
}

.pd-invite-envelope__title {
  margin: 8px 0 4px;
  font-size: 1.35rem;
  font-weight: 700;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  color: #5c2a38;
  z-index: 1;
}

.pd-invite-envelope__hint {
  margin: 12px 0 0;
  font-size: 0.82rem;
  color: #9a6b78;
  z-index: 1;
}

.pd-location-hint {
  margin: 8px 0 0;
  font-size: 0.8rem;
  color: #a07082;
  line-height: 1.45;
}

.pd-invite-card {
  position: relative;
  padding: 20px 18px 16px;
  border-radius: 20px;
  background: linear-gradient(155deg, #fffefb 0%, #fff8fb 42%, #ffeef5 100%);
  border: 1px solid rgba(215, 127, 162, 0.38);
  box-shadow:
    0 14px 40px rgba(215, 127, 162, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.pd-invite-card--clickable {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.pd-invite-card--clickable:hover {
  transform: translateY(-2px);
  border-color: rgba(215, 127, 162, 0.55);
  box-shadow:
    0 18px 48px rgba(215, 127, 162, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.pd-invite-card--clickable:focus-visible {
  outline: 2px solid rgba(215, 127, 162, 0.85);
  outline-offset: 3px;
}

.pd-invite-card__ribbon {
  text-align: center;
  font-weight: 700;
  font-size: 0.92rem;
  color: #9c3d62;
  padding-bottom: 14px;
  margin-bottom: 4px;
  border-bottom: 2px solid rgba(215, 127, 162, 0.28);
  letter-spacing: 0.06em;
}

.pd-invite-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.pd-invite-row__label {
  flex: 0 0 3.2rem;
  font-size: 0.78rem;
  color: $muted;
  font-weight: 500;
}

.pd-invite-row__user {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pd-invite-avatar {
  flex-shrink: 0;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(215, 127, 162, 0.2);
}

.pd-invite-nick {
  font-weight: 600;
  color: $text;
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pd-invite-divider {
  height: 1px;
  margin: 16px 0 14px;
  background: linear-gradient(90deg, transparent, rgba(215, 127, 162, 0.35), transparent);
}

.pd-invite-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 10px;
}

.pd-invite-field__k {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: $muted;
  font-weight: 600;
}

.pd-invite-field__v {
  font-size: 0.95rem;
  font-weight: 600;
  color: #5c3545;
  line-height: 1.4;
}

.pd-invite-desc {
  margin: 6px 0 0;
  font-size: 0.85rem;
  color: $muted;
  line-height: 1.45;
}

.pd-invite-reason {
  margin: 14px 0 0;
  font-size: 0.85rem;
  color: $muted;
  line-height: 1.5;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(215, 127, 162, 0.15);
}

.pd-countdown--in-card {
  margin-top: 16px;
}

.pd-invite-tap-hint {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(215, 127, 162, 0.25);
  text-align: center;
  font-size: 0.88rem;
  font-weight: 600;
  color: #c44b7a;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.pd-invite-tap-hint__arrow {
  font-size: 1.1rem;
}

.pd-invite-tap-hint--muted {
  font-weight: 500;
  color: $muted;
  border-top-style: solid;
}

.pd-steps {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  font-size: 0.8rem;
  color: $muted;
}

.pd-steps span {
  flex: 1;
  text-align: center;
  padding: 6px 4px;
  border-radius: 999px;
  background: rgba(215, 127, 162, 0.08);
}

.pd-steps span.on {
  color: $pink;
  font-weight: 600;
}

.pd-steps span.done {
  background: rgba(215, 127, 162, 0.2);
}

.pd-option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.pd-opt {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  text-align: left;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(215, 127, 162, 0.25);
  background: #fffafb;
  cursor: pointer;
}

.pd-opt__rank {
  font-weight: 700;
  color: $pink;
}

.pd-opt__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pd-opt__title {
  font-weight: 600;
}

.pd-opt__desc {
  font-size: 0.85rem;
  color: $muted;
}

.pd-grid-wrap {
  overflow-x: auto;
  margin: 14px 0;
}

.pd-grid {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.75rem;
}

.pd-grid th {
  padding: 6px 4px;
  font-weight: 500;
  color: $muted;
}

.pd-cell {
  width: 28px;
  height: 28px;
  margin: 4px auto;
  display: block;
  border-radius: 8px;
  border: 1px solid rgba(215, 127, 162, 0.35);
  background: #fff;
  cursor: pointer;
}

.pd-cell.on {
  background: $pink;
  border-color: $pink;
}

.pd-primary,
.pd-secondary {
  margin-top: 12px;
  width: 100%;
  padding: 12px;
  border-radius: 12px;
  border: none;
  font-size: 1rem;
  cursor: pointer;
}

.pd-primary {
  background: linear-gradient(135deg, #e8a0bf, $pink);
  color: #fff;
}

.pd-primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pd-loc-btns {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.pd-secondary {
  background: #fff;
  border: 1px solid rgba(215, 127, 162, 0.35);
  color: $text;
}

.pd-poll-hint {
  font-size: 0.8rem;
  color: $muted;
}

.pd-countdown {
  margin-top: 16px;
  padding: 14px;
  border-radius: 12px;
  background: rgba(215, 127, 162, 0.1);
  text-align: center;
}

.pd-countdown__nums {
  font-size: 1.35rem;
  font-weight: 700;
  color: $pink;
  margin: 6px 0 0;
}

.pd-countdown__nums.urgent {
  color: #e14d6e;
}

.pd-reason {
  margin-top: 6px;
}
</style>
