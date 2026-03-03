<template>
  <div class="feed-page">
    <div class="page-header">
      <h2 class="page-title">朋友圈</h2>
    </div>

    <div class="feed-list" v-if="posts.length">
      <div v-for="post in posts" :key="post.id" class="feed-item">
        <div class="feed-author">
          <img :src="post.avatarUrl || defaultAvatar" class="avatar" width="44" height="44"
            @click="$router.push(`/profile/${post.userId}`)" style="cursor:pointer;" />
          <div class="author-info">
            <span class="author-name" @click="$router.push(`/profile/${post.userId}`)">{{ post.nickname }}</span>
            <span class="post-time">{{ post.createdAt }}</span>
          </div>
          <button v-if="post.userId === userStore.user?.id" class="delete-btn" @click="handleDelete(post.id)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>

        <p class="feed-content">{{ post.content }}</p>

        <div v-if="post.images" class="feed-images">
          <img v-for="(img, i) in post.images.split(',')" :key="i" :src="img" class="feed-img" />
        </div>

        <div class="feed-actions">
          <button :class="['action-item', { liked: post.liked }]" @click="toggleLike(post)">
            <el-icon><component :is="post.liked ? 'StarFilled' : 'Star'" /></el-icon>
            <span>{{ post.likeCount || '' }}</span>
          </button>
          <button class="action-item" @click="openComment(post)">
            <el-icon><ChatRound /></el-icon>
            <span>{{ post.commentCount || '' }}</span>
          </button>
        </div>

        <div v-if="post.comments?.length" class="comment-list">
          <div v-for="c in post.comments" :key="c.id" class="comment-item">
            <span class="comment-author" @click="$router.push(`/profile/${c.userId}`)">{{ c.nickname }}</span>
            <span class="comment-text">{{ c.content }}</span>
          </div>
        </div>

        <div v-if="commentingPostId === post.id" class="comment-input">
          <el-input v-model="commentText" placeholder="写评论..." size="small" @keyup.enter="submitComment(post.id)">
            <template #append>
              <button class="comment-send" @click="submitComment(post.id)">发送</button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">📝</div>
      <p>暂无动态</p>
      <p class="empty-hint">互相关注的好友动态会显示在这里</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/userStore'
import { getTimeline, likePost, unlikePost, addComment, deletePost, type FeedPost } from '@/api/feedApi'
import { ElMessage, ElMessageBox } from 'element-plus'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 44 44"><rect fill="%23f0f2f5" width="44" height="44" rx="22"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="20">👤</text></svg>'
const userStore = useUserStore()
const posts = ref<FeedPost[]>([])
const commentingPostId = ref<number | null>(null)
const commentText = ref('')

onMounted(async () => {
  try {
    const res = await getTimeline(0, 20)
    posts.value = res.data.data || []
  } catch { /* empty */ }
})

async function toggleLike(post: FeedPost) {
  try {
    if (post.liked) {
      await unlikePost(post.id)
      post.liked = false
      post.likeCount--
    } else {
      await likePost(post.id)
      post.liked = true
      post.likeCount++
    }
  } catch { /* handled */ }
}

function openComment(post: FeedPost) {
  commentingPostId.value = commentingPostId.value === post.id ? null : post.id
  commentText.value = ''
}

async function submitComment(postId: number) {
  if (!commentText.value.trim()) return
  try {
    await addComment({ postId, content: commentText.value.trim() })
    const post = posts.value.find(p => p.id === postId)
    if (post) {
      post.commentCount++
      post.comments.push({
        id: Date.now(),
        userId: userStore.user!.id,
        nickname: userStore.user!.nickname,
        avatarUrl: userStore.user!.avatarUrl,
        content: commentText.value.trim(),
        parentId: null,
        createdAt: new Date().toISOString(),
      })
    }
    commentText.value = ''
    commentingPostId.value = null
  } catch { /* handled */ }
}

async function handleDelete(postId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条动态？', '提示', { type: 'warning' })
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    ElMessage.success('已删除')
  } catch { /* cancelled or error */ }
}
</script>

<style lang="scss" scoped>
.feed-page { padding: 0; }

.page-header {
  padding: 20px 24px;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;
  .page-title { font-size: 20px; font-weight: 700; }
}

.feed-list { display: flex; flex-direction: column; }

.feed-item {
  padding: 16px 24px;
  border-bottom: 1px solid $border-light;
  transition: background $transition-fast;
  &:hover { background: rgba($bg-tertiary, 0.5); }
}

.feed-author {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;

  .author-info { flex: 1; display: flex; flex-direction: column; }
  .author-name { font-size: 15px; font-weight: 600; cursor: pointer; &:hover { color: $primary; } }
  .post-time { font-size: 12px; color: $text-muted; }
  .delete-btn {
    color: $text-muted;
    width: 32px; height: 32px;
    display: flex; align-items: center; justify-content: center;
    border-radius: $radius-full;
    &:hover { color: $danger; background: rgba($danger, 0.1); }
  }
}

.feed-content {
  font-size: 15px;
  line-height: 1.6;
  color: $text-primary;
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.feed-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.feed-img {
  width: 100%;
  border-radius: $radius-md;
  object-fit: cover;
  aspect-ratio: 1;
}

.feed-actions {
  display: flex;
  gap: 32px;
  padding: 8px 0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
  padding: 4px 8px;
  border-radius: $radius-full;
  transition: all $transition-fast;

  &:hover { color: $primary; background: rgba($primary, 0.08); }
  &.liked { color: $primary; }
}

.comment-list {
  padding: 10px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment-item {
  font-size: 13px;
  line-height: 1.5;
  padding: 4px 12px;
  background: $bg-tertiary;
  border-radius: $radius-sm;

  .comment-author {
    font-weight: 600;
    color: $primary;
    cursor: pointer;
    margin-right: 6px;
  }
  .comment-text { color: $text-primary; }
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
  padding: 80px 20px;
  gap: 8px;
  .empty-icon { font-size: 64px; margin-bottom: 8px; }
  p { color: $text-muted; font-size: 15px; }
  .empty-hint { font-size: 13px; }
}
</style>
