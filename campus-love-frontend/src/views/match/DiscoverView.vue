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

    <div v-if="posts.length" class="feed-list">
      <div v-for="post in posts" :key="post.id" class="feed-card card">
        <div class="feed-header">
          <img :src="post.avatarUrl || defaultAvatar" class="feed-avatar" @click="$router.push(`/profile/${post.userId}`)" />
          <div class="feed-user" @click="$router.push(`/profile/${post.userId}`)">
            <div class="feed-name">{{ post.nickname }}</div>
            <div class="feed-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <div class="feed-content">{{ post.content }}</div>
        <div v-if="post.images" class="feed-images">
          <img v-for="(img, idx) in post.images.split(',').slice(0, 3)" :key="idx" :src="img" class="feed-image" />
        </div>
        <div class="feed-actions">
          <button :class="['action-btn', { active: post.liked }]" @click="handleLike(post.id, post.liked)">
            <span class="action-icon">{{ post.liked ? '❤️' : '🤍' }}</span>
            <span>{{ post.likeCount }}</span>
          </button>
          <button class="action-btn" @click="$router.push(`/profile/${post.userId}`)">
            <span class="action-icon">💬</span>
            <span>{{ post.commentCount }}</span>
          </button>
          <button v-if="canDeletePost(post)" class="action-btn delete-btn" @click="handleDeletePost(post.id)">
            <span class="action-icon">🗑️</span>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无动态</p>
      <p v-if="canPost" class="empty-hint">成为第一个发布动态的人吧！</p>
      <p v-else class="empty-hint">达到 Lv3 等级即可发布动态</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDiscoveryPosts, likePost, unlikePost, getLevelInfo, createDiscoveryPost, deletePost, type FeedPost, type UserLevelInfo } from '@/api/feedApi'
import { useUserStore } from '@/store/userStore'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const posts = ref<FeedPost[]>([])
const levelInfo = ref<UserLevelInfo | null>(null)
const showPostDialog = ref(false)
const postContent = ref('')
const posting = ref(false)

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
  await loadPosts()
  await loadLevelInfo()
})

async function loadPosts() {
  try {
    const res = await getDiscoveryPosts(0, 20)
    posts.value = res.data.data || []
  } catch { /* empty */ }
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
