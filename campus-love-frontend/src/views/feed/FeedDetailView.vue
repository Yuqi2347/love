<template>
  <div class="feed-detail-page">
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="post" class="detail-content">
      <div class="detail-header">
        <button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <span class="header-title">帖子详情</span>
      </div>

      <div class="post-card">
        <div class="post-header">
          <img
            :src="post.avatarUrl || defaultAvatar"
            class="post-avatar"
            @click="$router.push(`/profile/${post.userId}`)"
          />
          <div class="post-user" @click="$router.push(`/profile/${post.userId}`)">
            <div class="post-name">{{ post.nickname }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
          <button
            v-if="canDeletePost(post)"
            class="delete-btn"
            @click="handleDeletePost(post.id)"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </div>

        <p class="post-content">{{ post.content }}</p>

        <div v-if="post.images" class="post-images">
          <el-image
            v-for="(img, idx) in post.images.split(',')"
            :key="idx"
            :src="imageUrl(img)"
            :preview-src-list="post.images.split(',').map(i => imageUrl(i))"
            :initial-index="idx"
            fit="cover"
            class="post-image"
            preview-teleported
          />
        </div>

        <div class="post-actions">
          <button
            :class="['action-btn', { active: post.liked }]"
            @click="handleLike(post)"
          >
            <span class="action-icon">{{ post.liked ? '❤️' : '🤍' }}</span>
            <span>{{ post.likeCount }}</span>
          </button>
          <button class="action-btn" disabled>
            <span class="action-icon">💬</span>
            <span>{{ post.commentCount }}</span>
          </button>
        </div>
      </div>

      <!-- 评论区：树形线程 + 折叠 -->
      <div class="comments-section">
        <h3 class="comments-title">评论 ({{ (post.comments || []).length }})</h3>
        <div v-if="post.comments && post.comments.length" class="comment-list">
          <div
            v-for="(thread, idx) in getDisplayedThreads(post.comments)"
            :key="thread.comment.id"
            class="comment-thread"
          >
            <!-- 主评论 -->
            <div class="comment-item">
              <div class="comment-floor">{{ idx + 1 }}楼</div>
              <img :src="thread.comment.avatarUrl || defaultAvatar" class="comment-avatar" @click="$router.push(`/profile/${thread.comment.userId}`)" />
              <div class="comment-main">
                <div class="comment-header">
                  <span class="comment-author" @click="$router.push(`/profile/${thread.comment.userId}`)">
                    {{ thread.comment.nickname }}
                  </span>
                  <span class="comment-time">{{ formatTime(thread.comment.createdAt) }}</span>
                  <button class="reply-btn" @click="handleReplyClick(thread.comment)" @click.stop>
                    回复
                  </button>
                </div>
                <div class="comment-text">{{ thread.comment.content }}</div>
              </div>
            </div>
            <!-- 回复列表 -->
            <div v-if="thread.replies.length" class="thread-replies">
              <div
                v-for="reply in getDisplayedReplies(thread.comment.id, thread.replies)"
                :key="reply.id"
                class="comment-item is-reply"
              >
                <img :src="reply.avatarUrl || defaultAvatar" class="comment-avatar" @click="$router.push(`/profile/${reply.userId}`)" />
                <div class="comment-main">
                  <div class="comment-header">
                    <span class="comment-author" @click="$router.push(`/profile/${reply.userId}`)">
                      {{ reply.nickname }}
                    </span>
                    <span v-if="reply.repliedToName" class="reply-indicator">回复</span>
                    <span v-if="reply.repliedToName" class="replied-name">@{{ reply.repliedToName }}</span>
                    <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                    <button class="reply-btn" @click="handleReplyClick(reply)" @click.stop>
                      回复
                    </button>
                  </div>
                  <div class="comment-text">{{ reply.content }}</div>
                </div>
              </div>
              <button
                v-if="thread.replies.length > MAX_REPLIES_PER_THREAD && !expandedReplyThreads.has(thread.comment.id)"
                class="expand-btn"
                @click="toggleExpandReplies(thread.comment.id)"
              >
                展开 {{ thread.replies.length - MAX_REPLIES_PER_THREAD }} 条回复
              </button>
              <button
                v-else-if="expandedReplyThreads.has(thread.comment.id) && thread.replies.length > MAX_REPLIES_PER_THREAD"
                class="expand-btn"
                @click="toggleExpandReplies(thread.comment.id)"
              >
                收起回复
              </button>
            </div>
          </div>
          <!-- 展开更多评论 -->
          <button
            v-if="getHiddenThreadCount(post.comments) > 0 && !expandedCommentPosts"
            class="expand-btn"
            @click="toggleExpandComments()"
          >
            展开 {{ getHiddenThreadCount(post.comments) }} 条评论
          </button>
          <button
            v-else-if="expandedCommentPosts && buildCommentThreads(post.comments).length > MAX_ROOT_COMMENTS"
            class="expand-btn"
            @click="toggleExpandComments()"
          >
            收起评论
          </button>
        </div>
        <div v-else class="comments-empty">暂无评论</div>

        <div class="comment-input-wrap">
          <!-- 回复提示条 -->
          <div v-if="replyingTo" class="replying-hint">
            <span>回复 @{{ replyingTo.nickname }}</span>
            <button class="reply-cancel" @click="cancelReply">✕</button>
          </div>

          <el-input
            v-model="commentText"
            :placeholder="replyingTo ? `回复 @${replyingTo.nickname}...` : '写评论...'"
            maxlength="500"
            show-word-limit
            @keyup.enter="submitComment"
          >
            <template #append>
              <button
                class="comment-send-inline"
                :disabled="!commentText.trim() || submitting"
                @click="submitComment"
              >
                {{ submitting ? '发送中...' : '发送' }}
              </button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <div v-else class="error-state">
      <div class="error-icon">😕</div>
      <p>帖子不存在或已被删除</p>
      <button class="btn-outline" @click="goBack">返回</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import {
  getPostDetail,
  likePost,
  unlikePost,
  addComment,
  deletePost,
  type FeedPost,
  type FeedComment,
} from '@/api/feedApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 44 44"><rect fill="%23f0f2f5" width="44" height="44" rx="22"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="20">👤</text></svg>'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const postId = computed(() => Number(route.params.postId))
const post = ref<FeedPost | null>(null)
const loading = ref(true)
const commentText = ref('')
const submitting = ref(false)
const replyingTo = ref<{ id: number; nickname: string } | null>(null)

// 评论线程化 + 折叠
const expandedCommentPosts = ref(false)
const expandedReplyThreads = ref<Set<number>>(new Set())
const MAX_ROOT_COMMENTS = 5
const MAX_REPLIES_PER_THREAD = 2

interface CommentThread {
  comment: FeedComment
  replies: FeedComment[]
}

function buildCommentThreads(comments: FeedComment[]): CommentThread[] {
  if (!comments || !comments.length) return []
  const roots: FeedComment[] = []
  const replyMap = new Map<number, FeedComment[]>()
  for (const c of comments) {
    if (c.parentId) {
      if (!replyMap.has(c.parentId)) replyMap.set(c.parentId, [])
      replyMap.get(c.parentId)!.push(c)
    } else {
      roots.push(c)
    }
  }
  return roots.map(c => ({
    comment: c,
    replies: replyMap.get(c.id) || [],
  }))
}

function getDisplayedThreads(comments: FeedComment[]) {
  const threads = buildCommentThreads(comments)
  return expandedCommentPosts.value ? threads : threads.slice(0, MAX_ROOT_COMMENTS)
}

function getDisplayedReplies(threadId: number, replies: FeedComment[]) {
  return expandedReplyThreads.value.has(threadId) ? replies : replies.slice(0, MAX_REPLIES_PER_THREAD)
}

function getHiddenThreadCount(comments: FeedComment[]) {
  return Math.max(0, buildCommentThreads(comments).length - MAX_ROOT_COMMENTS)
}

function toggleExpandComments() {
  expandedCommentPosts.value = !expandedCommentPosts.value
}

function toggleExpandReplies(threadId: number) {
  if (expandedReplyThreads.value.has(threadId)) {
    expandedReplyThreads.value.delete(threadId)
  } else {
    expandedReplyThreads.value.add(threadId)
  }
}

function imageUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}

function formatTime(createdAt: string) {
  if (!createdAt) return ''
  const d = new Date(createdAt)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return d.toLocaleDateString()
}

function goBack() {
  router.push('/discover')
}

function canDeletePost(p: FeedPost) {
  return userStore.user?.id === p.userId
}

async function handleLike(p: FeedPost) {
  try {
    if (p.liked) {
      await unlikePost(p.id)
      p.liked = false
      p.likeCount = (p.likeCount || 1) - 1
    } else {
      await likePost(p.id)
      p.liked = true
      p.likeCount = (p.likeCount || 0) + 1
    }
  } catch { /* handled */ }
}

async function handleDeletePost(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这条动态吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deletePost(id)
    ElMessage.success('已删除')
    goBack()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function submitComment() {
  const text = commentText.value.trim()
  if (!text || !post.value) return
  submitting.value = true
  try {
    const parentId = replyingTo.value ? replyingTo.value.id : null
    const repliedUserId = replyingTo.value ? getCommentUserId(replyingTo.value.id) : null

    await addComment({ postId: post.value.id, content: text, parentId, repliedUserId })
    const newComment: FeedComment = {
      id: Date.now(),
      userId: userStore.user!.id,
      nickname: userStore.user!.nickname,
      avatarUrl: userStore.user!.avatarUrl,
      content: text,
      parentId: parentId,
      repliedToName: replyingTo.value?.nickname || null,
      createdAt: new Date().toISOString(),
    }
    if (!post.value.comments) post.value.comments = []
    post.value.comments.push(newComment)
    post.value.commentCount = (post.value.commentCount || 0) + 1
    commentText.value = ''
    replyingTo.value = null
    ElMessage.success('评论成功')
  } catch {
    ElMessage.error('评论失败')
  } finally {
    submitting.value = false
  }
}

function handleReplyClick(comment: FeedComment) {
  replyingTo.value = { id: comment.id, nickname: comment.nickname }
  commentText.value = ''
  // 聚焦到输入框
  document.querySelector('.comment-input-wrap input')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function cancelReply() {
  replyingTo.value = null
  commentText.value = ''
}

function getCommentUserId(commentId: number): number | null {
  const comment = post.value?.comments?.find(c => c.id === commentId)
  return comment?.userId || null
}

onMounted(async () => {
  try {
    const res = await getPostDetail(postId.value)
    post.value = res.data.data || null
  } catch {
    post.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.feed-detail-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 16px;
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--el-text-color-secondary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--el-border-color-light);
  border-top-color: var(--el-color-primary);
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
  gap: 12px;
  margin-bottom: 16px;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: transparent;
  transition: background 0.2s;
  &:hover { background: var(--el-fill-color-light); }
}

.header-title { font-size: 18px; font-weight: 600; }

.post-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.post-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  cursor: pointer;
  object-fit: cover;
}

.post-user { flex: 1; cursor: pointer; }
.post-name { font-weight: 600; font-size: 15px; }
.post-time { font-size: 12px; color: var(--el-text-color-secondary); }

.delete-btn {
  padding: 4px;
  color: var(--el-text-color-secondary);
  &:hover { color: var(--el-color-danger); }
}

.post-content {
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 12px;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.post-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  cursor: pointer;
}

.post-actions {
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

.comments-section {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 16px;
}

.comments-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.comment-thread {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thread-replies {
  margin-left: 40px;
  padding-left: 12px;
  border-left: 2px solid rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.2);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.expand-btn {
  padding: 4px 12px;
  font-size: 13px;
  color: var(--el-color-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.comment-item.is-reply {
  /* indent is handled by .thread-replies container */
}

.comment-floor {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  min-width: 28px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.comment-main { flex: 1; min-width: 0; }

.comment-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.reply-indicator {
  font-size: 12px;
  color: $text-secondary;
}

.replied-name {
  font-size: 13px;
  color: $primary;
  font-weight: 500;
}

.reply-btn {
  margin-left: auto;
  padding: 2px 8px;
  font-size: 12px;
  color: $text-secondary;
  background: transparent;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    color: $primary;
    border-color: rgba($primary, 0.3);
    background: rgba($primary, 0.06);
  }
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  &:hover { color: var(--el-color-primary); }
}

.comment-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.comment-text {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.comments-empty {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 24px;
  margin-bottom: 16px;
}

.comment-input-wrap {
  :deep(.el-input-group__append) {
    padding: 0;
    background: none;
    border: none;
  }
}

.replying-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba($primary, 0.08);
  border-radius: $radius-md;
  margin-bottom: 8px;
  font-size: 13px;
  color: $primary;
}

.reply-cancel {
  margin-left: auto;
  padding: 2px 6px;
  background: transparent;
  border: none;
  color: $text-muted;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;

  &:hover {
    color: $text-secondary;
  }
}

.comment-send-inline {
  padding: 0 16px;
  height: 32px;
  background: var(--el-color-primary);
  color: white;
  border: none;
  border-radius: 0 8px 8px 0;
  font-size: 14px;
  cursor: pointer;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
  &:hover:not(:disabled) { opacity: 0.9; }
}

.btn-outline {
  padding: 8px 20px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  margin-top: 16px;
}
</style>
