<template>
  <div class="chat-room">
    <div class="global-aurora-bg"></div>

    <div class="chat-container">
      <div class="chat-header glass-panel-top">
        <button class="icon-btn back-btn" @click="$router.push('/chat')">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="avatar-glow-wrap" style="--glow-color: rgba(79, 140, 255, 0.2)">
          <AppAvatar
            :src="otherUser?.avatarUrl"
            :name="otherUser?.nickname"
            :size="40"
            class="avatar clickable-avatar"
            @click="$router.push(`/profile/${otherUserId}`)"
          />
        </div>
        <div class="header-info">
          <div class="header-name text-main">{{ otherUser ? followStore.getDisplayName(otherUser.id, otherUser.nickname) : '感知信号中...' }}</div>
          <div v-if="otherUser" class="header-status text-accent-blue"><span class="pulse-dot-sm"></span> 引力场连接中</div>
        </div>
        <button class="icon-btn more-btn" @click="$router.push(`/profile/${otherUserId}`)">
          <el-icon><User /></el-icon>
        </button>
      </div>

      <div ref="messageListRef" class="message-list" @scroll="onMessageListScroll">
        <div v-if="noMoreHistory && messages.length > 0" class="no-more-hint glass-pill-light mx-auto w-fit px-4 py-1">星轨记录已到尽头</div>
        
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-row', { mine: msg.senderId === myId }]"
        >
          <AppAvatar
            v-if="msg.senderId !== myId"
            :src="msg.senderAvatar"
            :name="msg.senderNickname"
            :size="40"
            class="avatar msg-avatar clickable-avatar shadow-sm"
            @click="$router.push(`/profile/${msg.senderId}`)"
          />
          
          <div
            v-if="!msg.deleted && (isInviteMessage(msg) || msg.msgType === 5)"
            class="message-card-wrapper"
          >
            <template v-if="isInviteMessage(msg)">
              <div v-if="parsedInvite(msg.content)" class="invite-card-in-chat glass-card-light">
                <div class="invite-card-title text-main font-bold">{{ parsedInvite(msg.content)?.title || '心动邀约' }}</div>
                <div v-if="parsedInvite(msg.content)?.timeStr" class="invite-card-meta text-sub">
                  <el-icon><Clock /></el-icon>
                  {{ parsedInvite(msg.content)?.timeStr }}
                </div>
                <div class="mt-3 flex gap-2">
                  <template v-if="msg.senderId === myId">
                    <button class="glow-btn-warm px-4 py-1 text-sm h-8" @click="goToInvite(msg.content)">查看详情</button>
                  </template>
                  <template v-else>
                    <template v-if="inviteActionState[parsedInvite(msg.content)?.inviteId ?? 0] === 'accepted'">
                      <span class="invite-card-done glass-pill-light px-3 py-1 text-accent-pink">已接受</span>
                    </template>
                    <template v-else-if="inviteActionState[parsedInvite(msg.content)?.inviteId ?? 0] === 'declined'">
                      <span class="invite-card-done declined glass-pill-light px-3 py-1 text-sub">已婉拒</span>
                    </template>
                    <template v-else>
                      <button class="glow-btn-warm px-4 py-1 text-sm h-8" @click="handleAcceptInvite(msg.content)">接受</button>
                      <button class="glass-btn px-4 py-1 text-sm h-8" @click="handleDeclineInvite(msg.content)">婉拒</button>
                    </template>
                  </template>
                </div>
              </div>
              <div v-else class="invite-fallback glass-card-light">
                <p class="msg-content text-main">{{ msg.content }}</p>
                <button class="text-accent-pink bg-transparent border-none font-bold text-sm cursor-pointer mt-2" @click="goToInvite(msg.content)">查看邀约详情 ➜</button>
                <p class="invite-fallback-hint text-xs text-sub mt-1">请到「同行」查看并处理</p>
              </div>
            </template>
            
            <div v-else class="post-share-card glass-card-light" @click="goToSharedPost(msg.content)">
              <div class="share-card-header">
                <el-icon :size="16" class="text-accent-pink"><Share /></el-icon>
                <span class="share-label text-sub">帖子分享</span>
              </div>
              <div class="share-card-author text-main font-bold">
                {{ getSharedPostInfo(msg.content).nickname }}
              </div>
              <div class="share-card-content text-sub">
                {{ getSharedPostInfo(msg.content).content }}
              </div>
              <div v-if="getSharedPostInfo(msg.content).images" class="share-card-images">
                <img
                  v-for="(img, idx) in getSharedPostInfo(msg.content).images.slice(0, 3)"
                  :key="idx"
                  :src="img"
                  class="share-card-img"
                />
              </div>
            </div>
            <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
          </div>

          <div v-else class="message-bubble" :class="{ 'glass-card-light': msg.senderId !== myId }">
            <template v-if="msg.deleted">
              <p class="msg-content msg-recalled"><el-icon><InfoFilled /></el-icon> 信号已被撤回</p>
              <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
            </template>
            <template v-else-if="isImageMessage(msg)">
              <el-image
                :src="imageUrl(msg.content)"
                :preview-src-list="[imageUrl(msg.content)]"
                fit="cover"
                class="chat-image shadow-sm"
                preview-teleported
              />
              <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
            </template>
            <template v-else>
              <p class="msg-content" :class="{ 'msg-recalled': msg.deleted }">{{ msg.content }}</p>
              <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
            </template>
          </div>
          <button v-if="msg.senderId === myId && canRecall(msg)" type="button" class="recall-btn recall-btn-left" @click.stop="handleRecall(msg.id)">撤回</button>
        </div>
      </div>

      <div class="chat-input-area glass-panel-bottom">
        <div v-if="!canSend" class="chat-limit-hint glass-pill-light text-center py-1 text-accent-pink text-xs mb-2">未回复前只能发送一条消息，等待回音...</div>
        
        <input ref="imageInputRef" type="file" accept="image/*" class="hidden-file-input" @change="handleMediaSelect" />
        
        <div class="input-row" :class="{ disabled: !canSend }">
          <button type="button" class="icon-btn action-icon" title="图片/视频" :disabled="!canSend" @click="canSend && triggerImageInput()">
            <el-icon :size="22"><Picture /></el-icon>
          </button>
          <EmojiPicker @insert="insertEmoji" class="action-icon" />
          
          <el-input 
            ref="inputRef" 
            v-model="inputText" 
            :placeholder="canSend ? '发射引力波...' : '等待对方回复后可继续发送'" 
            size="large" 
            class="glass-input"
            :disabled="!canSend" 
            @keyup.enter="handleSend"
          >
            <template #append>
              <button class="send-btn" :class="{'active-send': inputText.trim() && canSend}" :disabled="!inputText.trim() || !canSend" @click="handleSend">
                <el-icon :size="20"><Promotion /></el-icon>
              </button>
            </template>
          </el-input>
        </div>

        <div v-if="iceBreakStatus?.canShow || iceBreakStatus?.canAllow" class="ice-break-below-input">
          <div class="flex justify-between items-center w-full">
            <div v-if="iceBreakStatus?.canShow" class="ice-break-lamp-wrap glass-pill-light px-3 py-1">
              <button
                type="button"
                :class="['icon-btn', 'ice-break-btn', { disabled: !iceBreakStatus.targetEnabled }]"
                :title="iceBreakStatus.targetEnabled ? '破冰灵感' : '对方暂未允许您获取破冰灵感'"
                :disabled="!iceBreakStatus.targetEnabled"
                @click="iceBreakStatus.targetEnabled && handleIceBreakClick()"
              >
                💡
              </button>
              <span class="ice-break-lamp-label text-xs font-bold" :class="iceBreakStatus.targetEnabled ? 'text-accent-blue' : 'text-sub'">破冰灵感</span>
            </div>
            
            <div v-if="iceBreakStatus?.canAllow" class="ice-break-allow-row glass-pill-light px-3 py-1 ml-auto">
              <span class="ice-break-allow-label text-xs text-sub mr-2 font-bold">允许TA获取破冰</span>
              <el-switch :model-value="iceBreakStatus.allowedByMe" :loading="iceBreakAllowLoading" @update:model-value="handleIceBreakAllowChange" />
            </div>
          </div>
        </div>
      </div>

      <Transition name="ice-break-panel">
        <div v-if="showIceBreakPanel" class="ice-break-panel glass-panel">
          <div class="ice-break-panel-header">
            <span class="text-gradient-warm font-bold"><el-icon><Opportunity /></el-icon> AI 破冰分析</span>
            <button type="button" class="ice-break-close icon-btn" @click="showIceBreakPanel = false" aria-label="关闭">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>
          <div v-if="iceBreakLoading" class="ice-break-loading flex flex-col items-center py-4">
            <div class="pulse-ring mb-2"></div>
            <span class="text-sm text-sub">AI 正在分析你们的引力交集...</span>
          </div>
          <template v-else>
            <p v-if="iceBreakAnalysis" class="ice-break-analysis text-main">{{ iceBreakAnalysis }}</p>
            <div class="ice-break-topics">
              <button
                v-for="(t, i) in iceBreakTopics"
                :key="i"
                type="button"
                class="ice-break-topic-btn glass-pill-light"
                @click="selectIceBreakTopic(t)"
              >
                {{ t }}
              </button>
            </div>
          </template>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/store/chatStore'
import { useUserStore } from '@/store/userStore'
import { useNotifyDismissStore } from '@/store/notifyDismissStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useFollowStore } from '@/store/followStore'
import { getChatHistory, markAsRead, uploadChatImage, canSendTo, recallMessage } from '@/api/chatApi'
import { getIceBreakStatus, getIceBreakTopics, updateIceBreakAllow, type IceBreakStatus } from '@/api/userApi'
import { getUserProfile, type UserProfile } from '@/api/userApi'
import { joinInvite, declineInvite } from '@/api/inviteApi'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { formatLocalDateTime } from '@/utils/dateTime'
import EmojiPicker from '@/components/EmojiPicker.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { Close, ArrowLeft, User, Clock, Share, Picture, Promotion, Opportunity, InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()
const notifyDismissStore = useNotifyDismissStore()
const badgeStore = useBadgeStore()
const followStore = useFollowStore()
const { currentMessages } = storeToRefs(chatStore)

const otherUserId = computed(() => Number(route.params.userId))
const myId = computed(() => userStore.user?.id)
const otherUser = ref<UserProfile | null>(null)
const inputText = ref('')
const messageListRef = ref<HTMLElement>()
const historyPage = ref(1)
const noMoreHistory = ref(false)
const loadingHistory = ref(false)
const imageInputRef = ref<HTMLInputElement | null>(null)
const inputRef = ref<{ $el: HTMLElement } | null>(null)
const canSend = ref(false)
const iceBreakStatus = ref<IceBreakStatus | null>(null)
const showIceBreakPanel = ref(false)
const iceBreakAnalysis = ref('')
const iceBreakTopics = ref<string[]>([])
const iceBreakLoading = ref(false)
const iceBreakAllowLoading = ref(false)

const messages = computed(() => {
  const filtered = currentMessages.value.filter(m =>
    (m.senderId === myId.value && m.receiverId === otherUserId.value) ||
    (m.senderId === otherUserId.value && m.receiverId === myId.value)
  )
  const seen = new Set<number | string>()
  return filtered.filter(m => {
    const key = typeof m.id === 'number' ? m.id : String(m.id)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})

onMounted(async () => {
  historyPage.value = 1
  noMoreHistory.value = false
  chatStore.connectWebSocket()
  try {
    const [profileRes, historyRes] = await Promise.all([
      getUserProfile(otherUserId.value),
      getChatHistory(otherUserId.value, 1, 30),
    ])
    otherUser.value = profileRes.data.data
    const loaded = historyRes.data.data?.reverse() || []
    const myIdNum = myId.value
    const otherId = otherUserId.value
    const belong = (m: { senderId: number; receiverId: number }) =>
      (m.senderId === myIdNum && m.receiverId === otherId) ||
      (m.senderId === otherId && m.receiverId === myIdNum)
    chatStore.setMessagesMergedWithHistory(loaded, m => belong(m), chatStore.getPendingForUser(otherId))
    await markAsRead(otherUserId.value)
    badgeStore.fetchBadges()
    const canRes = await canSendTo(otherUserId.value)
    canSend.value = canRes.data?.data ?? true
    scrollToBottom()

    const uid = otherUserId.value
    if (Number.isFinite(uid) && uid > 0) {
      try {
        const statusRes = await getIceBreakStatus(uid)
        iceBreakStatus.value = statusRes.data.data ?? null
      } catch {
        iceBreakStatus.value = canSend.value ? { canShow: true, targetEnabled: false, allowedByMe: false, canAllow: true } : null
      }
    } else { iceBreakStatus.value = null }
  } catch { /* handled */ }
})

function onMessageListScroll() {
  const el = messageListRef.value
  if (!el || loadingHistory.value || noMoreHistory.value) return
  if (el.scrollTop < 50) loadMoreHistory()
}

async function loadMoreHistory() {
  if (loadingHistory.value || noMoreHistory.value) return
  loadingHistory.value = true
  const el = messageListRef.value
  const oldScrollHeight = el?.scrollHeight ?? 0
  try {
    const nextPage = historyPage.value + 1
    const res = await getChatHistory(otherUserId.value, nextPage, 30)
    const loaded = res.data.data?.reverse() || []
    if (loaded.length === 0) noMoreHistory.value = true
    else {
      historyPage.value = nextPage
      const myIdNum = myId.value
      const otherId = otherUserId.value
      const belong = (m: { senderId: number; receiverId: number }) => (m.senderId === myIdNum && m.receiverId === otherId) || (m.senderId === otherId && m.receiverId === myIdNum)
      chatStore.setMessagesMergedWithHistory(loaded, m => belong(m), [])
    }
  } finally {
    loadingHistory.value = false
    await nextTick()
    if (el && oldScrollHeight > 0) el.scrollTop = el.scrollHeight - oldScrollHeight
  }
}

watch(messages, () => { nextTick(scrollToBottom) }, { deep: true })

watch(messages, (list) => {
  const fromThem = list.some(m => m.senderId === otherUserId.value && m.receiverId === myId.value)
  if (fromThem && !canSend.value) canSend.value = true
}, { deep: true })

function scrollToBottom() { if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight }

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || !canSend.value) return
  const now = formatLocalDateTime()
  chatStore.pushOptimisticMessage({
    id: -Date.now(), senderId: myId.value!, receiverId: otherUserId.value,
    senderNickname: userStore.user?.nickname ?? '', senderAvatar: userStore.user?.avatarUrl ?? null,
    content: text, msgType: 1, isRead: false, createdAt: now,
  }, { pendingOtherUserId: otherUserId.value })
  chatStore.sendMessage(otherUserId.value, text)
  notifyDismissStore.undismissChat(otherUserId.value)
  inputText.value = ''
  nextTick(scrollToBottom)
  try {
    const res = await canSendTo(otherUserId.value)
    canSend.value = res.data?.data ?? false
  } catch { /* ignore */ }
}

function isInviteMessage(msg: { msgType?: number; content?: string }): boolean { return msg.msgType === 4 || !!(msg.content && String(msg.content).includes('INVITE#')) }
function isImageMessage(msg: { msgType?: number; content?: string }): boolean {
  if (msg.msgType !== 2 && msg.msgType !== 3) return false
  const c = msg.content && String(msg.content).trim()
  if (!c) return false
  return c.startsWith('/uploads') || c.startsWith('/api/uploads') || c.startsWith('http')
}

function getSharedPostInfo(content: string | undefined): { nickname: string; content: string; images: string[] } {
  if (!content) return { nickname: '', content: '', images: [] }
  try {
    const data = JSON.parse(content)
    return { nickname: data.postNickname || '', content: data.postContent || '', images: data.postImages ? data.postImages.split(',').filter(Boolean) : [] }
  } catch { return { nickname: '', content: '', images: [] } }
}

function getSharedPostId(content: string | undefined): number | null {
  if (!content) return null
  try {
    const data = JSON.parse(content); const id = data.postId
    if (id == null || id === '' || id === 'NaN') return null
    const n = Number(id); return (Number.isFinite(n) && n > 0) ? Math.floor(n) : null
  } catch { return null }
}

function goToSharedPost(content: string | undefined) { const postId = getSharedPostId(content); if (postId) router.push(`/feed/${postId}`) }

async function handleIceBreakClick() {
  if (!iceBreakStatus.value?.targetEnabled || iceBreakLoading.value) return
  iceBreakLoading.value = true; showIceBreakPanel.value = true; iceBreakAnalysis.value = ''; iceBreakTopics.value = []
  try {
    const res = await getIceBreakTopics(otherUserId.value); const data = res.data.data
    iceBreakAnalysis.value = data?.analysis || ''; iceBreakTopics.value = data?.topics || []
  } catch { ElMessage.error('获取破冰灵感失败'); showIceBreakPanel.value = false } 
  finally { iceBreakLoading.value = false }
}

function selectIceBreakTopic(topic: string) { inputText.value = topic; showIceBreakPanel.value = false; inputRef.value?.$el?.querySelector('input')?.focus() }

function canRecall(msg: { senderId: number; deleted?: boolean; createdAt?: string }): boolean {
  if (msg.senderId !== myId.value || msg.deleted || !msg.createdAt) return false
  return new Date(msg.createdAt).getTime() >= Date.now() - 60 * 60 * 1000
}

async function handleRecall(messageId: number) {
  try {
    await recallMessage(messageId); chatStore.updateMessageRecall(messageId); ElMessage.success('已撤回')
    historyPage.value = 1; noMoreHistory.value = false
    const res = await getChatHistory(otherUserId.value, 1, 30)
    const loaded = (res.data.data || []).reverse()
    const myIdNum = myId.value; const otherId = otherUserId.value
    const belong = (m: { senderId: number; receiverId: number }) => (m.senderId === myIdNum && m.receiverId === otherId) || (m.senderId === otherId && m.receiverId === myIdNum)
    chatStore.setMessagesMergedWithHistory(loaded, m => belong(m), [])
    nextTick(scrollToBottom)
  } catch { /* 错误已拦截 */ }
}

async function handleIceBreakAllowChange(allowed: boolean) {
  if (iceBreakAllowLoading.value) return
  iceBreakAllowLoading.value = true
  try {
    await updateIceBreakAllow(otherUserId.value, allowed)
    if (iceBreakStatus.value) iceBreakStatus.value = { ...iceBreakStatus.value, allowedByMe: allowed }
    ElMessage.success(allowed ? '已允许对方使用破冰' : '已关闭')
  } catch { /* 错误已拦截 */ } finally { iceBreakAllowLoading.value = false }
}

function parsedInvite(content: string): { title: string; timeStr: string; inviteId: number } | null {
  const match = content.match(/INVITE#(\d+)/); if (!match || !match[1]) return null
  const inviteId = parseInt(match[1], 10); const parts = content.split(/[｜|]/)
  let title = '邀约邀请'; let timeStr = ''
  if (parts[0]) title = parts[0].replace(/^邀约邀请：?/, '').trim() || title
  if (parts[1]) timeStr = parts[1].replace(/^时间\s*/, '').trim()
  return { title, timeStr, inviteId }
}

const inviteActionState = ref<Record<number, 'accepted' | 'declined'>>({})

async function handleAcceptInvite(content: string) {
  const parsed = parsedInvite(content); if (!parsed) return
  try { await joinInvite(parsed.inviteId); inviteActionState.value[parsed.inviteId] = 'accepted'; ElMessage.success('已接受邀约'); router.push(`/invite/${parsed.inviteId}`) } 
  catch (e: any) { ElMessage.error(e?.response?.data?.message ?? e?.message ?? '操作失败') }
}

async function handleDeclineInvite(content: string) {
  const parsed = parsedInvite(content); if (!parsed) return
  try { await declineInvite(parsed.inviteId); inviteActionState.value[parsed.inviteId] = 'declined'; ElMessage.success('已拒绝') } 
  catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

function goToInvite(content: string) {
  const parsed = parsedInvite(content); if (parsed) router.push(`/invite/${parsed.inviteId}`); else ElMessage.warning('未找到邀约信息')
}

function imageUrl(url: string) {
  if (!url) return ''; if (url.startsWith('http') || url.startsWith('/api')) return url; return '/api' + (url.startsWith('/') ? url : '/' + url)
}

function triggerImageInput() { imageInputRef.value?.click() }

function insertEmoji(emoji: string) {
  const el = inputRef.value?.$el?.querySelector('input, textarea') as HTMLInputElement | null
  if (el) {
    const start = el.selectionStart ?? inputText.value.length; const end = el.selectionEnd ?? inputText.value.length
    const before = inputText.value.slice(0, start); const after = inputText.value.slice(end)
    inputText.value = before + emoji + after
    nextTick(() => { el.focus(); const pos = before.length + emoji.length; el.setSelectionRange(pos, pos) })
  } else { inputText.value += emoji }
}

async function handleMediaSelect(e: Event) {
  const input = e.target as HTMLInputElement; const file = input.files?.[0]
  if (!file || !canSend.value) return
  input.value = ''
  if (file.type.startsWith('video/')) { ElMessage.info('聊天暂不支持发送视频，请使用图片'); return }
  try {
    const res = await uploadChatImage(file); const imageUrlVal = res.data.data
    if (!imageUrlVal) throw new Error('上传失败')
    const now = formatLocalDateTime()
    chatStore.pushOptimisticMessage({
      id: -Date.now(), senderId: myId.value!, receiverId: otherUserId.value, senderNickname: userStore.user?.nickname ?? '',
      senderAvatar: userStore.user?.avatarUrl ?? null, content: imageUrlVal, msgType: 3, isRead: false, createdAt: now,
    }, { pendingOtherUserId: otherUserId.value })
    chatStore.sendMessage(otherUserId.value, imageUrlVal, 3); notifyDismissStore.undismissChat(otherUserId.value); nextTick(scrollToBottom)
    const canRes = await canSendTo(otherUserId.value); canSend.value = canRes.data?.data ?? false
  } catch { ElMessage.error('图片上传失败') }
}
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 聊天室 UI
   ========================================== */
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$border-light: rgba(255, 255, 255, 0.8);
$serif: 'Noto Serif SC', 'Songti SC', 'STSong', serif;
$mobile-tab-reserve: 72px;

.chat-room {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;

  @media (max-width: 640px) {
    height: calc(100vh - #{$mobile-tab-reserve});
    max-height: calc(100vh - #{$mobile-tab-reserve});
    height: calc(100dvh - #{$mobile-tab-reserve});
    max-height: calc(100dvh - #{$mobile-tab-reserve});
  }
}

// 全局底色
.global-aurora-bg {
  position: absolute; inset: 0; pointer-events: none; z-index: 0; background: #f8fafc;
  &::after {
    content: ''; position: absolute; inset: 0;
    background: 
      radial-gradient(circle at 0% 10%, rgba(79, 140, 255, 0.08), transparent 40%),
      radial-gradient(circle at 100% 80%, rgba(255, 51, 102, 0.06), transparent 40%);
  }
}

.chat-container {
  max-width: 800px; width: 100%; margin: 0 auto; height: 100%;
  display: flex; flex-direction: column; position: relative; z-index: 1;
}

// === 玻璃态工具类 ===
.glass-panel-top { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(24px); border-bottom: 1px solid $border-light; box-shadow: 0 4px 20px rgba(0,0,0,0.02);}
.glass-panel-bottom { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(24px); border-top: 1px solid $border-light; box-shadow: 0 -4px 20px rgba(0,0,0,0.02);}
.glass-card-light { background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(12px); border: 1px solid #ffffff; border-radius: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
.glass-pill-light { background: rgba(255, 255, 255, 0.5); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 16px; }

.text-gradient-warm { background: linear-gradient(135deg, $accent-pink, $accent-orange); -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800; }
.glow-btn-warm { height: 36px; border-radius: 999px; border: none; display: inline-flex; align-items: center; justify-content: center; background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white; font-weight: 700; cursor: pointer; box-shadow: 0 4px 15px rgba(255, 51, 102, 0.3); transition: all 0.3s; &:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255, 51, 102, 0.4); } }
.glass-btn { height: 36px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff; color: $text-sub; font-weight: 700; cursor: pointer; transition: all 0.3s; &:hover { background: #fff; color: $text-main; box-shadow: 0 4px 12px rgba(0,0,0,0.05); } }
.text-main { color: $text-main; }
.text-sub { color: $text-sub; }
.text-accent-pink { color: $accent-pink; }
.text-accent-blue { color: $accent-blue; }
.font-bold { font-weight: 700; }
.shadow-sm { box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.pulse-dot-sm { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: $accent-blue; animation: pulse-blue 2s infinite; margin-right: 2px;}

@keyframes pulse-blue { 0% { box-shadow: 0 0 0 0 rgba(79,140,255, 0.4); } 70% { box-shadow: 0 0 0 4px rgba(79,140,255, 0); } 100% { box-shadow: 0 0 0 0 rgba(79,140,255, 0); } }

// === 头部 ===
.chat-header {
  display: flex; align-items: center; gap: 16px; padding: 12px 20px; flex-shrink: 0;
  
  .icon-btn { width: 36px; height: 36px; border-radius: 50%; border: none; background: rgba(255,255,255,0.5); color: $text-main; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; &:hover { background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.05);} }
  .avatar-glow-wrap { border-radius: 50%; padding: 2px; background: var(--glow-color); box-shadow: 0 0 8px var(--glow-color); }
  .avatar { border: 2px solid #fff; }
  
  .header-info { flex: 1; }
  .header-name { font-size: 16px; font-weight: 800; font-family: $serif;}
  .header-status { font-size: 11px; font-weight: 600; margin-top: 2px; }
}

// === 消息列表 ===
.message-list {
  flex: 1; min-height: 0; overflow-y: auto; -webkit-overflow-scrolling: touch;
  padding: 24px 20px; display: flex; flex-direction: column; gap: 16px;
  
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }
}

.no-more-hint { font-size: 12px; color: $text-sub; text-align: center; border: 1px solid rgba(255,255,255,0.5); }

.message-row {
  display: flex; align-items: flex-end; gap: 12px; max-width: 85%;
  animation: slide-up 0.3s ease out;
  
  &.mine {
    align-self: flex-end; flex-direction: row-reverse;
    .message-bubble {
      background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white; border: none; box-shadow: 0 4px 15px rgba(255,51,102,0.2);
      .msg-time { color: rgba(255,255,255,0.8); }
      .msg-recalled { color: rgba(255,255,255,0.7); font-style: italic; }
    }
  }
}

@keyframes slide-up { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.msg-avatar { flex-shrink: 0; }
.clickable-avatar { cursor: pointer; transition: transform 0.2s; &:hover{ transform: scale(1.05); } }

.message-bubble {
  position: relative; padding: 12px 16px; border-radius: 20px; max-width: 100%;
  .msg-content { font-size: 15px; line-height: 1.5; word-wrap: break-word; margin: 0; }
  .msg-recalled { color: $text-sub; font-style: italic; display: flex; align-items: center; gap: 4px;}
  .msg-time { font-size: 11px; display: block; text-align: right; margin-top: 6px; color: $text-sub; font-weight: 600;}
}

// 邀约/帖子分享卡片
.message-card-wrapper { position: relative; max-width: 100%; }

.invite-card-in-chat {
  padding: 16px; min-width: 200px;
  .invite-card-meta { font-size: 12px; display: flex; align-items: center; gap: 4px; margin-bottom: 12px; font-weight: 600;}
  .invite-card-done { font-size: 12px; font-weight: 700; border: 1px solid rgba(255,255,255,0.8); }
}

.invite-fallback { padding: 16px; min-width: 200px; }

.post-share-card {
  padding: 16px; max-width: 260px; cursor: pointer; transition: all 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,0,0,0.06); border-color: rgba($accent-pink, 0.3);}
  .share-card-header { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; font-size: 12px; font-weight: 600;}
  .share-card-content { font-size: 13px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-top: 4px;}
  .share-card-images { display: flex; gap: 6px; margin-top: 10px; .share-card-img { width: 50px; height: 50px; object-fit: cover; border-radius: 8px; border: 1px solid rgba(0,0,0,0.05);} }
}

.chat-image { max-width: 220px; max-height: 220px; border-radius: 16px; cursor: pointer; display: block; border: 2px solid rgba(255,255,255,0.5); }

.recall-btn-left {
  flex-shrink: 0; font-size: 12px; color: $text-sub; background: rgba(255,255,255,0.6); border: 1px solid #fff; border-radius: 999px;
  cursor: pointer; padding: 4px 10px; align-self: center; transition: all 0.2s; font-weight: 600;
  &:hover { background: #fff; color: #f56c6c; box-shadow: 0 2px 8px rgba(0,0,0,0.05);}
}

// === 底部输入区 ===
.chat-input-area {
  padding: 16px 20px; padding-bottom: max(16px, env(safe-area-inset-bottom, 16px)); flex-shrink: 0; position: relative; z-index: 10;
  .input-row.disabled { opacity: 0.6; pointer-events: none; }
  .input-row { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
  
  .action-icon {
    width: 40px; height: 40px; border-radius: 50%; border: none; background: rgba(255,255,255,0.6); color: $text-sub; cursor: pointer;
    display: flex; align-items: center; justify-content: center; transition: all 0.2s; flex-shrink: 0; box-shadow: 0 2px 6px rgba(0,0,0,0.02);
    &:hover { background: #fff; color: $accent-pink; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
  }

  .glass-input :deep(.el-input__wrapper) {
    background: rgba(255,255,255,0.7); backdrop-filter: blur(12px); border-radius: 999px; border: 1px solid rgba(255,255,255,0.9);
    box-shadow: inset 0 2px 6px rgba(0,0,0,0.02); padding-left: 16px;
    &.is-focus { border-color: rgba($accent-pink, 0.4); box-shadow: 0 0 0 2px rgba($accent-pink, 0.1); }
  }
  .glass-input :deep(.el-input-group__append) { padding: 0; background: transparent; border: none; box-shadow: none; margin-left: 8px;}

  .send-btn {
    width: 40px; height: 40px; border-radius: 50%; border: none; display: flex; align-items: center; justify-content: center;
    background: rgba(255,255,255,0.6); color: $text-sub; cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 6px rgba(0,0,0,0.02);
    &.active-send { background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white; box-shadow: 0 4px 15px rgba(255, 51, 102, 0.3); }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}

// 破冰区
.ice-break-below-input { margin-top: 12px; }
.ice-break-btn { 
  font-size: 18px !important; background: transparent !important; box-shadow: none !important; padding: 0;
  &.disabled { opacity: 0.5; filter: grayscale(100%); cursor: not-allowed; }
}

.ice-break-panel {
  position: absolute; bottom: 100px; left: 20px; right: 20px; padding: 20px; border-radius: 24px; z-index: 20;
  
  .ice-break-panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
  .pulse-ring { width: 36px; height: 36px; border-radius: 50%; border: 3px solid rgba(255,51,102,0.2); border-top-color: $accent-pink; animation: spin 1s linear infinite; }
  
  .ice-break-analysis { font-size: 14px; line-height: 1.6; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid rgba(0,0,0,0.05); }
  .ice-break-topics { display: flex; flex-wrap: wrap; gap: 10px; }
  .ice-break-topic-btn {
    padding: 8px 16px; font-size: 13px; font-weight: 700; color: $accent-pink; border: 1px solid rgba(255,255,255,0.8);
    cursor: pointer; transition: all 0.2s;
    &:hover { background: #fff; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(255,51,102,0.1); }
  }
}
.ice-break-panel-enter-active, .ice-break-panel-leave-active { transition: opacity 0.3s, transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1); }
.ice-break-panel-enter-from, .ice-break-panel-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }
.hidden-file-input { display: none; }

@keyframes spin { 100% { transform: rotate(360deg); } }

@media (max-width: 640px) {
  .chat-header { padding: 12px 16px; }
  .message-list { padding: 16px; }
  .message-row { max-width: 90%; }
  .chat-input-area { padding: 12px 16px; padding-bottom: max(12px, env(safe-area-inset-bottom, 12px)); }
  .ice-break-panel { left: 16px; right: 16px; bottom: 80px; padding: 16px;}
}
</style>