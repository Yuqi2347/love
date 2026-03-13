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
            :src="imageUrl(post.avatarUrl) || defaultAvatar"
            class="post-avatar"
            @click="$router.push(`/profile/${post.userId}`)"
          />
          <div class="post-user">
            <div class="post-name" @click="$router.push(`/profile/${post.userId}`)">{{ post.nickname }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
          <button
            :class="['report-btn', { reported: postReported }]"
            :title="postReported ? '已举报' : '举报'"
            @click="handleReportClick(post.id, 'POST')"
          >
            <el-icon><Flag /></el-icon>
          </button>
          <button
            v-if="canDeletePost(post)"
            class="delete-btn"
            @click="handleDeletePost(post.id)"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </div>

        <p class="post-content">{{ post.content }}</p>

        <!-- AI 标签（所有人可见） -->
        <div v-if="post.aiTags" class="post-ai-tags">
          <span
            v-for="tag in (post.aiTags || '').split(/[,，]/).filter(Boolean)"
            :key="tag"
            class="ai-tag"
          >{{ tag.trim() }}</span>
        </div>
        <!-- AI 标签确认条（仅作者可见，5秒自动消失，可编辑） -->
        <FeedTagConfirmBar
          v-if="isPostOwner && post.aiTags"
          :post-id="post.id"
          :ai-tags="post.aiTags"
          :is-owner="isPostOwner"
          :auto-dismiss-ms="5000"
          @update:ai-tags="post.aiTags = $event"
        />
        <!-- 作者可手动触发重新生成标签（无标签时显示） -->
        <div v-if="isPostOwner && !post.aiTags" class="retag-hint">
          <button type="button" class="retag-btn" :disabled="retagging" @click="handleRetag">
            {{ retagging ? '生成中...' : '生成 AI 标签' }}
          </button>
        </div>

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

        <!-- 视频展示 -->
        <div v-if="post.videos" class="post-videos">
          <video
            v-for="(video, idx) in post.videos.split(',')"
            :key="idx"
            :src="videoUrl(video)"
            class="post-video"
            controls
          />
        </div>

        <div class="post-actions">
          <button
            :class="['action-btn', { active: post.liked }]"
            @click="handleLike(post)"
          >
            <el-icon :size="18"><StarFilled v-if="post.liked" /><Star v-else /></el-icon>
            <span>{{ post.likeCount }}</span>
          </button>
          <button class="action-btn" disabled>
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>{{ post.commentCount }}</span>
          </button>
          <button class="action-btn" @click="openShareDialog">
            <el-icon :size="18"><Share /></el-icon>
            <span>分享</span>
          </button>
        </div>
      </div>

      <!-- 评论区：虎扑/小红书 两层扁平 -->
      <div class="comments-section">
        <h3 class="comments-title">评论 ({{ post.commentCount || 0 }})</h3>
        <div v-if="commentThreads.length" class="comment-list">
          <div
            v-for="(thread, idx) in displayedThreads"
            :key="thread.comment.id"
            class="comment-thread"
          >
            <!-- 根评论 -->
            <div class="root-comment" :class="{ 'comment-deleted': thread.comment.deleted }">
              <img
                :src="thread.comment.avatarUrl || defaultAvatar"
                class="comment-avatar"
                @click="$router.push(`/profile/${thread.comment.userId}`)"
              />
              <div class="comment-body">
                <div class="comment-meta">
                  <span class="comment-author" @click="$router.push(`/profile/${thread.comment.userId}`)">
                    {{ thread.comment.nickname }}
                  </span>
                  <span class="comment-floor">#{{ idx + 1 }}</span>
                </div>
                <div class="comment-text">{{ thread.comment.content }}</div>
                <div v-if="thread.comment.images && !thread.comment.deleted" class="comment-images">
                  <el-image
                    v-for="(img, i) in thread.comment.images.split(',')"
                    :key="i"
                    :src="imageUrl(img)"
                    :preview-src-list="thread.comment.images.split(',').map(u => imageUrl(u))"
                    :initial-index="i"
                    fit="cover"
                    class="comment-img"
                    preview-teleported
                  />
                </div>
                <div class="comment-footer">
                  <span class="comment-time">{{ formatTime(thread.comment.createdAt) }}</span>
                  <button v-if="!thread.comment.deleted" class="reply-btn" @click="handleReplyClick(thread.comment, thread.comment)">回复</button>
                  <button v-if="canDeleteComment(thread.comment)" class="reply-btn delete-comment-btn" @click="handleDeleteComment(thread.comment.id)">删除</button>
                </div>
              </div>
            </div>
            <!-- 子回复列表 -->
            <div v-if="thread.replies.length" class="reply-list">
              <div
                v-for="reply in getDisplayedReplies(thread.comment.id, thread.replies)"
                :key="reply.id"
                class="reply-item"
                :class="{ 'comment-deleted': reply.deleted }"
              >
                <img
                  :src="reply.avatarUrl || defaultAvatar"
                  class="reply-avatar"
                  @click="$router.push(`/profile/${reply.userId}`)"
                />
                <div class="reply-body">
                  <div class="reply-content">
                    <span class="reply-author" @click="$router.push(`/profile/${reply.userId}`)">{{ reply.nickname }}</span>
                    <template v-if="reply.repliedToName && !reply.deleted">
                      <span class="reply-arrow"> 回复 </span>
                      <span class="reply-target">@{{ reply.repliedToName }}</span>
                    </template>
                    <span class="reply-text">{{ reply.deleted ? '' : '：' }}{{ reply.content }}</span>
                    <div v-if="reply.images && !reply.deleted" class="comment-images">
                      <el-image
                        v-for="(img, i) in reply.images.split(',')"
                        :key="i"
                        :src="imageUrl(img)"
                        :preview-src-list="reply.images.split(',').map(u => imageUrl(u))"
                        :initial-index="i"
                        fit="cover"
                        class="comment-img"
                        preview-teleported
                      />
                    </div>
                  </div>
                  <div class="reply-footer">
                    <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                    <button v-if="!reply.deleted" class="reply-btn" @click="handleReplyClick(reply, thread.comment)">回复</button>
                    <button v-if="canDeleteComment(reply)" class="reply-btn delete-comment-btn" @click="handleDeleteComment(reply.id)">删除</button>
                  </div>
                </div>
              </div>
              <!-- 展开更多回复 -->
              <button
                v-if="thread.replies.length > MAX_REPLIES && !expandedReplies.has(thread.comment.id)"
                class="expand-replies-btn"
                @click="toggleExpandReplies(thread.comment.id)"
              >
                展开其余 {{ thread.replies.length - MAX_REPLIES }} 条回复
              </button>
              <button
                v-else-if="expandedReplies.has(thread.comment.id) && thread.replies.length > MAX_REPLIES"
                class="expand-replies-btn"
                @click="toggleExpandReplies(thread.comment.id)"
              >
                收起回复
              </button>
            </div>
          </div>
          <!-- 展开更多评论 -->
          <button
            v-if="commentThreads.length > MAX_THREADS && !showAllThreads"
            class="expand-threads-btn"
            @click="showAllThreads = true"
          >
            查看全部 {{ commentThreads.length }} 条评论
          </button>
        </div>
        <div v-else class="comments-empty">暂无评论，快来抢沙发~</div>

        <!-- 底部固定输入框 -->
        <div class="comment-input-bar">
          <div v-if="replyingTo" class="replying-hint">
            <span>回复 @{{ replyingTo.nickname }}</span>
            <button class="reply-cancel" @click="cancelReply">✕</button>
          </div>
          <div v-if="commentImages.length" class="comment-images-preview">
            <div v-for="(img, i) in commentImages" :key="i" class="comment-preview-item">
              <img :src="imageUrl(img)" class="comment-preview-img" />
              <button type="button" class="comment-preview-remove" @click="removeCommentImage(i)">
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>
          <div class="input-row">
            <input ref="commentImageInputRef" type="file" accept="image/*" multiple class="hidden-file-input" @change="handleCommentImageSelect" />
            <button type="button" class="icon-btn" title="图片" @click="commentImageInputRef?.click()">
              <el-icon><Picture /></el-icon>
            </button>
            <EmojiPicker @insert="insertCommentEmoji" />
            <el-input
              ref="commentInputRef"
              v-model="commentText"
              :placeholder="replyingTo ? `回复 @${replyingTo.nickname}...` : '写评论...'"
              maxlength="500"
              @keyup.enter="submitComment"
            />
            <button
              class="send-btn"
              :disabled="(!commentText.trim() && !commentImages.length) || submitting"
              @click="submitComment"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="error-state">
      <div class="error-icon">😕</div>
      <p>帖子不存在或已被删除</p>
      <button class="btn-outline" @click="goBack">返回</button>
    </div>

    <!-- 举报弹窗 -->
    <ReportDialog
      v-model="showReportDialog"
      :target-type="reportTargetType"
      :target-id="reportTargetId"
      @success="onReportSuccess"
    />

    <!-- 分享弹窗 -->
    <ShareDialog
      v-model:show="showShareDialog"
      :post="post"
      @success="handleShareSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import {
  getPostDetail,
  likePost,
  unlikePost,
  addComment,
  deletePost,
  deleteComment,
  retagPost,
  type FeedPost,
  type FeedComment,
} from '@/api/feedApi'
import { checkReported, getMyReport } from '@/api/reportApi'
import ReportDialog from '@/components/ReportDialog.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Flag, Picture, Close } from '@element-plus/icons-vue'
import ShareDialog from '@/components/ShareDialog.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import FeedTagConfirmBar from './components/FeedTagConfirmBar.vue'
import { uploadImage } from '@/api/feedApi'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 44 44"><rect fill="%23f0f2f5" width="44" height="44" rx="22"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="20">👤</text></svg>'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const postId = computed(() => Number(route.params.postId))
const isPostIdValid = computed(() => {
  const id = postId.value
  return Number.isFinite(id) && id > 0
})
const post = ref<FeedPost | null>(null)
const loading = ref(true)
const commentText = ref('')
const commentImages = ref<string[]>([])
const submitting = ref(false)
const retagging = ref(false)
const commentInputRef = ref()
const commentImageInputRef = ref<HTMLInputElement | null>(null)

// 分享相关状态
const showShareDialog = ref(false)

// 举报相关
const showReportDialog = ref(false)
const reportTargetType = ref('POST')
const reportTargetId = ref(0)
const postReported = ref(false)

function openShareDialog() {
  showShareDialog.value = true
}

function handleShareSuccess() {
  console.log('分享成功')
}

// 回复状态：rootCommentId 用于 parentId（始终指向根评论），repliedUserId/nickname 用于标记实际被回复人
const replyingTo = ref<{
  rootCommentId: number
  repliedUserId: number
  nickname: string
} | null>(null)

// 折叠状态
const showAllThreads = ref(false)
const expandedReplies = ref<Set<number>>(new Set())
const MAX_THREADS = 10
const MAX_REPLIES = 2

interface CommentThread {
  comment: FeedComment
  replies: FeedComment[]
}

/**
 * 虎扑/小红书两层扁平模型：
 * 所有回复（无论嵌套多深）都归入其所属的根评论下方。
 * 通过 parentId 链向上追溯到根评论。
 */
function buildCommentThreads(comments: FeedComment[]): CommentThread[] {
  if (!comments || !comments.length) return []

  const commentMap = new Map<number, FeedComment>()
  for (const c of comments) commentMap.set(c.id, c)

  const roots: FeedComment[] = []
  const replyMap = new Map<number, FeedComment[]>()

  for (const c of comments) {
    if (!c.parentId) {
      roots.push(c)
    } else {
      // 向上追溯到根评论
      let rootId = c.parentId
      let parent = commentMap.get(rootId)
      while (parent && parent.parentId) {
        rootId = parent.parentId
        parent = commentMap.get(rootId)
      }
      if (!replyMap.has(rootId)) replyMap.set(rootId, [])
      replyMap.get(rootId)!.push(c)
    }
  }

  return roots.map(c => ({
    comment: c,
    replies: replyMap.get(c.id) || [],
  }))
}

const commentThreads = computed(() => {
  return post.value?.comments ? buildCommentThreads(post.value.comments) : []
})

const displayedThreads = computed(() => {
  return showAllThreads.value ? commentThreads.value : commentThreads.value.slice(0, MAX_THREADS)
})

function getDisplayedReplies(threadId: number, replies: FeedComment[]) {
  return expandedReplies.value.has(threadId) ? replies : replies.slice(0, MAX_REPLIES)
}

function toggleExpandReplies(threadId: number) {
  if (expandedReplies.value.has(threadId)) {
    expandedReplies.value.delete(threadId)
  } else {
    expandedReplies.value.add(threadId)
  }
}

function imageUrl(url: string) {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}

function videoUrl(url: string) {
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
  router.back()
}

const isPostOwner = computed(() => post.value && userStore.user?.id === post.value.userId)

function canDeletePost(p: FeedPost) {
  return userStore.user?.id === p.userId
}

function canDeleteComment(c: FeedComment) {
  if (c.deleted) return false
  return userStore.user?.id === c.userId || userStore.user?.isAdmin
}

async function handleDeleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteComment(commentId)
    ElMessage.success('已删除')
    if (post.value) {
      const p = await getPostDetail(postId.value)
      post.value = p.data.data
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
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

async function handleReportClick(targetId: number, targetType: string) {
  try {
    const res = await checkReported(targetType, targetId)
    if (res.data.data) {
      const myRes = await getMyReport(targetType, targetId)
      const info = myRes.data.data
      const types = info?.violationTypes ? info.violationTypes.split(',') : []
      const reason = info?.reason || ''
      const adminNote = info?.adminNote || ''
      let msg = types.length ? `举报类型：${types.join('、')}${reason ? `\n理由：${reason}` : ''}` : '您已举报过该内容'
      if (adminNote) msg += `\n\n举报反馈：${adminNote}`
      ElMessage.info(msg)
      return
    }
    reportTargetType.value = targetType
    reportTargetId.value = targetId
    showReportDialog.value = true
  } catch {
    ElMessage.error('操作失败')
  }
}

function onReportSuccess() {
  postReported.value = true
}

async function handleRetag() {
  if (!post.value || retagging.value) return
  retagging.value = true
  try {
    await retagPost(post.value.id)
    const res = await getPostDetail(postId.value)
    if (res.data.data) {
      post.value = res.data.data
      ElMessage.success('AI 标签已生成')
    }
  } catch {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    retagging.value = false
  }
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

/**
 * 点击回复按钮
 * @param targetComment 被回复的评论（可以是根评论或子回复）
 * @param rootComment 该评论所属的根评论（用于 parentId）
 */
function handleReplyClick(targetComment: FeedComment, rootComment: FeedComment) {
  replyingTo.value = {
    rootCommentId: rootComment.id,
    repliedUserId: targetComment.userId,
    nickname: targetComment.nickname,
  }
  commentText.value = ''
  // 滚动到输入框
  setTimeout(() => {
    const el = document.querySelector('.comment-input-bar')
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, 100)
}

function cancelReply() {
  replyingTo.value = null
  commentText.value = ''
  commentImages.value = []
}

function insertCommentEmoji(emoji: string) {
  commentText.value += emoji
}

async function handleCommentImageSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files?.length) return
  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (!file.type.startsWith('image/')) continue
    try {
      const res = await uploadImage(file)
      const url = res.data.data
      if (url) commentImages.value.push(url)
    } catch {
      ElMessage.error('图片上传失败')
    }
  }
  if (target) target.value = ''
}

function removeCommentImage(idx: number) {
  commentImages.value.splice(idx, 1)
}

async function submitComment() {
  const text = commentText.value.trim()
  const imagesStr = commentImages.value.length ? commentImages.value.join(',') : ''
  if ((!text && !imagesStr) || !post.value) return
  submitting.value = true
  try {
    const parentId = replyingTo.value ? replyingTo.value.rootCommentId : null
    const repliedUserId = replyingTo.value ? replyingTo.value.repliedUserId : null

    await addComment({ postId: post.value.id, content: text || '', images: imagesStr || undefined, parentId, repliedUserId })

    const newComment: FeedComment = {
      id: Date.now(),
      userId: userStore.user!.id,
      nickname: userStore.user!.nickname,
      avatarUrl: userStore.user!.avatarUrl,
      content: text,
      images: imagesStr || null,
      parentId: parentId,
      repliedToName: replyingTo.value?.nickname || null,
      createdAt: new Date().toISOString(),
    }
    if (!post.value.comments) post.value.comments = []
    post.value.comments.push(newComment)
    post.value.commentCount = (post.value.commentCount || 0) + 1
    commentText.value = ''
    commentImages.value = []
    replyingTo.value = null
    ElMessage.success('评论成功')
  } catch {
    ElMessage.error('评论失败')
  } finally {
    submitting.value = false
  }
}

let aiTagPollTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  if (!isPostIdValid.value) {
    ElMessage.error('动态不存在或链接无效')
    router.replace('/discover')
    loading.value = false
    return
  }
  try {
    const res = await getPostDetail(postId.value)
    post.value = res.data.data || null
    if (post.value) {
      const reported = await checkReported('POST', post.value.id)
      postReported.value = !!reported.data.data
      // 作者自己的帖子且无 AI 标签时，轮询等待标签生成（最多 30 秒）
      if (userStore.user?.id === post.value.userId && !post.value.aiTags) {
        let pollCount = 0
        aiTagPollTimer = setInterval(async () => {
          pollCount++
          if (pollCount > 10 || !isPostIdValid.value) {
            if (aiTagPollTimer) clearInterval(aiTagPollTimer)
            aiTagPollTimer = null
            return
          }
          try {
            const r = await getPostDetail(postId.value)
            const p = r.data.data
            if (p?.aiTags && post.value) {
              post.value.aiTags = p.aiTags
              if (aiTagPollTimer) clearInterval(aiTagPollTimer)
              aiTagPollTimer = null
            }
          } catch {
            // 忽略轮询错误（如 postId 变为无效）
          }
        }, 3000)
      }
    }
  } catch {
    post.value = null
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  if (aiTagPollTimer) {
    clearInterval(aiTagPollTimer)
    aiTagPollTimer = null
  }
})
</script>

<style lang="scss" scoped>
.feed-detail-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 16px;
  padding-bottom: 80px;
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
  border: none;
  cursor: pointer;
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

.post-user { flex: 1; }
.post-name { font-weight: 600; font-size: 15px; cursor: pointer; }
.post-time { font-size: 12px; color: var(--el-text-color-secondary); cursor: default; }

.report-btn {
  padding: 4px;
  &.reported {
    color: var(--el-color-primary);
  }
  background: none;
  border: none;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  &:hover { color: var(--el-color-warning); }
}

.delete-btn {
  padding: 4px;
  background: none;
  border: none;
  cursor: pointer;
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

.post-ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.post-ai-tags .ai-tag {
  font-size: 12px;
  color: var(--el-color-primary);
  background: rgba(var(--el-color-primary-rgb), 0.08);
  padding: 2px 8px;
  border-radius: 4px;
}

.retag-hint {
  margin-bottom: 12px;
}

.retag-btn {
  font-size: 13px;
  color: var(--el-color-primary);
  background: rgba(var(--el-color-primary-rgb), 0.08);
  border: 1px dashed var(--el-color-primary);
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.retag-btn:hover:not(:disabled) {
  background: rgba(var(--el-color-primary-rgb), 0.15);
}

.retag-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.post-videos {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.post-video {
  width: 100%;
  max-width: 400px;
  border-radius: 8px;
  background-color: #000;
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
  &.active { color: $primary; }
  &:hover:not(:disabled) { color: var(--el-color-primary); }
}

/* ===== 评论区：虎扑/小红书风格 ===== */
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
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.comment-thread {
  padding: 16px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: none;
  }
}

/* 根评论 */
.root-comment {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  cursor: pointer;
  &:hover { color: var(--el-color-primary); }
}

.comment-floor {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color-light);
  padding: 1px 6px;
  border-radius: 4px;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  word-break: break-word;
  white-space: pre-wrap;
  cursor: default;
}

.comment-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;

  .comment-img {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    object-fit: cover;
  }
}

.comment-images-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;

  .comment-preview-item {
    position: relative;
    width: 60px;
    height: 60px;

    .comment-preview-img {
      width: 100%;
      height: 100%;
      border-radius: 8px;
      object-fit: cover;
    }

    .comment-preview-remove {
      position: absolute;
      top: -6px;
      right: -6px;
      width: 20px;
      height: 20px;
      padding: 0;
      border-radius: 50%;
      background: var(--el-color-danger);
      color: white;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
    }
  }
}

.hidden-file-input {
  display: none;
}

.input-row .icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  border-radius: 8px;
  flex-shrink: 0;

  &:hover {
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
  }
}

.comment-deleted .comment-text,
.comment-deleted .reply-text {
  color: var(--el-text-color-placeholder);
  font-style: italic;
}

.delete-comment-btn {
  color: var(--el-color-danger);
}

.delete-comment-btn:hover {
  color: var(--el-color-danger);
}

.comment-deleted .comment-author,
.comment-deleted .reply-author {
  opacity: 0.8;
}

.comment-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.comment-time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.reply-btn {
  padding: 2px 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: var(--el-color-primary);
  }
}

/* 子回复列表 */
.reply-list {
  margin-top: 12px;
  margin-left: 48px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reply-item {
  display: flex;
  gap: 8px;
}

.reply-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-content {
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.reply-author {
  font-weight: 600;
  color: var(--el-text-color-primary);
  cursor: pointer;
  &:hover { color: var(--el-color-primary); }
}

.reply-arrow {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.reply-target {
  color: var(--el-color-primary);
  font-weight: 500;
}

.reply-text {
  color: var(--el-text-color-primary);
}

.reply-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.expand-replies-btn,
.expand-threads-btn {
  padding: 6px 0;
  font-size: 13px;
  color: var(--el-color-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  font-weight: 500;
  text-align: left;

  &:hover {
    text-decoration: underline;
  }
}

.expand-threads-btn {
  display: block;
  padding: 12px 0;
  text-align: center;
}

.comments-empty {
  text-align: center;
  color: var(--el-text-color-placeholder);
  padding: 32px;
  font-size: 14px;
}

/* 底部固定输入框 */
.comment-input-bar {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.replying-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(var(--el-color-primary-rgb, 64, 158, 255), 0.08);
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-color-primary);
}

.reply-cancel {
  margin-left: auto;
  padding: 2px 6px;
  background: transparent;
  border: none;
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;

  &:hover {
    color: var(--el-text-color-secondary);
  }
}

.input-row {
  display: flex;
  gap: 8px;

  .el-input {
    flex: 1;
  }
}

.send-btn {
  padding: 0 20px;
  height: 32px;
  background: var(--el-color-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:hover:not(:disabled) {
    opacity: 0.9;
  }
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
