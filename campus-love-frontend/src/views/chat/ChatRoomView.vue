<template>
  <div class="chat-room">
    <div class="chat-header">
      <button class="back-btn" @click="$router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <img :src="otherUser?.avatarUrl || defaultAvatar" class="avatar clickable-avatar" width="36" height="36" @click="$router.push(`/profile/${otherUserId}`)" />
      <div class="header-info">
        <div class="header-name">{{ otherUser ? followStore.getDisplayName(otherUser.id, otherUser.nickname) : '加载中...' }}</div>
        <div class="header-status">{{ chatStore.connected ? '在线' : '离线' }}</div>
      </div>
      <button class="more-btn" @click="$router.push(`/profile/${otherUserId}`)">
        <el-icon><User /></el-icon>
      </button>
    </div>

    <div ref="messageListRef" class="message-list" @scroll="onMessageListScroll">
      <div v-if="noMoreHistory && messages.length > 0" class="no-more-hint">没有更多消息</div>
      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['message-row', { mine: msg.senderId === myId }]"
      >
        <img
          v-if="msg.senderId !== myId"
          :src="msg.senderAvatar || defaultAvatar"
          class="avatar msg-avatar clickable-avatar"
          width="36"
          height="36"
          @click="$router.push(`/profile/${msg.senderId}`)"
        />
        <!-- 邀约/帖子分享：独立卡片，不用气泡包裹 -->
        <div
          v-if="!msg.deleted && (isInviteMessage(msg) || msg.msgType === 5)"
          class="message-card-wrapper"
        >
          <template v-if="isInviteMessage(msg)">
            <div v-if="parsedInvite(msg.content)" class="invite-card-in-chat">
              <div class="invite-card-title">{{ parsedInvite(msg.content)?.title || '邀约邀请' }}</div>
              <div v-if="parsedInvite(msg.content)?.timeStr" class="invite-card-meta">
                <el-icon><Clock /></el-icon>
                {{ parsedInvite(msg.content)?.timeStr }}
              </div>
              <template v-if="msg.senderId === myId">
                <el-button class="invite-card-btn" type="primary" size="small" @click="goToInvite(msg.content)">
                  查看详情
                </el-button>
              </template>
              <template v-else>
                <template v-if="inviteActionState[parsedInvite(msg.content)?.inviteId ?? 0] === 'accepted'">
                  <span class="invite-card-done">已接受</span>
                </template>
                <template v-else-if="inviteActionState[parsedInvite(msg.content)?.inviteId ?? 0] === 'declined'">
                  <span class="invite-card-done declined">已拒绝</span>
                </template>
                <template v-else>
                  <el-button class="invite-card-btn" type="primary" size="small" @click="handleAcceptInvite(msg.content)">
                    同意
                  </el-button>
                  <el-button class="invite-card-btn outline" size="small" @click="handleDeclineInvite(msg.content)">
                    拒绝
                  </el-button>
                </template>
              </template>
            </div>
            <div v-else class="invite-fallback">
              <p class="msg-content">{{ msg.content }}</p>
              <el-button class="invite-link-btn" type="primary" text size="small" @click="goToInvite(msg.content)">
                查看邀约详情
              </el-button>
              <p class="invite-fallback-hint">请到「我的邀约」查看并处理</p>
            </div>
          </template>
          <div v-else class="post-share-card" @click="goToSharedPost(msg.content)">
            <div class="share-card-header">
              <el-icon :size="16"><Share /></el-icon>
              <span class="share-label">帖子分享</span>
            </div>
            <div class="share-card-author">
              {{ getSharedPostInfo(msg.content).nickname }}
            </div>
            <div class="share-card-content">
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
        <!-- 文本/图片/已撤回：气泡包裹 -->
        <div v-else class="message-bubble">
          <template v-if="msg.deleted">
            <p class="msg-content msg-recalled">{{ msg.content }}</p>
            <span class="msg-time">{{ msg.createdAt?.slice(11, 16) }}</span>
          </template>
          <template v-else-if="isImageMessage(msg)">
            <el-image
              :src="imageUrl(msg.content)"
              :preview-src-list="[imageUrl(msg.content)]"
              fit="cover"
              class="chat-image"
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

    <!-- 破冰话题面板 -->
    <Transition name="ice-break-panel">
      <div v-if="showIceBreakPanel" class="ice-break-panel">
        <div class="ice-break-panel-header">
          <span>💡 破冰灵感</span>
          <button type="button" class="ice-break-close" @click="showIceBreakPanel = false" aria-label="关闭">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </div>
        <div v-if="iceBreakLoading" class="ice-break-loading">加载中...</div>
        <template v-else>
          <p v-if="iceBreakAnalysis" class="ice-break-analysis">{{ iceBreakAnalysis }}</p>
          <div class="ice-break-topics">
          <button
            v-for="(t, i) in iceBreakTopics"
            :key="i"
            type="button"
            class="ice-break-topic-btn"
            @click="selectIceBreakTopic(t)"
          >
            {{ t }}
          </button>
          </div>
        </template>
      </div>
    </Transition>

    <div class="chat-input-area">
      <div v-if="!canSend" class="chat-limit-hint">未回复前只能发送两条消息</div>
      <!-- 允许对方获取破冰灵感（按好友单独设置） -->
      <div v-if="iceBreakStatus?.canAllow" class="ice-break-allow-row">
        <span class="ice-break-allow-label">允许TA获取破冰灵感</span>
        <el-switch
          :model-value="iceBreakStatus.allowedByMe"
          :loading="iceBreakAllowLoading"
          @update:model-value="handleIceBreakAllowChange"
        />
      </div>
      <input
        ref="imageInputRef"
        type="file"
        accept="image/*"
        class="hidden-file-input"
        @change="handleMediaSelect"
      />
      <div class="input-row" :class="{ disabled: !canSend }">
        <button
          v-if="iceBreakStatus?.canShow"
          type="button"
          :class="['icon-btn', 'ice-break-btn', { disabled: !iceBreakStatus.targetEnabled }]"
          :title="iceBreakStatus.targetEnabled ? '破冰灵感' : '对方暂未允许您获取破冰灵感（可开启下方开关）'"
          :disabled="!iceBreakStatus.targetEnabled"
          @click="iceBreakStatus.targetEnabled && handleIceBreakClick()"
        >
          💡
        </button>
        <button type="button" class="icon-btn" title="图片/视频" :disabled="!canSend" @click="canSend && triggerImageInput()">
          <el-icon :size="20"><Picture /></el-icon>
        </button>
        <EmojiPicker @insert="insertEmoji" />
        <el-input ref="inputRef" v-model="inputText" :placeholder="canSend ? '输入消息...' : '未回复前只能发送两条消息，等待对方回复后可继续发送'" size="large" :disabled="!canSend" @keyup.enter="handleSend">
          <template #append>
            <button class="send-btn" :disabled="!inputText.trim() || !canSend" @click="handleSend">
              <el-icon :size="20"><Promotion /></el-icon>
            </button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
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
import { Close } from '@element-plus/icons-vue'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36"><rect fill="%23f0f2f5" width="36" height="36" rx="18"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="16">👤</text></svg>'

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
/** 未互关时仅允许发一条，对方回复前不能继续发；互关则始终可发。初始 false，等 canSendTo 返回后再设 */
const canSend = ref(false)
/** 破冰功能状态：canShow=互关时显示按钮，targetEnabled=对方已开启时高亮 */
const iceBreakStatus = ref<IceBreakStatus | null>(null)
/** 破冰话题面板 */
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
  // 按 id 去重，保留首次出现（兜底防止乐观消息与回显未正确替换时的重复）
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
    chatStore.setMessagesMergedWithHistory(
      loaded,
      m => belong(m),
      chatStore.getPendingForUser(otherId)
    )
    await markAsRead(otherUserId.value)
    badgeStore.fetchBadges()
    const canRes = await canSendTo(otherUserId.value)
    canSend.value = canRes.data?.data ?? true
    scrollToBottom()
    // 加载破冰功能状态（仅当 otherUserId 有效时）；互关时始终显示按钮，API 失败时用默认状态
    const uid = otherUserId.value
    if (Number.isFinite(uid) && uid > 0) {
      try {
        const statusRes = await getIceBreakStatus(uid)
        iceBreakStatus.value = statusRes.data.data ?? null
      } catch {
        // API 失败时：若互关且可发消息，仍显示破冰区域（按钮置灰，用户可开启「允许对方使用破冰」）
        iceBreakStatus.value = canSend.value ? { canShow: true, targetEnabled: false, allowedByMe: false, canAllow: true } : null
      }
    } else {
      iceBreakStatus.value = null
    }
  } catch { /* handled */ }
})

function onMessageListScroll() {
  const el = messageListRef.value
  if (!el || loadingHistory.value || noMoreHistory.value) return
  if (el.scrollTop < 50) {
    loadMoreHistory()
  }
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
    if (loaded.length === 0) {
      noMoreHistory.value = true
    } else {
      historyPage.value = nextPage
      const myIdNum = myId.value
      const otherId = otherUserId.value
      const belong = (m: { senderId: number; receiverId: number }) =>
        (m.senderId === myIdNum && m.receiverId === otherId) ||
        (m.senderId === otherId && m.receiverId === myIdNum)
      chatStore.setMessagesMergedWithHistory(loaded, m => belong(m), [])
    }
  } finally {
    loadingHistory.value = false
    await nextTick()
    if (el && oldScrollHeight > 0) {
      const newScrollHeight = el.scrollHeight
      el.scrollTop = newScrollHeight - oldScrollHeight
    }
  }
}

watch(messages, () => { nextTick(scrollToBottom) }, { deep: true })

// 收到对方消息时，未互关限制解除，可再次发送
watch(messages, (list) => {
  const fromThem = list.some(m => m.senderId === otherUserId.value && m.receiverId === myId.value)
  if (fromThem && !canSend.value) {
    canSend.value = true
  }
}, { deep: true })

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || !canSend.value) return
  const now = formatLocalDateTime()
  chatStore.pushOptimisticMessage(
    {
      id: -Date.now(),
      senderId: myId.value!,
      receiverId: otherUserId.value,
      senderNickname: userStore.user?.nickname ?? '',
      senderAvatar: userStore.user?.avatarUrl ?? null,
      content: text,
      msgType: 1,
      isRead: false,
      createdAt: now,
    },
    { pendingOtherUserId: otherUserId.value }
  )
  chatStore.sendMessage(otherUserId.value, text)
  notifyDismissStore.undismissChat(otherUserId.value)
  inputText.value = ''
  nextTick(scrollToBottom)
  // 发送后刷新是否还能发（未互关时发一条后变为 false）
  try {
    const res = await canSendTo(otherUserId.value)
    canSend.value = res.data?.data ?? false
  } catch { /* ignore */ }
}

/** 是否为邀约类消息：后端 msgType=4 或 content 含 INVITE# */
function isInviteMessage(msg: { msgType?: number; content?: string }): boolean {
  return msg.msgType === 4 || !!(msg.content && String(msg.content).includes('INVITE#'))
}

/** 是否为图片消息：msgType 2/3 且 content 为图片路径 */
function isImageMessage(msg: { msgType?: number; content?: string }): boolean {
  if (msg.msgType !== 2 && msg.msgType !== 3) return false
  const c = msg.content && String(msg.content).trim()
  if (!c) return false
  return c.startsWith('/uploads') || c.startsWith('/api/uploads') || c.startsWith('http')
}

/** 解析帖子分享内容 */
function getSharedPostInfo(content: string | undefined): { nickname: string; content: string; images: string[] } {
  if (!content) {
    return { nickname: '', content: '', images: [] }
  }
  try {
    const data = JSON.parse(content)
    return {
      nickname: data.postNickname || '',
      content: data.postContent || '',
      images: data.postImages ? data.postImages.split(',').filter(Boolean) : []
    }
  } catch {
    return { nickname: '', content: '', images: [] }
  }
}

/** 获取帖子分享中的 postId（严格校验，避免 NaN 导致后端报错） */
function getSharedPostId(content: string | undefined): number | null {
  if (!content) return null
  try {
    const data = JSON.parse(content)
    const id = data.postId
    if (id == null || id === '' || id === 'NaN') return null
    const n = Number(id)
    if (!Number.isFinite(n) || n <= 0) return null
    return Math.floor(n)
  } catch {
    return null
  }
}

/** 跳转到分享的帖子 */
function goToSharedPost(content: string | undefined) {
  const postId = getSharedPostId(content)
  if (postId) {
    router.push(`/feed/${postId}`)
  }
}

/** 点击破冰灵感按钮 */
async function handleIceBreakClick() {
  if (!iceBreakStatus.value?.targetEnabled || iceBreakLoading.value) return
  iceBreakLoading.value = true
  showIceBreakPanel.value = true
  iceBreakAnalysis.value = ''
  iceBreakTopics.value = []
  try {
    const res = await getIceBreakTopics(otherUserId.value)
    const data = res.data.data
    iceBreakAnalysis.value = data?.analysis || ''
    iceBreakTopics.value = data?.topics || []
  } catch {
    ElMessage.error('获取破冰灵感失败')
    showIceBreakPanel.value = false
  } finally {
    iceBreakLoading.value = false
  }
}

/** 选择破冰话题并填入输入框 */
function selectIceBreakTopic(topic: string) {
  inputText.value = topic
  showIceBreakPanel.value = false
  inputRef.value?.$el?.querySelector('input')?.focus()
}

/** 是否可撤回（本人发送、未撤回、1小时内） */
function canRecall(msg: { senderId: number; deleted?: boolean; createdAt?: string }): boolean {
  if (msg.senderId !== myId.value || msg.deleted) return false
  if (!msg.createdAt) return false
  const created = new Date(msg.createdAt).getTime()
  const oneHourAgo = Date.now() - 60 * 60 * 1000
  return created >= oneHourAgo
}

/** 撤回消息 */
async function handleRecall(messageId: number) {
  try {
    await recallMessage(messageId)
    chatStore.updateMessageRecall(messageId)
    ElMessage.success('已撤回')
    // 重新拉取历史，确保与后端一致（后端会返回 deleted: true）
    historyPage.value = 1
    noMoreHistory.value = false
    const res = await getChatHistory(otherUserId.value, 1, 30)
    const loaded = (res.data.data || []).reverse()
    const myIdNum = myId.value
    const otherId = otherUserId.value
    const belong = (m: { senderId: number; receiverId: number }) =>
      (m.senderId === myIdNum && m.receiverId === otherId) ||
      (m.senderId === otherId && m.receiverId === myIdNum)
    chatStore.setMessagesMergedWithHistory(loaded, m => belong(m), [])
    nextTick(scrollToBottom)
  } catch {
    // 错误已由 request 拦截器展示
  }
}

/** 切换「允许对方使用破冰」 */
async function handleIceBreakAllowChange(allowed: boolean) {
  if (iceBreakAllowLoading.value) return
  iceBreakAllowLoading.value = true
  try {
    await updateIceBreakAllow(otherUserId.value, allowed)
    if (iceBreakStatus.value) {
      iceBreakStatus.value = { ...iceBreakStatus.value, allowedByMe: allowed }
    }
    ElMessage.success(allowed ? '已允许对方使用破冰' : '已关闭')
  } catch {
    // 错误已由 request 拦截器展示
  } finally {
    iceBreakAllowLoading.value = false
  }
}

/** 解析邀约消息 content：邀约邀请：标题｜时间 xx:xx｜INVITE#id */
function parsedInvite(content: string): { title: string; timeStr: string; inviteId: number } | null {
  const match = content.match(/INVITE#(\d+)/)
  if (!match || !match[1]) return null
  const inviteId = parseInt(match[1], 10)
  const parts = content.split(/[｜|]/)
  let title = '邀约邀请'
  let timeStr = ''
  if (parts[0]) title = parts[0].replace(/^邀约邀请：?/, '').trim() || title
  if (parts[1]) timeStr = parts[1].replace(/^时间\s*/, '').trim()
  return { title, timeStr, inviteId }
}

const inviteActionState = ref<Record<number, 'accepted' | 'declined'>>({})

async function handleAcceptInvite(content: string) {
  const parsed = parsedInvite(content)
  if (!parsed) return
  try {
    await joinInvite(parsed.inviteId)
    inviteActionState.value[parsed.inviteId] = 'accepted'
    ElMessage.success('已接受邀约')
    router.push(`/invite/${parsed.inviteId}`)
  } catch (e: any) {
    const msg = e?.response?.data?.message ?? e?.message ?? '操作失败'
    ElMessage.error(msg)
  }
}

async function handleDeclineInvite(content: string) {
  const parsed = parsedInvite(content)
  if (!parsed) return
  try {
    await declineInvite(parsed.inviteId)
    inviteActionState.value[parsed.inviteId] = 'declined'
    ElMessage.success('已拒绝')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function goToInvite(content: string) {
  const parsed = parsedInvite(content)
  if (parsed) {
    router.push(`/invite/${parsed.inviteId}`)
  } else {
    ElMessage.warning('未找到邀约信息')
  }
}

function imageUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}

function triggerImageInput() {
  imageInputRef.value?.click()
}

function insertEmoji(emoji: string) {
  const el = inputRef.value?.$el?.querySelector('input, textarea') as HTMLInputElement | null
  if (el) {
    const start = el.selectionStart ?? inputText.value.length
    const end = el.selectionEnd ?? inputText.value.length
    const before = inputText.value.slice(0, start)
    const after = inputText.value.slice(end)
    inputText.value = before + emoji + after
    nextTick(() => {
      el.focus()
      const pos = before.length + emoji.length
      el.setSelectionRange(pos, pos)
    })
  } else {
    inputText.value += emoji
  }
}

async function handleMediaSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !canSend.value) return
  input.value = ''
  if (file.type.startsWith('video/')) {
    ElMessage.info('聊天暂不支持发送视频，请使用图片')
    return
  }
  try {
    const res = await uploadChatImage(file)
    const imageUrlVal = res.data.data
    if (!imageUrlVal) throw new Error('上传失败')
    const now = formatLocalDateTime()
    chatStore.pushOptimisticMessage(
      {
        id: -Date.now(),
        senderId: myId.value!,
        receiverId: otherUserId.value,
        senderNickname: userStore.user?.nickname ?? '',
        senderAvatar: userStore.user?.avatarUrl ?? null,
        content: imageUrlVal,
        msgType: 3,
        isRead: false,
        createdAt: now,
      },
      { pendingOtherUserId: otherUserId.value }
    )
    chatStore.sendMessage(otherUserId.value, imageUrlVal, 3)
    notifyDismissStore.undismissChat(otherUserId.value)
    nextTick(scrollToBottom)
    const canRes = await canSendTo(otherUserId.value)
    canSend.value = canRes.data?.data ?? false
  } catch {
    ElMessage.error('图片上传失败')
  }
}
</script>

<style lang="scss" scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid $border-light;
  background: $bg-primary;
  flex-shrink: 0;

  .back-btn, .more-btn {
    width: 36px; height: 36px;
    display: flex; align-items: center; justify-content: center;
    border-radius: $radius-full;
    transition: background $transition-fast;
    &:hover { background: $bg-tertiary; }
  }

  .header-info { flex: 1; }
  .header-name { font-size: 15px; font-weight: 600; }
  .header-status { font-size: 12px; color: $success; }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.no-more-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 8px 0;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 75%;

  &.mine {
    align-self: flex-end;
    flex-direction: row-reverse;

    .message-bubble {
      background: rgba($primary, 0.06);
      border: 1px solid $primary;
      color: $text-primary;

      .msg-time { color: $text-muted; }
    }
  }
}

.msg-avatar { flex-shrink: 0; }
.clickable-avatar { cursor: pointer; }

// 邀约/帖子分享：独立卡片，无气泡
.message-card-wrapper {
  position: relative;
  max-width: 100%;

  .msg-time { font-size: 11px; color: $text-muted; display: block; text-align: right; margin-top: 4px; }
}

.message-bubble {
  position: relative;
  padding: 10px 14px;
  border-radius: $radius-lg;
  background: $bg-primary;
  border: 1px solid $border-light;
  max-width: 100%;

  .msg-content { font-size: 14px; line-height: 1.5; word-wrap: break-word; }
  .msg-content.msg-recalled { color: var(--el-text-color-placeholder); font-style: italic; }
  .msg-time { font-size: 11px; color: $text-muted; display: block; text-align: right; margin-top: 4px; }
}

.recall-btn-left {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-color-danger);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  opacity: 0.85;
  align-self: center;
}
.recall-btn-left:hover { opacity: 1; }

.invite-link-btn {
  margin-top: 4px;
  padding: 0;
  font-size: 12px;
}

.invite-card-in-chat {
  padding: 12px 14px;
  min-width: 160px;
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-md;

  .invite-card-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 6px;
  }
  .invite-card-meta {
    font-size: 12px;
    color: $text-muted;
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 8px;
  }
  .invite-card-btn {
    margin-right: 8px;
    margin-bottom: 4px;
    &.outline {
      background: transparent;
      border: 1px solid currentColor;
    }
  }
  .invite-card-done {
    font-size: 12px;
    color: $text-muted;
    &.declined { color: var(--el-text-color-secondary); }
  }
}

.invite-fallback .msg-content { margin-bottom: 4px; }
.invite-fallback-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin: 6px 0 0;
}

.hidden-file-input { display: none; }

.chat-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: $radius-md;
  cursor: pointer;
  display: block;
}

// 帖子分享卡片（独立展示，白底）
.post-share-card {
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  padding: 10px 12px;
  max-width: 240px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;

  &:hover {
    background: $bg-secondary;
    border-color: rgba($primary, 0.3);
  }

  .share-card-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;

    .share-label {
      font-size: 12px;
      color: $text-muted;
    }
  }

  .share-card-author {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .share-card-content {
    font-size: 13px;
    color: $text-secondary;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .share-card-images {
    display: flex;
    gap: 4px;
    margin-top: 8px;

    .share-card-img {
      width: 50px;
      height: 50px;
      object-fit: cover;
      border-radius: 4px;
    }
  }
}

// 破冰话题面板
.ice-break-panel {
  padding: 12px 16px;
  margin: 0 20px 8px;
  background: linear-gradient(135deg, rgba($primary, 0.08), rgba($primary, 0.04));
  border: 1px solid rgba($primary, 0.2);
  border-radius: $radius-md;
  flex-shrink: 0;

  .ice-break-panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 14px;
    font-weight: 600;
    color: $primary;
  }
  .ice-break-close {
    padding: 4px;
    color: $text-muted;
    background: transparent;
    border: none;
    cursor: pointer;
    border-radius: 4px;
    &:hover { color: $text-primary; }
  }
  .ice-break-loading {
    font-size: 13px;
    color: $text-muted;
  }
  .ice-break-analysis {
    font-size: 13px;
    color: $text-secondary;
    line-height: 1.6;
    margin-bottom: 12px;
    padding: 8px 0;
    border-bottom: 1px solid rgba($primary, 0.15);
  }
  .ice-break-topics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .ice-break-topic-btn {
    padding: 6px 12px;
    font-size: 13px;
    color: $primary;
    background: rgba($primary, 0.08);
    border: 1px solid rgba($primary, 0.2);
    border-radius: $radius-md;
    cursor: pointer;
    transition: background 0.2s, border-color 0.2s;
    &:hover {
      background: rgba($primary, 0.15);
      border-color: rgba($primary, 0.4);
    }
  }
}
.ice-break-panel-enter-active,
.ice-break-panel-leave-active { transition: opacity 0.2s, transform 0.2s; }
.ice-break-panel-enter-from,
.ice-break-panel-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.ice-break-btn {
  font-size: 18px !important;
  &.disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.ice-break-allow-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 12px;
  font-size: 13px;
  color: $text-secondary;

  .ice-break-allow-label {
    flex: 1;
  }
}

.chat-input-area {
  padding: 12px 20px;
  border-top: 1px solid $border-light;
  background: $bg-primary;
  flex-shrink: 0;

  .chat-limit-hint {
    font-size: 12px;
    color: var(--el-color-warning);
    margin-bottom: 8px;
  }

  .input-row.disabled {
    opacity: 0.6;
    pointer-events: none;
  }

  .input-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
  }
  .input-row :deep(.el-input) { flex: 1; min-width: 0; }

  .icon-btn {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-full;
    background: transparent;
    color: $text-secondary;
    transition: background $transition-fast, color $transition-fast;
    flex-shrink: 0;
    &:hover { background: $bg-tertiary; color: $primary; }
  }

  :deep(.el-input__wrapper) { border-radius: $radius-full; }
  :deep(.el-input-group__append) { padding: 0; background: none; border: none; box-shadow: none; }

  .send-btn {
    width: 44px; height: 44px;
    display: flex; align-items: center; justify-content: center;
    background: rgba($primary, 0.06);
    color: $primary;
    border: 1.5px solid $primary;
    border-radius: $radius-full;
    transition: opacity $transition-fast, background $transition-fast;
    cursor: pointer;

    &:hover:not(:disabled) { background: rgba($primary, 0.12); }
    &:disabled { opacity: 0.4; cursor: not-allowed; }
  }
}
</style>
