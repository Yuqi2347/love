<template>
  <div class="discover-page">
    <div class="page-header">
      <h2 class="page-title">发现</h2>
      <div class="header-right">
        <!-- 始终显示等级信息 -->
        <div v-if="levelInfo" class="level-info">
          <span class="level-badge">Lv{{ levelInfo.level }}</span>
          <span class="level-progress">{{ levelInfo.score }}/{{ levelInfo.scoreToNext }}</span>
        </div>
        <!-- 可发布时显示发布按钮 -->
        <button v-if="canPost" class="btn-primary post-btn" @click="showPostDialog = true">
          <el-icon><Plus /></el-icon> 发布
        </button>
      </div>
    </div>

    <!-- 统一信息流：邀约 + 动态 -->
    <div v-if="timelineItems.length" class="timeline-list">
      <div
        v-for="item in timelineItems"
        :key="item.key"
        class="timeline-item"
      >
        <!-- 邀约信息：粉色卡片 -->
        <div
          v-if="item.kind === 'invite'"
          class="invite-card"
          @click="$router.push(`/invite/${item.invite.id}`)"
        >
          <div class="invite-header">
            <div class="invite-type-badge" :style="{ background: getTypeColor(item.invite.inviteType) }">
              {{ INVITE_TYPE_LABELS[item.invite.inviteType as InviteType] }}
            </div>
            <div class="invite-status-badge" :style="{ color: INVITE_STATUS_COLORS[item.invite.status as InviteStatus] }">
              {{ INVITE_STATUS_LABELS[item.invite.status as InviteStatus] }}
            </div>
          </div>
          <h3 class="invite-title">{{ item.invite.title }}</h3>
          <p v-if="item.invite.content" class="invite-content">{{ item.invite.content }}</p>
          <div class="invite-meta">
            <span class="meta-item">
              {{ item.invite.creator?.nickname || '未知' }}
            </span>
            <span class="meta-item">
              {{ formatInviteTime(item.invite.inviteTime) }}
            </span>
            <span v-if="item.invite.location" class="meta-item">
              {{ item.invite.location }}
            </span>
          </div>
          <div class="invite-footer">
            <span class="participants">
              {{ item.invite.participantCount }}/{{ item.invite.inviteMode === 'PRIVATE' ? 1 : (item.invite.maxParticipants || '不限') }}人
            </span>
            <span v-if="item.invite.isUrgent" class="urgent-tag">急需</span>
            <span v-if="item.invite.ratingCount" class="rating-info">
              ⭐ {{ item.invite.socialRating?.toFixed(1) || '-' }} ({{ item.invite.ratingCount }})
            </span>
          </div>
        </div>

        <!-- 社交动态：白色卡片 -->
        <div v-else class="feed-card card">
          <div class="feed-header">
            <img
              :src="item.post.avatarUrl || defaultAvatar"
              class="feed-avatar"
              @click="$router.push(`/profile/${item.post.userId}`)"
            />
            <div class="feed-user" @click="$router.push(`/profile/${item.post.userId}`)">
              <div class="feed-name">{{ item.post.nickname }}</div>
              <div class="feed-time">{{ formatTime(item.post.createdAt) }}</div>
            </div>
          </div>
          <div class="feed-content">{{ item.post.content }}</div>
          <div v-if="item.post.images" class="feed-images">
            <img
              v-for="(img, idx) in item.post.images.split(',').slice(0, 3)"
              :key="idx"
              :src="img"
              class="feed-image"
            />
          </div>
          <div class="feed-actions">
            <button
              :class="['action-btn', { active: item.post.liked }]"
              @click="handleLike(item.post.id, item.post.liked)"
            >
              <span class="action-icon">{{ item.post.liked ? '❤️' : '🤍' }}</span>
              <span>{{ item.post.likeCount }}</span>
            </button>
            <button class="action-btn" @click="openComment(item.post)">
              <span class="action-icon">💬</span>
              <span>{{ item.post.commentCount }}</span>
            </button>
            <button
              v-if="canDeletePost(item.post)"
              class="action-btn delete-btn"
              @click="handleDeletePost(item.post.id)"
            >
              <span class="action-icon">🗑️</span>
            </button>
          </div>

          <!-- 评论叠楼列表 -->
          <div v-if="item.post.comments && item.post.comments.length" class="comment-list">
            <div
              v-for="c in item.post.comments"
              :key="c.id"
              class="comment-item"
            >
              <div class="comment-header">
                <span
                  class="comment-author"
                  @click="$router.push(`/profile/${c.userId}`)"
                >
                  {{ c.nickname }}
                </span>
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
              </div>
              <div class="comment-text">
                {{ c.content }}
              </div>
            </div>
          </div>

          <div v-if="commentingPostId === item.post.id" class="comment-input">
            <el-input
              v-model="commentText"
              placeholder="写评论..."
              size="small"
              @keyup.enter="submitComment(item.post.id)"
            >
              <template #append>
                <button class="comment-send" @click="submitComment(item.post.id)">发送</button>
              </template>
            </el-input>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布动态弹窗 -->
    <el-dialog v-model="showPostDialog" title="发布动态" width="500px">
      <el-form @submit.prevent="handlePost">
        <el-form-item>
          <el-input
            v-model="postContent"
            type="textarea"
            :rows="4"
            placeholder="分享你的想法..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <div v-if="levelInfo && levelInfo.level < 3" class="post-tip">
          当前等级 Lv{{ levelInfo.level }}，升级到 Lv3 可发布动态
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showPostDialog = false">取消</el-button>
        <el-button type="primary" :disabled="posting || !postContent.trim()" @click="handlePost">
          {{ posting ? '发布中...' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <div v-if="!timelineItems.length" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无动态</p>
      <p v-if="canPost" class="empty-hint">成为第一个发布动态的人吧！</p>
      <p v-else class="empty-hint">达到 Lv3 等级即可发布动态</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getDiscoveryPosts,
  likePost,
  unlikePost,
  getLevelInfo,
  createDiscoveryPost,
  deletePost,
  addComment,
  type FeedPost,
  type UserLevelInfo,
} from '@/api/feedApi'
import { useUserStore } from '@/store/userStore'
import { useInviteStore } from '@/store/inviteStore'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Invite } from '@/api/inviteApi'
import {
  InviteType,
  InviteStatus,
  INVITE_TYPE_LABELS,
  INVITE_STATUS_LABELS,
  INVITE_STATUS_COLORS,
  formatInviteTime,
} from '@/constants/inviteConst'

const userStore = useUserStore()
const inviteStore = useInviteStore()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const posts = ref<FeedPost[]>([])
const levelInfo = ref<UserLevelInfo | null>(null)
const showPostDialog = ref(false)
const postContent = ref('')
const posting = ref(false)
const commentingPostId = ref<number | null>(null)
const commentText = ref('')

// 管理员或Lv3及以上可以发布
const canPost = computed(() => {
  const isAdmin = userStore.user?.isAdmin || false
  const level = levelInfo.value?.level || 0
  return isAdmin || level >= 3
})

// 判断是否可以删除帖子（管理员或帖子作者）
function canDeletePost(post: FeedPost): boolean {
  const isAdmin = userStore.user?.isAdmin || false
  const isOwner = post.userId === userStore.user?.id
  return isAdmin || isOwner
}

onMounted(async () => {
  await Promise.all([loadPosts(), loadLevelInfo(), loadInvites()])
})

type TimelineItem =
  | { kind: 'invite'; invite: Invite; time: string; key: string }
  | { kind: 'post'; post: FeedPost; time: string; key: string }

const timelineItems = computed<TimelineItem[]>(() => {
  const inviteItems: TimelineItem[] = inviteStore.invites
    // 只展示公开且进行中的邀约：过滤掉私密、一对一、已取消和已结束
    .filter(invite =>
      invite.inviteMode === InviteMode.PUBLIC &&
      invite.status !== InviteStatus.CANCELLED &&
      invite.status !== InviteStatus.ENDED
    )
    .map(invite => {
      const time = invite.createdAt || invite.inviteTime
      return {
        kind: 'invite',
        invite,
        time: time || '',
        key: `invite-${invite.id}`,
      }
    })

  const postItems: TimelineItem[] = posts.value.map(post => ({
    kind: 'post',
    post,
    time: post.createdAt,
    key: `post-${post.id}`,
  }))

  return [...inviteItems, ...postItems].sort((a, b) => {
    const ta = a.time ? new Date(a.time).getTime() : 0
    const tb = b.time ? new Date(b.time).getTime() : 0
    return tb - ta
  })
})

// 邀约类型对应的主色，供邀请卡片使用
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

async function loadPosts() {
  try {
    const res = await getDiscoveryPosts(0, 20)
    posts.value = res.data.data || []
  } catch { /* empty */ }
}

async function loadInvites() {
  try {
    // 若全局 store 已有邀约数据，则直接复用，避免重复请求
    if (!inviteStore.invites.length) {
      await inviteStore.fetchInvites()
    }
  } catch {
    // ignore
  }
}

async function loadLevelInfo() {
  try {
    const res = await getLevelInfo()
    levelInfo.value = res.data.data
  } catch { /* empty */ }
}

async function handleLike(postId: number, liked: boolean) {
  try {
    if (liked) {
      await unlikePost(postId)
      const post = posts.value.find(p => p.id === postId)
      if (post) {
        post.liked = false
        post.likeCount--
      }
    } else {
      await likePost(postId)
      const post = posts.value.find(p => p.id === postId)
      if (post) {
        post.liked = true
        post.likeCount++
      }
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

function openComment(post: FeedPost) {
  commentingPostId.value = commentingPostId.value === post.id ? null : post.id
  commentText.value = ''
}

async function submitComment(postId: number) {
  if (!commentText.value.trim()) {
    return
  }
  try {
    const content = commentText.value.trim()
    await addComment({ postId, content })
    const post = posts.value.find(p => p.id === postId)
    if (post) {
      post.commentCount = (post.commentCount || 0) + 1
      // 本地追加一条评论，用于“叠楼”即时展示
      if (!post.comments) {
        post.comments = []
      }
      post.comments.push({
        id: Date.now(),
        userId: userStore.user!.id,
        nickname: userStore.user!.nickname,
        avatarUrl: userStore.user!.avatarUrl,
        content,
        parentId: null,
        createdAt: new Date().toISOString(),
      } as any)
    }
    commentText.value = ''
    commentingPostId.value = null
    ElMessage.success('评论成功')
  } catch {
    ElMessage.error('评论失败')
  }
}

function formatTime(timeStr: string): string {
  const date = new Date(timeStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return timeStr.split(' ')[0] || timeStr
}

async function handlePost() {
  if (!postContent.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  posting.value = true
  try {
    const res = await createDiscoveryPost({ content: postContent.value.trim() })
    posts.value.unshift(res.data.data)
    postContent.value = ''
    showPostDialog.value = false
    await loadLevelInfo()
    ElMessage.success('发布成功')
  } catch (err: unknown) {
    const error = err as { response?: { data?: { message?: string } } }
    ElMessage.error(error.response?.data?.message || '发布失败')
  } finally {
    posting.value = false
  }
}

async function handleDeletePost(postId: number) {
  try {
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    ElMessage.success('删除成功')
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>

<style lang="scss" scoped>
.discover-page { padding: 0; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;
}

.page-title { font-size: 20px; font-weight: 700; }

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.post-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
}

.level-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-tip {
  font-size: 13px;
  color: $warning;
  margin-top: 8px;
}

.level-badge {
  padding: 4px 12px;
  background: $primary-gradient;
  color: white;
  border-radius: $radius-full;
  font-size: 14px;
  font-weight: 700;
}

.level-progress {
  font-size: 13px;
  color: $text-secondary;
}
 
.timeline-list {
  padding: 12px 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
}

.invite-card {
  background: linear-gradient(135deg, rgba(#ff6b9d, 0.12), rgba(#c44569, 0.08));
  border: 1px solid rgba(#ff6b9d, 0.4);
  border-radius: $radius-lg;
  padding: 12px 14px;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    border-color: $primary;
    box-shadow: $shadow-md;
  }
}

.invite-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.invite-type-badge {
  padding: 2px 10px;
  color: white;
  border-radius: $radius-full;
  font-size: 11px;
  font-weight: 600;
}

.invite-status-badge {
  font-size: 11px;
  font-weight: 600;
}

.invite-title {
  font-size: 15px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 4px;
}

.invite-content {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 6px;
}

.invite-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;

  .meta-item {
    font-size: 12px;
    color: $text-secondary;
  }
}

.invite-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.participants {
  color: $text-secondary;
}

.urgent-tag {
  padding: 2px 6px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border-radius: $radius-full;
  font-size: 11px;
  font-weight: 700;
}

.rating-info {
  margin-left: auto;
  color: $text-secondary;
}

.invite-empty {
  padding: 8px 0 4px;
  font-size: 13px;
  color: $text-muted;
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 24px;
}

.feed-card {
  padding: 16px;
}

.feed-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.feed-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  cursor: pointer;
}

.feed-user {
  cursor: pointer;
}

.feed-name {
  font-size: 15px;
  font-weight: 600;
}

.feed-time {
  font-size: 12px;
  color: $text-muted;
  margin-top: 2px;
}

.feed-content {
  font-size: 15px;
  line-height: 1.6;
  color: $text-primary;
  margin-bottom: 12px;
}

.feed-images {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.feed-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: $radius-md;
}

.feed-actions {
  display: flex;
  gap: 20px;
  padding-top: 8px;
  border-top: 1px solid $border-light;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 14px;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover { color: $primary; }
  &.active { color: #ff4757; }
  &.delete-btn { color: $text-muted; }
  &.delete-btn:hover { color: #ff4757; }
}

.action-icon { font-size: 18px; }

.comment-list {
  padding: 8px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment-item {
  font-size: 13px;
  line-height: 1.5;
  padding: 6px 10px;
  background: $bg-tertiary;
  border-radius: $radius-md;

  .comment-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 2px;
  }

  .comment-author {
    font-weight: 600;
    color: $primary;
    cursor: pointer;
  }

  .comment-time {
    font-size: 12px;
    color: $text-muted;
  }

  .comment-text {
    color: $text-primary;
    white-space: pre-wrap;
  }
}

.comment-input {
  margin-top: 8px;

  .comment-send {
    color: $primary;
    font-weight: 600;
    font-size: 13px;
    cursor: pointer;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 12px;

  .empty-icon { font-size: 64px; }
  p { color: $text-muted; font-size: 15px; }
  .empty-hint { font-size: 13px; color: $text-secondary; }
}
</style>
