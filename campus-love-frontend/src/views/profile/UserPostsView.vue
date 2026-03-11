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
            :src="post.avatarUrl || defaultAvatar"
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
        <div class="feed-content">{{ post.content }}</div>
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
        <div v-if="post.linkUrl" class="feed-link" @click.stop>
          <a :href="post.linkUrl" target="_blank" class="link-card">
            <img v-if="post.linkImage" :src="getMediaUrl(post.linkImage)" class="link-image" />
            <div class="link-content">
              <div class="link-title">{{ post.linkTitle || post.linkUrl }}</div>
            </div>
          </a>
        </div>
        <div class="feed-actions" @click.stop>
          <button
            :class="['action-btn', { active: post.liked }]"
            @click="handleLike(post)"
          >
            <span class="action-icon">{{ post.liked ? '❤️' : '🤍' }}</span>
            <span>{{ post.likeCount }}</span>
          </button>
          <button class="action-btn" @click="goPostDetail(post.id)">
            <span class="action-icon">💬</span>
            <span>{{ post.commentCount }}</span>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无动态</p>
    </div>
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

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 44 44"><rect fill="%23f0f2f5" width="44" height="44" rx="22"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="20">👤</text></svg>'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.userId))
const profile = ref<UserProfile | null>(null)
const posts = ref<FeedPost[]>([])
const loading = ref(true)

function getMediaUrl(url: string | null): string {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}

function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return d.toLocaleDateString()
}

function goPostDetail(postId: number) {
  router.push(`/feed/${postId}`)
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

.feed-link {
  margin-bottom: 12px;
}

.link-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
}

.link-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.link-content {
  flex: 1;
  min-width: 0;
}

.link-title {
  font-size: 14px;
  font-weight: 500;
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
  &.active { color: #f43f5e; }
  &:hover:not(:disabled) { color: var(--el-color-primary); }
}

.action-icon {
  font-size: 18px;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
