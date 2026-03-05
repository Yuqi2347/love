<template>
  <div class="invite-detail-page">
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="invite" class="detail-content">
      <!-- Header -->
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

      <!-- Invite Card -->
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

        <div class="rating-display" v-if="invite.ratingCount > 0">
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

      <!-- Participants -->
      <div v-if="invite.inviteMode === 'PUBLIC'" class="participants-section">
        <h3 class="section-title">参与者 ({{ invite.participantCount }})</h3>
        <div v-if="invite.participants && invite.participants.length" class="participants-list">
          <div
            v-for="p in invite.participants"
            :key="p.userId"
            class="participant-item"
          >
            <img :src="p.avatarUrl || defaultAvatar" class="avatar" width="36" height="36" />
            <span class="participant-name">{{ p.nickname }}</span>
            <span class="join-time">{{ formatJoinTime(p.joinAt) }}</span>
          </div>
        </div>
        <div v-else class="empty-participants">
          暂无参与者，快来加入吧！
        </div>
      </div>

      <!-- Group chat entry for public invites -->
      <div v-if="invite.inviteMode === 'PUBLIC' && invite.chatGroupId" class="group-chat-entry">
        <button class="btn-outline" @click="$router.push(`/chat/group/${invite.chatGroupId}`)">
          进入群聊
        </button>
      </div>

      <!-- Actions -->
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
            v-if="canJoin"
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useInviteStore } from '@/store/inviteStore'
import {
  getInviteDetail,
  joinInvite,
  leaveInvite,
  cancelInvite,
  createRating,
  type Invite,
  type InviteRatingCreateRequest,
} from '@/api/inviteApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
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

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="%23f0f2f5" width="40" height="40" rx="20"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="18">👤</text></svg>'

const invite = ref<Invite | null>(null)
const loading = ref(true)
const showRatingDialog = ref(false)

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

// 是否可以加入
const canJoin = computed(() => {
  if (!invite.value) return false
  if (isCreator.value) return false
  if (hasJoined.value) return false
  if (invite.value.inviteMode === 'PRIVATE') {
    return invite.value.targetUserId === userStore.user?.id
  }
  return true
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
  return canJoin.value || hasJoined.value
})

// 是否是参与者（非发起人）
const isParticipant = computed(() => {
  return hasJoined.value && !isCreator.value
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
    // 重新加载详情
    await loadInvite()
    // 更新store中的参与人数
    inviteStore.incrementParticipantCount(invite.value.id)
  } catch {
    // 用户取消或错误
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
  } catch (error) {
    invite.value = null
  } finally {
    loading.value = false
  }
}

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

.invite-card {
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 20px;
  margin-bottom: 20px;
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
  background: $bg-primary;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 16px;
  margin-bottom: 20px;
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
