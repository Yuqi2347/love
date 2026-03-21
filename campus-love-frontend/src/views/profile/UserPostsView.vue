<template>
  <div class="user-posts-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <span class="header-title">{{ profile?.nickname || '加载中...' }}的动态</span>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="posts.length" class="post-list">
      <div v-for="post in posts" :key="post.id" class="feed-card card">
        <div class="feed-header" @click.stop>
          <img
            :src="getMediaUrl(post.avatarUrl) || defaultAvatar"
            class="feed-avatar"
            @click="$router.push(`/profile/${post.userId}`)"
          />
          <div class="feed-user" @click="$router.push(`/profile/${post.userId}`)">
            <div class="feed-name">{{ post.nickname }}</div>
            <div class="feed-time">{{ formatTime(post.createdAt) }}</div>
          </div>
          <button
            v-if="canDeletePost(post)"
            type="button"
            class="feed-delete-btn"
            title="删除"
            @click.stop="handleDeletePost(post.id)"
          >
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </button>
        </div>
        <div class="feed-content">
          <template v-if="shouldCollapse(post.content)">
            <span v-if="isExpanded(post.id)">{{ post.content }}</span>
            <span v-else>{{ getDisplayContent(post.content, post.id) }}</span>
            <button class="expand-btn" @click="toggleExpand(post.id)">
              {{ isExpanded(post.id) ? '收起' : '显示更多' }}
            </button>
          </template>
          <span v-else>{{ post.content }}</span>
        </div>
        <div v-if="post.images" class="feed-images" @click.stop>
          <img
            v-for="(img, idx) in post.images.split(',')"
            :key="idx"
            :src="getMediaUrl(img)"
            class="feed-image"
            @click="goPostDetail(post.id)"
          />
        </div>
        <div v-if="post.videos" class="feed-videos" @click.stop>
          <video
            v-for="(video, idx) in post.videos.split(',')"
            :key="idx"
            :src="getMediaUrl(video)"
            class="feed-video"
            controls
          />
        </div>
        <FeedInviteCard v-if="post.inviteCard" :card="post.inviteCard" />
        <!-- AI 标签 -->
        <div v-if="post.aiTags" class="feed-ai-tags">
          <span
            v-for="tag in (post.aiTags || '').split(/[,，]/).filter(Boolean)"
            :key="tag"
            class="ai-tag"
          >{{ tag.trim() }}</span>
        </div>
        <div class="feed-actions" @click.stop>
          <button
            :class="['action-btn', { active: post.liked }]"
            @click="handleLike(post)"
          >
            <el-icon :size="18"><StarFilled v-if="post.liked" /><Star v-else /></el-icon>
            <span>{{ post.likeCount }}</span>
          </button>
          <button class="action-btn" @click="goPostDetail(post.id)">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>{{ post.commentCount }}</span>
          </button>
          <button class="action-btn" @click="openShareDialog(post)">
            <el-icon :size="18"><Share /></el-icon>
            <span>分享</span>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <el-icon class="empty-icon" :size="48"><Document /></el-icon>
      <p>暂无动态</p>
    </div>

    <!-- 分享弹窗 -->
    <ShareDialog
      v-model:show="showShareDialog"
      :post="currentSharePost"
      @success="handleShareSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { getUserProfile, type UserProfile } from '@/api/userApi'
import {
  getUserPosts,
  likePost,
  unlikePost,
  deletePost,
  type FeedPost,
} from '@/api/feedApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import ShareDialog from '@/components/ShareDialog.vue'
import FeedInviteCard from '@/components/FeedInviteCard.vue'
import { DEFAULT_AVATAR, getMediaUrl, formatRelativeTime } from '@/utils/shared'

const defaultAvatar = DEFAULT_AVATAR

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.userId))
const profile = ref<UserProfile | null>(null)
const posts = ref<FeedPost[]>([])
// 帖子展开状态 Map
const expandedPosts = ref<Map<number, boolean>>(new Map())
// 文字内容折叠配置
const CONTENT_MAX_LENGTH = 100
const loading = ref(true)

// 分享相关状态
const showShareDialog = ref(false)
const currentSharePost = ref<FeedPost | null>(null)

function openShareDialog(post: FeedPost) {
  currentSharePost.value = post
  showShareDialog.value = true
}

function handleShareSuccess() {
  console.log('分享成功')
}

function isExpanded(postId: number): boolean {
  return expandedPosts.value.get(postId) || false
}

// 切换帖子展开/折叠状态
function toggleExpand(postId: number) {
  const current = expandedPosts.value.get(postId) || false
  expandedPosts.value.set(postId, !current)
}

// 获取帖子显示内容
function getDisplayContent(content: string, postId: number): string {
  if (isExpanded(postId) || content.length <= CONTENT_MAX_LENGTH) {
    return content
  }
  return content.slice(0, CONTENT_MAX_LENGTH) + '...'
}

// 检查帖子内容是否需要折叠
function shouldCollapse(content: string): boolean {
  return content.length > CONTENT_MAX_LENGTH
}

function formatTime(timeStr: string): string {
  return formatRelativeTime(timeStr)
}

function goPostDetail(postId: number) {
  const id = Number(postId)
  if (!Number.isFinite(id) || id <= 0) return
  router.push(`/feed/${id}`)
}

function canDeletePost(post: FeedPost): boolean {
  return userStore.user?.id === post.userId || userStore.user?.isAdmin === true
}

async function handleLike(post: FeedPost) {
  try {
    if (post.liked) {
      await unlikePost(post.id)
      post.liked = false
      post.likeCount = (post.likeCount || 1) - 1
    } else {
      await likePost(post.id)
      post.liked = true
      post.likeCount = (post.likeCount || 0) + 1
    }
  } catch { /* handled */ }
}

async function handleDeletePost(postId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条动态吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(async () => {
  if (!userId.value) return
  loading.value = true
  try {
    const [profileRes, postsRes] = await Promise.all([
      getUserProfile(userId.value),
      getUserPosts(userId.value, 0, 30),
    ])
    profile.value = profileRes.data.data
    posts.value = postsRes.data.data || []
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.user-posts-page {
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.back-btn {
  padding: 8px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.loading-state {
  padding: 60px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 16px;
  border: 3px solid var(--el-border-color-lighter);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.post-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feed-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.feed-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.feed-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
}

.feed-user {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.feed-name {
  font-weight: 600;
  font-size: 15px;
}

.feed-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.feed-delete-btn {
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  &:hover { color: var(--el-color-danger); }
}

.feed-content {
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
  white-space: pre-wrap;
}

.expand-btn {
  color: #6366f1;
  background: none;
  border: none;
  padding: 0;
  font-size: 14px;
  cursor: pointer;
  margin-left: 4px;

  &:hover {
    text-decoration: underline;
  }
}

.feed-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.feed-image {
  max-width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
}

.feed-videos {
  margin-bottom: 12px;
}

.feed-video {
  max-width: 100%;
  border-radius: 8px;
}

.feed-ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 0;
}

.ai-tag {
  font-size: 12px;
  color: var(--el-color-primary);
  background: rgba(var(--el-color-primary-rgb), 0.08);
  padding: 2px 8px;
  border-radius: 4px;
}

.feed-actions {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  &.active { color: $primary; }
  &:hover:not(:disabled) { color: var(--el-color-primary); }
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  margin-bottom: 16px;
  color: $text-muted;
}

.empty-state p {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
