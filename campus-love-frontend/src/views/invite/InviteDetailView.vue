<template>
  <div class="invite-detail-page">
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="invite" class="detail-content">
      <!-- 顶部返回与操作 -->
      <div class="detail-header">
        <button class="btn-text" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="header-actions">
          <button v-if="isCreator && canCancel" class="btn-text danger" @click="handleCancel">
            取消邀约
          </button>
        </div>
      </div>

      <!-- 主内容卡片（信息 + 参与者 + 讨论 + 操作） -->
      <div class="invite-main-card">
        <!-- 顶部信息区 -->
        <div class="invite-card">
        <div class="invite-status-badge" :style="{ color: INVITE_STATUS_COLORS[invite.status as InviteStatus] }">
          {{ INVITE_STATUS_LABELS[invite.status as InviteStatus] }}
        </div>

        <div class="invite-type-badge" :style="{ background: getTypeColor(invite.inviteType) }">
          {{ INVITE_TYPE_LABELS[invite.inviteType as InviteType] }}
        </div>

        <h1 class="invite-title">{{ invite.title }}</h1>

        <div class="invite-meta">
          <div class="meta-row">
            <span class="meta-label">发起人:</span>
            <span class="meta-value">
              {{ invite.creator?.nickname || '未知' }}
              <span v-if="invite.creator?.creditScore !== null" class="meta-tag">
                信用分 {{ invite.creator!.creditScore }}
              </span>
            </span>
          </div>
          <div class="meta-row">
            <span class="meta-label">时间:</span>
            <span class="meta-value">{{ formatDateTime(invite.inviteTime) }}</span>
          </div>
          <div v-if="invite.location" class="meta-row">
            <span class="meta-label">地点:</span>
            <span class="meta-value">{{ invite.location }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">人数:</span>
            <span class="meta-value">
              {{ invite.participantCount }}/{{ invite.inviteMode === 'PRIVATE' ? 1 : (invite.maxParticipants || '不限') }}
            </span>
          </div>
        </div>

        <div v-if="invite.content" class="invite-content">
          {{ invite.content }}
        </div>

        <div v-if="invite.atmosphereTags" class="tags-display">
          <span
            v-for="tag in invite.atmosphereTags.split(',')"
            :key="tag"
            class="tag-item"
          >
            {{ getTagLabel(tag) }}
          </span>
        </div>

        <div v-if="invite.ratingCount > 0" class="rating-display">
          <div class="rating-item">
            <span class="rating-label">社交体验</span>
            <span class="rating-stars">⭐ {{ invite.socialRating?.toFixed(1) || '-' }}</span>
          </div>
          <div v-if="invite.orgRating" class="rating-item">
            <span class="rating-label">组织能力</span>
            <span class="rating-stars">⭐ {{ invite.orgRating.toFixed(1) }}</span>
          </div>
          <span class="rating-count">({{ invite.ratingCount }}人评价)</span>
        </div>
        </div>
        <!-- 参与者区（同一主卡片内） -->
        <div v-if="invite.inviteMode === 'PUBLIC'" class="participants-section">
          <div class="participants-header">
            <h3 class="section-title">参与者 ({{ invite.participantCount }})</h3>
          </div>
          <div v-if="invite.participants && invite.participants.length" class="participants-list">
            <!-- 发起人单独展示在列表最上方 -->
            <div class="participant-item">
              <img :src="invite.creator?.avatarUrl || defaultAvatar" class="avatar" width="36" height="36" />
              <span class="participant-name">
                {{ invite.creator?.nickname || '发起人' }}
                <span class="participant-tag">发起人</span>
                <span v-if="invite.creatorId === myId" class="participant-tag">我</span>
              </span>
              <span class="join-time">{{ formatDateTime(invite.inviteTime) }}</span>
            </div>

            <div
              v-for="p in invite.participants.filter(p => p.userId !== invite!.creatorId)"
              :key="p.userId"
              class="participant-item"
            >
              <img :src="p.avatarUrl || defaultAvatar" class="avatar" width="36" height="36" />
              <span class="participant-name">
                {{ p.nickname }}
                <span v-if="p.userId === myId" class="participant-tag">我</span>
              </span>
              <span class="join-time">{{ formatJoinTime(p.joinAt) }}</span>
            </div>
          </div>
          <div v-else class="empty-participants">
            暂无参与者，快来加入吧！
          </div>
        </div>

        <!-- 发起人：待处理的再次加入申请 -->
        <div v-if="isCreator && rejoinRequests.length" class="rejoin-requests-section">
          <div class="participants-header">
            <h3 class="section-title">待处理的再次加入申请</h3>
          </div>
          <div class="rejoin-list">
            <div
              v-for="r in rejoinRequests"
              :key="r.userId"
              class="rejoin-item"
            >
              <img :src="r.avatarUrl || defaultAvatar" class="avatar" width="36" height="36" alt="" />
              <div class="rejoin-info">
                <span class="rejoin-name">{{ r.nickname || '用户' }}</span>
                <span class="rejoin-time">{{ formatRequestTime(r.requestedAt) }}</span>
              </div>
              <div class="rejoin-actions">
                <button type="button" class="btn-text primary" @click="handleApproveRejoin(r.userId)">
                  同意
                </button>
                <button type="button" class="btn-text" @click="handleRejectRejoin(r.userId)">
                  拒绝
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 邀约讨论区（类似 X 的评论区） -->
        <div v-if="showChatPanel" class="invite-chat-section">
          <div class="invite-chat-header">
            <h3 class="section-title">邀约讨论</h3>
            <span class="chat-meta">仅发起人和参与者可见</span>
          </div>
          <div class="invite-chat-box">
            <div v-if="pinnedMessages.length" class="pinned-bar">
              <span class="pinned-label">置顶</span>
              <div class="pinned-list">
                <button
                  v-for="pm in pinnedMessages"
                  :key="pm.id"
                  class="pinned-chip"
                  type="button"
                  @click="scrollToMessage(pm.id)"
                >
                  <span class="pinned-text text-ellipsis">{{ pm.content }}</span>
                  <span class="pinned-time">{{ formatMsgTime(pm.createdAt) }}</span>
                </button>
              </div>
            </div>
            <div ref="chatListRef" class="invite-chat-list">
              <div
                v-for="msg in inviteChatMessages"
                :id="`invite-msg-${msg.id}`"
                :key="msg.id"
                class="comment-item"
              >
                <img
                  :src="msg.senderAvatar || defaultAvatar"
                  class="comment-avatar"
                  width="36"
                  height="36"
                  alt=""
                />
                <div class="comment-main">
                  <div class="comment-header">
                    <span class="comment-name">
                      {{ msg.senderNickname || '用户' }}
                      <span v-if="msg.senderId === myId" class="comment-tag">我</span>
                    </span>
                    <span class="comment-time">{{ formatMsgTime(msg.createdAt) }}</span>
                    <button v-if="isCreator" class="comment-pin" type="button" @click="togglePin(msg)">
                      {{ pinnedMessageIds.includes(msg.id) ? '取消置顶' : '置顶' }}
                    </button>
                  </div>
                  <template v-if="msg.msgType === 3">
                    <el-image
                      :src="inviteImageUrl(msg.content)"
                      :preview-src-list="[inviteImageUrl(msg.content)]"
                      fit="cover"
                      class="invite-chat-image"
                      preview-teleported
                    />
                  </template>
                  <template v-else>
                    <p class="comment-text">{{ msg.content }}</p>
                  </template>
                </div>
              </div>
              <div v-if="!inviteChatMessages.length" class="comments-empty">
                暂无讨论，发一条评论开启话题吧～
              </div>
            </div>
            <div class="invite-chat-input-wrap">
              <input
                ref="inviteImageInputRef"
                type="file"
                accept="image/png,image/jpeg,image/gif,image/webp"
                class="hidden-file-input"
                @change="handleInviteImageSelect"
              />
              <div class="invite-input-row">
                <button type="button" class="icon-btn" title="发送图片" @click="triggerInviteImageInput">
                  <el-icon :size="18"><Picture /></el-icon>
                </button>
                <EmojiPicker @insert="insertInviteEmoji" />
                <el-input
                  ref="chatInputRef"
                  v-model="chatInputText"
                  placeholder="发表你的想法…"
                  size="default"
                  @keyup.enter="sendInviteChat"
                >
                  <template #append>
                    <button class="send-btn" :disabled="!chatInputText.trim()" @click="sendInviteChat">
                      发送
                    </button>
                  </template>
                </el-input>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作条，仍在主卡片内部 -->
        <div v-if="showActions" class="action-buttons">
          <!-- 已结束且我参与过：展示评价按钮 -->
          <button
            v-if="invite.status === 'ENDED' && hasJoined"
            class="btn-primary"
            @click="showRatingDialog = true"
          >
            评价本次邀约
          </button>

          <!-- 进行中的报名/退出操作 -->
          <template v-else>
            <button
              v-if="canRequestRejoin"
              class="btn-primary"
              @click="handleRequestRejoin"
            >
              申请再次加入
            </button>
            <button
              v-else-if="canJoin"
              class="btn-primary"
              :disabled="invite.status !== 'RECRUITING'"
              @click="handleJoin"
            >
              {{ invite.status === 'RECRUITING' ? '加入邀约' : '已满员' }}
            </button>
            <button
              v-if="hasJoined"
              class="btn-outline danger"
              @click="handleLeave"
            >
              退出邀约
            </button>
          </template>
        </div>
      </div>

      <!-- Rating Dialog -->
      <el-dialog v-model="showRatingDialog" title="评价邀约" width="400px" :close-on-click-modal="false">
        <div class="rating-form">
          <div class="rating-item">
            <label>社交体验</label>
            <el-rate v-model="ratingForm.socialRating" :max="5" allow-half />
          </div>
          <div v-if="!isParticipant" class="rating-item">
            <label>组织能力</label>
            <el-rate v-model="ratingForm.orgRating" :max="5" allow-half />
          </div>
          <div class="rating-item">
            <label>评价内容</label>
            <el-input
              v-model="ratingForm.content"
              type="textarea"
              :rows="3"
              placeholder="说说你的体验..."
              maxlength="200"
              show-word-limit
            />
          </div>
        </div>
        <template #footer>
          <button class="btn-outline" @click="showRatingDialog = false">取消</button>
          <button class="btn-primary" @click="submitRating">提交评价</button>
        </template>
      </el-dialog>
    </div>

    <div v-else class="error-state">
      <div class="error-icon">😕</div>
      <p>邀约不存在或已被删除</p>
      <button class="btn-outline" @click="$router.push('/invite')">返回广场</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useInviteStore } from '@/store/inviteStore'
import { useChatStore } from '@/store/chatStore'
import { storeToRefs } from 'pinia'
import { formatLocalDateTime } from '@/utils/dateTime'
import {
  getInviteDetail,
  joinInvite,
  leaveInvite,
  cancelInvite,
  createRating,
  requestRejoin,
  getRejoinRequests,
  approveRejoin,
  rejectRejoin,
  type Invite,
  type InviteRatingCreateRequest,
  type InviteRejoinRequestItem,
} from '@/api/inviteApi'
import { getChatHistory, getGroupChatHistory, uploadChatImage } from '@/api/chatApi'
import type { ChatMessage } from '@/api/chatApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import {
  InviteType,
  InviteStatus,
  INVITE_TYPE_LABELS,
  INVITE_STATUS_LABELS,
  INVITE_STATUS_COLORS,
  ATMOSPHERE_TAGS,
} from '@/constants/inviteConst'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const inviteStore = useInviteStore()
const chatStore = useChatStore()
const { currentMessages: chatCurrentMessages } = storeToRefs(chatStore)

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="%23f0f2f5" width="40" height="40" rx="20"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="18">👤</text></svg>'

const invite = ref<Invite | null>(null)
const loading = ref(true)
const showRatingDialog = ref(false)

// 内嵌聊天
const chatListRef = ref<HTMLElement>()
const chatInputText = ref('')
const chatInputRef = ref<{ $el: HTMLElement } | null>(null)
const inviteImageInputRef = ref<HTMLInputElement | null>(null)
const loadedChatMessages = ref<ChatMessage[]>([])
/** 本页已发送的消息（仅当前会话），不依赖 store 响应式，确保发完必显 */
const invitePageSentMessages = ref<ChatMessage[]>([])
/** 置顶消息 id 列表（按邀约维度存入 localStorage） */
const pinnedMessageIds = ref<number[]>([])
const lastProcessedWsIndex = ref(0)
/** 发起人可见：待处理的再次加入申请列表 */
const rejoinRequests = ref<InviteRejoinRequestItem[]>([])

const ratingForm = ref<InviteRatingCreateRequest>({
  inviteId: 0,
  ratedUserId: 0,
  socialRating: 5,
  orgRating: 5,
  content: '',
})

// 是否是发起人
const isCreator = computed(() => {
  return invite.value?.creatorId === userStore.user?.id
})

// 是否已加入
const hasJoined = computed(() => {
  return invite.value?.participants?.some(p => p.userId === userStore.user?.id)
})

// 是否已退出（曾参与过且已退出）
const hasLeft = computed(() => invite.value?.myRole === 'LEFT')

// 是否可以加入（未加入且未退出的用户）
const canJoin = computed(() => {
  if (!invite.value) return false
  if (isCreator.value) return false
  if (hasJoined.value) return false
  if (hasLeft.value) return false
  if (invite.value.inviteMode === 'PRIVATE') {
    return invite.value.targetUserId === userStore.user?.id
  }
  return true
})

// 已退出用户是否可申请再次加入
const canRequestRejoin = computed(() => {
  if (!invite.value || !hasLeft.value) return false
  return invite.value.status === 'RECRUITING' || invite.value.status === 'FULL'
})

// 是否可以取消
const canCancel = computed(() => {
  return invite.value?.status === 'RECRUITING' ||
         invite.value?.status === 'FULL' ||
         invite.value?.status === 'CONFIRMED'
})

// 是否显示操作按钮
const showActions = computed(() => {
  if (isCreator.value) return false
  if (invite.value?.status === 'ENDED' && hasJoined.value) {
    // 显示评价按钮
    return true
  }
  return canJoin.value || hasJoined.value || canRequestRejoin.value
})

// 是否是参与者（非发起人）
const isParticipant = computed(() => {
  return hasJoined.value && !isCreator.value
})

// 是否展示邀约内嵌聊天（发起人或已加入的参与者）
const showChatPanel = computed(() => {
  if (!invite.value) return false
  return isCreator.value || hasJoined.value
})

const myId = computed(() => userStore.user?.id ?? 0)

// 只要有 chatGroupId 就使用群聊通道（公开/私密均可）
const isGroupChat = computed(() => {
  return (invite.value?.chatGroupId ?? 0) > 0
})

// 单聊时的对方用户 ID
const chatOtherUserId = computed(() => {
  if (!invite.value) return 0
  return isCreator.value ? (invite.value.targetUserId ?? 0) : invite.value.creatorId
})

// 合并：接口历史 + 本页已发（invitePageSentMessages），仅按 id 去重（允许相同内容多次出现）
const inviteChatMessages = computed(() => {
  const loaded = loadedChatMessages.value
  const local = invitePageSentMessages.value
  const combined: ChatMessage[] = [...loaded]
  const existingIds = new Set(loaded.map(m => m.id))

  for (const msg of local) {
    if (typeof msg.id === 'number' && msg.id > 0 && existingIds.has(msg.id)) continue
    combined.push(msg)
  }

  combined.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
  return combined
})

// 已置顶的消息列表（按当前 inviteChatMessages 过滤）
const pinnedMessages = computed(() => {
  const ids = new Set(pinnedMessageIds.value)
  return inviteChatMessages.value.filter(m => ids.has(m.id))
})

// 获取类型颜色
function getTypeColor(type: string): string {
  const colors: Record<string, string> = {
    DINNER: '#ff6b9d',
    SPORT: '#52c41a',
    STUDY: '#1890ff',
    DRAMA: '#722ed1',
    OTHER: '#8c8c8c',
  }
  return colors[type] || '#8c8c8c'
}

// 获取标签名称
function getTagLabel(value: string): string {
  const tag = ATMOSPHERE_TAGS.find(t => t.value === value)
  return tag?.label || value
}

// 格式化日期时间
function formatDateTime(timeStr: string): string {
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    weekday: 'short',
  })
}

// 格式化加入时间
function formatJoinTime(timeStr: string): string {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diff < 60) return '刚刚加入'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return `${Math.floor(diff / 86400)}天前加入`
}

// 格式化申请时间
function formatRequestTime(timeStr: string): string {
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 加入邀约
async function handleJoin() {
  if (!invite.value) return

  try {
    await ElMessageBox.confirm('确定要加入这个邀约吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await joinInvite(invite.value.id)
    ElMessage.success('加入成功')
    await loadInvite()
    inviteStore.incrementParticipantCount(invite.value.id)
  } catch {
    // 用户取消或错误
  }
}

// 申请再次加入（已退出用户）
async function handleRequestRejoin() {
  if (!invite.value) return
  try {
    await requestRejoin(invite.value.id)
    ElMessage.success('已提交申请，等待发起人同意')
    await loadInvite()
  } catch {
    // 错误由拦截器处理
  }
}

// 发起人：加载待处理的再次加入申请
async function loadRejoinRequests() {
  if (!invite.value?.id || !isCreator.value) return
  try {
    const res = await getRejoinRequests(invite.value.id)
    rejoinRequests.value = res.data.data ?? []
  } catch {
    rejoinRequests.value = []
  }
}

// 发起人：同意再次加入
async function handleApproveRejoin(userId: number) {
  if (!invite.value) return
  try {
    await ElMessageBox.confirm('确定同意该用户再次加入邀约吗？', '确认', {
      confirmButtonText: '同意',
      cancelButtonText: '取消',
    })
    await approveRejoin(invite.value.id, userId)
    ElMessage.success('已同意，对方已重新加入邀约')
    await loadInvite()
    await loadRejoinRequests()
    inviteStore.incrementParticipantCount(invite.value.id)
  } catch {
    // 用户取消或错误
  }
}

// 发起人：拒绝再次加入
async function handleRejectRejoin(userId: number) {
  if (!invite.value) return
  try {
    await rejectRejoin(invite.value.id, userId)
    ElMessage.success('已拒绝')
    await loadRejoinRequests()
  } catch {
    // 错误由拦截器处理
  }
}

// 退出邀约
async function handleLeave() {
  if (!invite.value) return

  try {
    await ElMessageBox.confirm('确定要退出这个邀约吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await leaveInvite(invite.value.id)
    ElMessage.success('已退出邀约')
    // 重新加载详情
    await loadInvite()
    // 更新store中的参与人数
    inviteStore.decrementParticipantCount(invite.value.id)
  } catch {
    // 用户取消或错误
  }
}

// 取消邀约
async function handleCancel() {
  if (!invite.value) return

  try {
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消邀约', {
      confirmButtonText: '确定',
      cancelButtonText: '暂不取消',
      inputPlaceholder: '可选',
    })
    await cancelInvite(invite.value.id, reason || undefined)
    ElMessage.success('邀约已取消')
    await loadInvite()
  } catch {
    // 用户取消或错误
  }
}

// 提交评价
async function submitRating() {
  if (!invite.value) return

  ratingForm.value.inviteId = invite.value.id
  ratingForm.value.ratedUserId = invite.value.creatorId

  try {
    await createRating(ratingForm.value)
    ElMessage.success('评价成功')
    showRatingDialog.value = false
    await loadInvite()
  } catch {
    // Error handled by interceptor
  }
}

// 加载邀约详情
async function loadInvite() {
  const inviteId = Number(route.params.id)
  if (!inviteId) {
    router.push('/invite')
    return
  }

  loading.value = true
  try {
    const res = await getInviteDetail(inviteId)
    invite.value = res.data.data
    // 加载本邀约的置顶消息 id（仅本地存储）
    try {
      const rawPins = localStorage.getItem(`invite_pins_${inviteId}`)
      pinnedMessageIds.value = rawPins ? JSON.parse(rawPins) : []
    } catch {
      pinnedMessageIds.value = []
    }
    if (invite.value && (isCreator.value || hasJoined.value)) {
      await loadInviteChat()
    }
    if (invite.value && invite.value.creatorId === userStore.user?.id) {
      await loadRejoinRequests()
    }
  } catch (error) {
    invite.value = null
  } finally {
    loading.value = false
  }
}

function mergeHistoryWithPending(loaded: ChatMessage[], pending: ChatMessage[]): ChatMessage[] {
  // 仅按 id 去重：同一条消息（相同 id）保留一条；允许内容完全相同的多条消息并存
  const byId = new Map<number | string, ChatMessage>()
  for (const m of loaded) byId.set(m.id, m)
  for (const p of pending) {
    if (!byId.has(p.id)) byId.set(p.id, p)
  }
  const out = [...byId.values()]
  out.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
  return out
}

async function loadInviteChat() {
  const inv = invite.value
  if (!inv || !myId.value) return
  chatStore.connectWebSocket()
  try {
    if (isGroupChat.value && inv.chatGroupId) {
      const res = await getGroupChatHistory(inv.chatGroupId, 1, 50)
      loadedChatMessages.value = (res.data.data ?? []).reverse()
    } else if (chatOtherUserId.value) {
      const res = await getChatHistory(chatOtherUserId.value, 1, 50)
      const loaded = (res.data.data ?? []).reverse()
      const pending = chatStore.getPendingForUser(chatOtherUserId.value)
      loadedChatMessages.value = mergeHistoryWithPending(loaded, pending)
    } else {
      loadedChatMessages.value = []
    }
    lastProcessedWsIndex.value = chatCurrentMessages.value?.length ?? 0
    nextTick(scrollChatToBottom)
  } catch {
    loadedChatMessages.value = []
  }
}

function scrollChatToBottom() {
  if (chatListRef.value) {
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  }
}

function formatMsgTime(createdAt: string) {
  if (!createdAt) return ''
  // 直接从字符串截取时间部分，避免浏览器对时区的不同解析导致偏移
  // 期望格式：yyyy-MM-dd HH:mm:ss
  const parts = createdAt.split(' ')
  if (parts.length < 2) return createdAt
  const timePart = parts[1] || ''
  const [h, m] = timePart.split(':')
  if (!h || !m) return createdAt
  return `${h}:${m}`
}

function inviteImageUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return url.startsWith('/') ? url : '/' + url
}

function triggerInviteImageInput() {
  inviteImageInputRef.value?.click()
}

function insertInviteEmoji(emoji: string) {
  const el = chatInputRef.value?.$el?.querySelector('input, textarea') as HTMLInputElement | null
  if (el) {
    const start = el.selectionStart ?? chatInputText.value.length
    const end = el.selectionEnd ?? chatInputText.value.length
    const before = chatInputText.value.slice(0, start)
    const after = chatInputText.value.slice(end)
    chatInputText.value = before + emoji + after
    nextTick(() => {
      el.focus()
      const pos = before.length + emoji.length
      el.setSelectionRange(pos, pos)
    })
  } else {
    chatInputText.value += emoji
  }
}

async function handleInviteImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !invite.value) return
  input.value = ''
  if (!chatStore.connected) {
    ElMessage.warning('聊天服务连接中，请稍后再试')
    return
  }
  try {
    const res = await uploadChatImage(file)
    const imageUrlVal = res.data.data
    if (!imageUrlVal) throw new Error('上传失败')
    const now = formatLocalDateTime()
    const optimistic: ChatMessage = {
      id: -Date.now(),
      senderId: myId.value,
      receiverId: isGroupChat.value ? 0 : chatOtherUserId.value,
      groupId: isGroupChat.value ? invite.value.chatGroupId ?? undefined : undefined,
      senderNickname: userStore.user?.nickname ?? '',
      senderAvatar: userStore.user?.avatarUrl ?? null,
      content: imageUrlVal,
      msgType: 3,
      isRead: false,
      createdAt: now,
    }
    invitePageSentMessages.value.push(optimistic)
    chatStore.pushOptimisticMessage(
      optimistic,
      !isGroupChat.value && chatOtherUserId.value ? { pendingOtherUserId: chatOtherUserId.value } : undefined
    )
    if (isGroupChat.value && invite.value.chatGroupId) {
      chatStore.sendGroupMessage(invite.value.chatGroupId, imageUrlVal, 3)
    } else if (chatOtherUserId.value) {
      chatStore.sendMessage(chatOtherUserId.value, imageUrlVal, 3)
    }
    nextTick(scrollChatToBottom)
  } catch {
    ElMessage.error('图片上传失败')
  }
}

function sendInviteChat() {
  const text = chatInputText.value.trim()
  if (!text || !invite.value) return
  // 若 WebSocket 未连接，直接提示用户，避免“前端显示过、刷新就消失”这种假发送
  if (!chatStore.connected) {
    ElMessage.warning('聊天服务连接中，请稍后再试')
    return
  }
  const now = formatLocalDateTime()
  const optimistic: ChatMessage = {
    id: -Date.now(),
    senderId: myId.value,
    receiverId: isGroupChat.value ? 0 : chatOtherUserId.value,
    groupId: isGroupChat.value ? invite.value.chatGroupId ?? undefined : undefined,
    senderNickname: userStore.user?.nickname ?? '',
    senderAvatar: userStore.user?.avatarUrl ?? null,
    content: text,
    msgType: 1,
    isRead: false,
    createdAt: now,
  }
  invitePageSentMessages.value.push(optimistic)
  chatStore.pushOptimisticMessage(
    optimistic,
    !isGroupChat.value && chatOtherUserId.value ? { pendingOtherUserId: chatOtherUserId.value } : undefined
  )
  if (isGroupChat.value && invite.value.chatGroupId) {
    chatStore.sendGroupMessage(invite.value.chatGroupId, text)
  } else if (chatOtherUserId.value) {
    chatStore.sendMessage(chatOtherUserId.value, text)
  }
  chatInputText.value = ''
  nextTick(scrollChatToBottom)
}

// WebSocket 回显：把本页乐观消息替换掉，避免必须"退出再进"才看到正确时间/顺序
watch(
  chatCurrentMessages,
  (all) => {
    if (!invite.value || !Array.isArray(all)) return
    const inv = invite.value
    for (let i = lastProcessedWsIndex.value; i < all.length; i++) {
      const msg = all[i]
      if (!msg) continue
      if (typeof msg.id === 'number' && msg.id < 0) continue

      const belongs = isGroupChat.value
        ? (inv.chatGroupId != null && msg.groupId === inv.chatGroupId)
        : (chatOtherUserId.value
          ? (
            (msg.senderId === myId.value && msg.receiverId === chatOtherUserId.value) ||
            (msg.senderId === chatOtherUserId.value && msg.receiverId === myId.value)
          )
          : false)
      if (!belongs) continue

      invitePageSentMessages.value = invitePageSentMessages.value.filter(m =>
        !(typeof m.id === 'number' && m.id < 0 && m.senderId === msg.senderId && m.content === msg.content)
      )

      if (!loadedChatMessages.value.some(m => m.id === msg.id)) {
        const next = [...loadedChatMessages.value, msg].sort(
          (a, b) => (a.createdAt || '').localeCompare(b.createdAt || '')
        )
        loadedChatMessages.value = next
      }
    }
    lastProcessedWsIndex.value = all.length
  },
  { deep: true }
)

function togglePin(msg: ChatMessage) {
  if (!invite.value || !isCreator.value) return
  if (typeof msg.id !== 'number' || msg.id <= 0) {
    ElMessage.info('请等待消息发送成功后再置顶')
    return
  }
  const idx = pinnedMessageIds.value.indexOf(msg.id)
  if (idx >= 0) {
    pinnedMessageIds.value.splice(idx, 1)
  } else {
    pinnedMessageIds.value.unshift(msg.id)
  }
  localStorage.setItem(`invite_pins_${invite.value.id}`, JSON.stringify(pinnedMessageIds.value))
}

function scrollToMessage(msgId: number) {
  const el = document.getElementById(`invite-msg-${msgId}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

watch(inviteChatMessages, () => nextTick(scrollChatToBottom), { deep: true })

onMounted(loadInvite)
</script>

<style lang="scss" scoped>
.invite-detail-page { padding: 20px; max-width: 600px; margin: 0 auto; }

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: $text-muted;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid $border-light;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-icon { font-size: 48px; margin-bottom: 16px; }

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.invite-main-card {
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 20px 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.invite-card {
  position: relative;
}

.invite-status-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 14px;
  font-weight: 700;
}

.invite-type-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 4px 12px;
  color: white;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
}

.invite-title {
  margin: 40px 0 16px;
  font-size: 24px;
  font-weight: 800;
  color: $text-primary;
  line-height: 1.4;
}

.invite-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-light;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.meta-label { color: $text-muted; }
.meta-value { color: $text-primary; font-weight: 500; }

.meta-tag {
  margin-left: 8px;
  font-size: 12px;
  color: $primary;
}

.invite-content {
  font-size: 15px;
  line-height: 1.6;
  color: $text-primary;
  margin-bottom: 16px;
}

.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-item {
  padding: 4px 12px;
  background: rgba($primary, 0.08);
  color: $primary;
  border-radius: $radius-full;
  font-size: 13px;
  font-weight: 500;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: rgba($primary, 0.05);
  border-radius: $radius-md;
}

.rating-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-label { font-size: 13px; color: $text-secondary; }
.rating-stars { font-size: 14px; font-weight: 600; color: $primary; }
.rating-count { margin-left: auto; font-size: 12px; color: $text-muted; }

.participants-section {
  padding-top: 4px;
  border-top: 1px solid $border-light;
}

.participants-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
  color: $text-primary;
}

.participants-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.participant-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.participant-tag {
  margin-left: 4px;
  padding: 0 6px;
  font-size: 11px;
  border-radius: $radius-full;
  background: rgba($primary, 0.06);
  color: $primary;
  font-weight: 500;
}

.join-time {
  font-size: 12px;
  color: $text-muted;
}

.empty-participants {
  text-align: center;
  padding: 20px;
  color: $text-muted;
  font-size: 14px;
}

.rejoin-requests-section {
  padding-top: 12px;
  border-top: 1px solid $border-light;
}

.rejoin-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rejoin-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: rgba($primary, 0.04);
  border-radius: $radius-md;
}

.rejoin-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rejoin-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.rejoin-time {
  font-size: 12px;
  color: $text-muted;
}

.rejoin-actions {
  display: flex;
  gap: 8px;
  .btn-text.primary { color: $primary; font-weight: 600; }
}

.invite-chat-section {
  padding-top: 4px;
  border-top: 1px solid $border-light;
}

.invite-chat-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;

  .chat-meta {
    font-size: 12px;
    color: $text-muted;
  }
}

.invite-chat-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.invite-chat-list {
  min-height: 180px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 0;
  border-top: 1px solid $border-light;
}

.pinned-bar {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0 4px;
}

.pinned-label {
  font-size: 12px;
  color: $text-muted;
}

.pinned-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.pinned-chip {
  max-width: 100%;
  border: 1px solid $border-light;
  background: $bg-secondary;
  border-radius: 999px;
  padding: 2px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-secondary;
  cursor: pointer;
  transition: background $transition-fast, border-color $transition-fast;

  &:hover {
    background: $bg-tertiary;
    border-color: rgba($primary, 0.4);
  }
}

.pinned-text {
  max-width: 140px;
}

.pinned-time {
  font-size: 11px;
  color: $text-muted;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  padding: 10px 0;
  gap: 10px;
  border-bottom: 1px solid rgba($border-light, 0.7);

  &:last-of-type {
    border-bottom: none;
  }
}

.comment-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: $radius-full;
  object-fit: cover;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 2px;
}

.comment-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;

  .comment-tag {
    margin-left: 4px;
    padding: 0 6px;
    font-size: 11px;
    border-radius: $radius-full;
    background: rgba($primary, 0.06);
    color: $primary;
    font-weight: 500;
  }
}

.comment-time {
  margin-left: auto;
  font-size: 12px;
  color: $text-muted;
}

.comment-pin {
  margin-left: 8px;
  padding: 0 6px;
  font-size: 11px;
  border-radius: $radius-full;
  border: none;
  background: transparent;
  color: $text-muted;
  cursor: pointer;
  transition: color $transition-fast, background $transition-fast;

  &:hover {
    color: $primary;
    background: rgba($primary, 0.06);
  }
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: $text-primary;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.comments-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
  color: $text-muted;
}

.hidden-file-input { display: none; }

.invite-chat-image {
  max-width: 180px;
  max-height: 180px;
  border-radius: $radius-md;
  cursor: pointer;
  display: block;
  margin-top: 4px;
}

.invite-chat-input-wrap {
  padding-top: 6px;
  border-top: 1px solid $border-light;

  .invite-input-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .icon-btn {
    width: 36px;
    height: 36px;
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

  .invite-input-row :deep(.el-input) { flex: 1; min-width: 0; }

  :deep(.el-input__wrapper) {
    border-radius: 999px;
    padding-left: 14px;
    background-color: $bg-primary;
  }

  :deep(.el-input-group__append) {
    padding: 0;
    background: none;
    border: none;
    box-shadow: none;
  }

  .send-btn {
    padding: 0 18px;
    height: 32px;
    background: $primary;
    color: white;
    border: none;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: opacity $transition-fast, transform $transition-fast;

    &:hover:not(:disabled) {
      opacity: 0.9;
      transform: translateY(-0.5px);
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.btn-primary {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-text {
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition: color $transition-fast;

  &.danger:hover { color: $danger; }
  &:hover:not(.danger) { color: $primary; }
}

.rating-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rating-item {
  display: flex;
  flex-direction: column;
  gap: 8px;

  label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }
}
</style>
