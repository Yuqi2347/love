<template>
  <div class="discover-page">
    <div class="page-header">
      <!-- 顶部搜索与发布 -->
      <div class="top-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索邀约、帖子..."
          class="search-input"
          clearable
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <button class="btn-primary post-btn" @click="showPostDialog = true">
          <el-icon><Plus /></el-icon> 发布
        </button>
      </div>

      <!-- Tab 模块切换 -->
      <div class="discover-tabs">
        <button
          :class="['tab-btn', { active: activeTab === 'recommend' }]"
          @click="switchTab('recommend')"
        >
          推荐
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'post' }]"
          @click="switchTab('post')"
        >
          动态
        </button>
        <button
          :class="['tab-btn', { active: activeTab === 'following' }]"
          @click="switchTab('following')"
        >
          关注
        </button>
      </div>
    </div>

    <!-- 统一信息流：动态 -->
    <div v-if="timelineItems.length" class="timeline-list">
      <div
        v-for="item in timelineItems"
        :key="item.key"
        class="timeline-item"
      >
        <!-- 社交动态：白色卡片 -->
        <div class="feed-card card" @click="goPostDetail(item.post.id)">
          <div class="feed-header" @click.stop>
            <img
              :src="item.post.avatarUrl || defaultAvatar"
              class="feed-avatar"
              @click="$router.push(`/profile/${item.post.userId}`)"
            />
            <div class="feed-user" @click="$router.push(`/profile/${item.post.userId}`)">
              <div class="feed-name">{{ item.post.nickname }}</div>
              <div class="feed-time">{{ formatTime(item.post.createdAt) }}</div>
            </div>
            <button
              v-if="canDeletePost(item.post)"
              type="button"
              class="feed-delete-btn"
              title="删除"
              @click.stop="handleDeletePost(item.post.id)"
            >
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </button>
          </div>
          <div class="feed-content">{{ item.post.content }}</div>
          <div v-if="item.post.images" class="feed-images" @click.stop>
            <img
              v-for="(img, idx) in item.post.images.split(',').slice(0, 3)"
              :key="idx"
              :src="getMediaUrl(img)"
              class="feed-image"
            />
          </div>
          <!-- 视频展示 -->
          <div v-if="item.post.videos" class="feed-videos" @click.stop>
            <video
              v-for="(video, idx) in item.post.videos.split(',')"
              :key="idx"
              :src="getMediaUrl(video)"
              class="feed-video"
              controls
            />
          </div>
          <!-- 链接预览 -->
          <div v-if="item.post.linkUrl" class="feed-link" @click.stop>
            <a :href="item.post.linkUrl" target="_blank" class="link-card">
              <img v-if="item.post.linkImage" :src="getMediaUrl(item.post.linkImage)" class="link-image" />
              <div class="link-content">
                <div class="link-title">{{ item.post.linkTitle || item.post.linkUrl }}</div>
                <div class="link-url">{{ getDomain(item.post.linkUrl) }}</div>
              </div>
            </a>
          </div>
          <div class="feed-actions" @click.stop>
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
          </div>

          <!-- 评论叠楼列表（树形线程 + 折叠） -->
          <div v-if="item.post.comments && item.post.comments.length" class="comment-list" @click.stop>
            <div
              v-for="thread in getDisplayedThreads(item.post.id, item.post.comments)"
              :key="thread.comment.id"
              class="comment-thread"
            >
              <!-- 主评论 -->
              <div class="comment-item">
                <div class="comment-header">
                  <span class="comment-author" @click.stop="$router.push(`/profile/${thread.comment.userId}`)">
                    {{ thread.comment.nickname }}
                  </span>
                  <span class="comment-time">{{ formatTime(thread.comment.createdAt) }}</span>
                  <button class="comment-reply-btn" @click.stop="handleReplyClick(item.post.id, thread.comment)">
                    回复
                  </button>
                </div>
                <div class="comment-text">{{ thread.comment.content }}</div>
              </div>
              <!-- 回复列表 -->
              <div v-if="thread.replies.length" class="thread-replies">
                <div
                  v-for="reply in getDisplayedReplies(thread.comment.id, thread.replies)"
                  :key="reply.id"
                  class="comment-item is-reply"
                >
                  <div class="comment-header">
                    <span class="comment-author" @click.stop="$router.push(`/profile/${reply.userId}`)">
                      {{ reply.nickname }}
                    </span>
                    <span v-if="reply.repliedToName" class="reply-indicator">回复</span>
                    <span v-if="reply.repliedToName" class="replied-name">@{{ reply.repliedToName }}</span>
                    <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                    <button class="comment-reply-btn" @click.stop="handleReplyClick(item.post.id, reply)">
                      回复
                    </button>
                  </div>
                  <div class="comment-text">{{ reply.content }}</div>
                </div>
                <button
                  v-if="thread.replies.length > MAX_REPLIES_PER_THREAD && !expandedReplyThreads.has(thread.comment.id)"
                  class="expand-btn"
                  @click.stop="toggleExpandReplies(thread.comment.id)"
                >
                  展开 {{ thread.replies.length - MAX_REPLIES_PER_THREAD }} 条回复
                </button>
                <button
                  v-else-if="expandedReplyThreads.has(thread.comment.id) && thread.replies.length > MAX_REPLIES_PER_THREAD"
                  class="expand-btn"
                  @click.stop="toggleExpandReplies(thread.comment.id)"
                >
                  收起回复
                </button>
              </div>
            </div>
            <!-- 展开更多评论 -->
            <button
              v-if="getHiddenThreadCount(item.post.id, item.post.comments) > 0 && !expandedCommentPosts.has(item.post.id)"
              class="expand-btn"
              @click.stop="toggleExpandComments(item.post.id)"
            >
              展开 {{ getHiddenThreadCount(item.post.id, item.post.comments) }} 条评论
            </button>
            <button
              v-else-if="expandedCommentPosts.has(item.post.id) && buildCommentThreads(item.post.comments).length > MAX_ROOT_COMMENTS"
              class="expand-btn"
              @click.stop="toggleExpandComments(item.post.id)"
            >
              收起评论
            </button>
          </div>

          <div v-if="commentingPostId === item.post.id" class="comment-input" @click.stop>
            <!-- 回复提示 -->
            <div v-if="replyingTo && replyingTo.postId === item.post.id" class="replying-hint">
              <span>回复 @{{ replyingTo.nickname }}</span>
              <button class="reply-cancel" @click="cancelReply">✕</button>
            </div>
            <el-input
              v-model="commentText"
              :placeholder="replyingTo && replyingTo.postId === item.post.id ? `回复 @${replyingTo.nickname}...` : '写评论...'"
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
    <el-dialog v-model="showPostDialog" title="发布动态" width="560px" :close-on-click-modal="false">
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

        <!-- 多媒体上传区域 -->
        <div class="post-media-section">
          <!-- 已上传的图片预览 -->
          <div v-if="uploadedImages.length" class="media-preview-grid">
            <div v-for="(img, idx) in uploadedImages" :key="idx" class="media-preview-item">
              <img :src="getMediaUrl(img)" class="preview-img" />
              <button type="button" class="preview-remove" @click="removeImage(idx)">
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 已上传的视频预览 -->
          <div v-if="uploadedVideos.length" class="media-preview-grid">
            <div v-for="(video, idx) in uploadedVideos" :key="idx" class="media-preview-item">
              <video :src="getMediaUrl(video)" class="preview-video" />
              <button type="button" class="preview-remove" @click="removeVideo(idx)">
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 链接预览 -->
          <div v-if="linkPreview.url" class="link-preview-card">
            <img v-if="linkPreview.image" :src="getMediaUrl(linkPreview.image)" class="link-preview-img" />
            <div class="link-preview-content">
              <div class="link-preview-title">{{ linkPreview.title || linkPreview.url }}</div>
              <button type="button" class="link-preview-remove" @click="clearLink">
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 上传按钮 -->
          <div class="media-actions">
            <input ref="imageInputRef" type="file" accept="image/*" multiple hidden @change="handleImageSelect" />
            <button type="button" class="media-btn" @click="imageInputRef?.click()">
              <el-icon><Picture /></el-icon>
              <span>图片</span>
            </button>

            <input ref="videoInputRef" type="file" accept="video/*" hidden @change="handleVideoSelect" />
            <button type="button" class="media-btn" @click="videoInputRef?.click()">
              <el-icon><VideoCamera /></el-icon>
              <span>视频</span>
            </button>

            <button type="button" class="media-btn" @click="showLinkInput = !showLinkInput">
              <el-icon><Link /></el-icon>
              <span>链接</span>
            </button>
          </div>

          <!-- 链接输入 -->
          <div v-if="showLinkInput" class="link-input-wrapper">
            <el-input v-model="linkUrlInput" placeholder="粘贴链接 (https://...)" @keyup.enter="handleAddLink" />
            <button type="button" class="btn-link-add" @click="handleAddLink">添加</button>
          </div>
        </div>

      </el-form>
      <template #footer>
        <el-button @click="closePostDialog">取消</el-button>
        <el-button type="primary" :disabled="posting || (!postContent.trim() && !uploadedImages.length && !uploadedVideos.length && !linkPreview.url)" @click="handlePost">
          {{ posting ? '发布中...' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <div v-if="!timelineItems.length" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无内容</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getDiscoveryPosts,
  getTimeline,
  likePost,
  unlikePost,
  getLevelInfo,
  createDiscoveryPost,
  deletePost,
  addComment,
  uploadImage,
  uploadVideo,
  type FeedPost,
  type FeedComment,
  type UserLevelInfo,
} from '@/api/feedApi'
import { useUserStore } from '@/store/userStore'
import { useInviteStore } from '@/store/inviteStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Search } from '@element-plus/icons-vue'
import type { Invite } from '@/api/inviteApi'
import {
  InviteType,
  InviteStatus,
  InviteMode,
  INVITE_TYPE_LABELS,
  INVITE_STATUS_LABELS,
  INVITE_STATUS_COLORS,
  formatInviteTime,
} from '@/constants/inviteConst'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const inviteStore = useInviteStore()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const posts = ref<FeedPost[]>([])
const followingPosts = ref<FeedPost[]>([])
const levelInfo = ref<UserLevelInfo | null>(null)
const searchKeyword = ref('')
const activeTab = ref<'recommend' | 'post' | 'following'>('recommend')
const showPostDialog = ref(false)
const postContent = ref('')
const posting = ref(false)
const commentingPostId = ref<number | null>(null)
const commentText = ref('')
const replyingTo = ref<{ postId: number; commentId: number; nickname: string } | null>(null)

// 评论折叠状态：记录哪些帖子展开了全部评论
const expandedCommentPosts = ref<Set<number>>(new Set())
// 展开回复的线程 ID
const expandedReplyThreads = ref<Set<number>>(new Set())

// 最大显示数
const MAX_ROOT_COMMENTS = 3
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

function getDisplayedThreads(postId: number, comments: FeedComment[]) {
  const threads = buildCommentThreads(comments)
  const expanded = expandedCommentPosts.value.has(postId)
  return expanded ? threads : threads.slice(0, MAX_ROOT_COMMENTS)
}

function getDisplayedReplies(threadId: number, replies: FeedComment[]) {
  const expanded = expandedReplyThreads.value.has(threadId)
  return expanded ? replies : replies.slice(0, MAX_REPLIES_PER_THREAD)
}

function getHiddenThreadCount(postId: number, comments: FeedComment[]) {
  const total = buildCommentThreads(comments).length
  return Math.max(0, total - MAX_ROOT_COMMENTS)
}

function toggleExpandComments(postId: number) {
  if (expandedCommentPosts.value.has(postId)) {
    expandedCommentPosts.value.delete(postId)
  } else {
    expandedCommentPosts.value.add(postId)
  }
}

function toggleExpandReplies(threadId: number) {
  if (expandedReplyThreads.value.has(threadId)) {
    expandedReplyThreads.value.delete(threadId)
  } else {
    expandedReplyThreads.value.add(threadId)
  }
}

// 多媒体上传状态
const uploadedImages = ref<string[]>([])
const uploadedVideos = ref<string[]>([])
const linkPreview = ref<{ url: string; title: string; image: string }>({ url: '', title: '', image: '' })
const linkUrlInput = ref('')
const showLinkInput = ref(false)
const imageInputRef = ref<HTMLInputElement>()
const videoInputRef = ref<HTMLInputElement>()

function goPostDetail(postId: number) {
  router.push(`/feed/${postId}`)
}

// 判断是否可以删除帖子（管理员或帖子作者）
function canDeletePost(post: FeedPost): boolean {
  const isAdmin = userStore.user?.isAdmin || false
  const isOwner = post.userId === userStore.user?.id
  return isAdmin || isOwner
}

// 从 URL 读取邀约类型筛选（热门邀约看板跳转时传入）
function getInviteTypeFromRoute(): string | undefined {
  const t = route.query.type
  return typeof t === 'string' && t ? t : undefined
}

function switchTab(tab: 'recommend' | 'post' | 'following') {
  activeTab.value = tab
  loadByTab()
}

function doSearch() {
  loadByTab()
}

async function loadByTab() {
  const kw = searchKeyword.value.trim() || undefined
  if (activeTab.value === 'recommend') {
    await loadPosts(kw)
  } else if (activeTab.value === 'following') {
    await loadFollowingPosts()
  } else {
    await loadPosts(kw)
  }
}

onMounted(async () => {
  await loadLevelInfo()
  await loadByTab()
})

// 路由 query.type 变化时重新拉取邀约（如从热门看板切换类型）
watch(() => route.query.type, () => {
  loadInvites(searchKeyword.value.trim() || undefined)
})

type TimelineItem =
  | { kind: 'invite'; invite: Invite; time: string; key: string }
  | { kind: 'post'; post: FeedPost; time: string; key: string }

const timelineItems = computed<TimelineItem[]>(() => {
  const postList = activeTab.value === 'following' ? followingPosts.value : posts.value
  const postItems: TimelineItem[] = postList.map(post => ({
    kind: 'post',
    post,
    time: post.createdAt,
    key: `post-${post.id}`,
  }))

  return postItems.sort((a, b) => {
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

async function loadPosts(keyword?: string) {
  try {
    const res = await getDiscoveryPosts(0, 20, keyword)
    posts.value = res.data.data || []
  } catch { /* empty */ }
}

async function loadFollowingPosts() {
  try {
    const res = await getTimeline(0, 20)
    followingPosts.value = res.data.data || []
  } catch { /* empty */ }
}

async function loadInvites(keyword?: string) {
  try {
    const type = getInviteTypeFromRoute()
    await inviteStore.fetchInvites(type, undefined, 'year', keyword, true)
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

// 统一从当前 tab 对应的帖子数组中查找（解决关注tab下找不到帖子的bug）
function findPostById(postId: number): FeedPost | undefined {
  return posts.value.find(p => p.id === postId)
    || followingPosts.value.find(p => p.id === postId)
}

async function handleLike(postId: number, liked: boolean) {
  try {
    if (liked) {
      await unlikePost(postId)
      const post = findPostById(postId)
      if (post) {
        post.liked = false
        post.likeCount--
      }
    } else {
      await likePost(postId)
      const post = findPostById(postId)
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
  // 如果切换到其他帖子，清除回复状态
  if (replyingTo.value && replyingTo.value.postId !== post.id) {
    replyingTo.value = null
  }
}

function handleReplyClick(postId: number, comment: FeedComment) {
  if (commentingPostId.value !== postId) {
    commentingPostId.value = postId
  }
  replyingTo.value = { postId, commentId: comment.id, nickname: comment.nickname }
  commentText.value = ''
  // 聚焦到输入框
  document.querySelector('.comment-input input')?.focus()
}

function cancelReply() {
  replyingTo.value = null
  commentText.value = ''
}

async function submitComment(postId: number) {
  if (!commentText.value.trim()) {
    return
  }
  try {
    const content = commentText.value.trim()
    const parentId = replyingTo.value ? replyingTo.value.commentId : null
    // 获取被回复用户的ID
    let repliedUserId = null
    if (replyingTo.value) {
      const parentComment = findPostById(postId)?.comments?.find(c => c.id === replyingTo.value.commentId)
      repliedUserId = parentComment?.userId || null
    }

    await addComment({ postId, content, parentId, repliedUserId })
    const post = findPostById(postId)
    if (post) {
      post.commentCount = (post.commentCount || 0) + 1
      // 本地追加一条评论，用于”叠楼”即时展示
      if (!post.comments) {
        post.comments = []
      }
      const optimistic: FeedComment = {
        id: Date.now(),
        userId: userStore.user!.id,
        nickname: userStore.user!.nickname,
        avatarUrl: userStore.user!.avatarUrl,
        content,
        parentId: parentId,
        repliedToName: replyingTo.value?.nickname || null,
        createdAt: new Date().toISOString(),
      }
      post.comments.push(optimistic)
    }
    commentText.value = ''
    replyingTo.value = null
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
  if (!postContent.value.trim() && !uploadedImages.value.length && !uploadedVideos.value.length && !linkPreview.value.url) {
    ElMessage.warning('请输入内容或添加媒体')
    return
  }

  posting.value = true
  try {
    const res = await createDiscoveryPost({
      content: postContent.value.trim(),
      images: uploadedImages.value.length ? uploadedImages.value.join(',') : undefined,
      videos: uploadedVideos.value.length ? uploadedVideos.value.join(',') : undefined,
      linkUrl: linkPreview.value.url || undefined,
      linkTitle: linkPreview.value.title || undefined,
      linkImage: linkPreview.value.image || undefined
    })
    posts.value.unshift(res.data.data)
    resetPostForm()
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

function resetPostForm() {
  postContent.value = ''
  uploadedImages.value = []
  uploadedVideos.value = []
  linkPreview.value = { url: '', title: '', image: '' }
  linkUrlInput.value = ''
  showLinkInput.value = false
}

function closePostDialog() {
  showPostDialog.value = false
  resetPostForm()
}

// 图片上传
async function handleImageSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files) return

  for (const file of Array.from(files)) {
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过10MB')
      continue
    }
    try {
      ElMessage.info('上传中...')
      const res = await uploadImage(file)
      // 后端返回 data 字段为 "/uploads/xxx.jpg"，存储原始路径，显示时用 getMediaUrl() 加前缀
      const imagePath = res.data.data
      if (imagePath) uploadedImages.value.push(imagePath)
      ElMessage.success('图片上传成功')
    } catch (err) {
      ElMessage.error('图片上传失败')
    }
  }
  target.value = ''
}

function removeImage(index: number) {
  uploadedImages.value.splice(index, 1)
}

// 视频上传
async function handleVideoSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 100 * 1024 * 1024) {
    ElMessage.warning('视频大小不能超过100MB')
    return
  }

  try {
    ElMessage.info('视频上传中，请稍候...')
    const res = await uploadVideo(file)
    const videoPath = res.data.data
    if (videoPath) uploadedVideos.value.push(videoPath)
    ElMessage.success('视频上传成功')
  } catch (err) {
    ElMessage.error('视频上传失败')
  }
  target.value = ''
}

function removeVideo(index: number) {
  uploadedVideos.value.splice(index, 1)
}

// 链接处理
function handleAddLink() {
  const url = linkUrlInput.value.trim()
  if (!url) return

  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    ElMessage.warning('请输入有效的链接（以 http:// 或 https:// 开头）')
    return
  }

  linkPreview.value = {
    url,
    title: url,
    image: ''
  }
  linkUrlInput.value = ''
  showLinkInput.value = false
}

function clearLink() {
  linkPreview.value = { url: '', title: '', image: '' }
}

function getDomain(url: string) {
  try {
    const domain = new URL(url).hostname
    return domain.replace('www.', '')
  } catch {
    return url
  }
}

function getMediaUrl(url: string | null): string {
  if (!url) return ''
  // 如果是完整 URL 或已包含 /api 前缀，直接返回
  if (url.startsWith('http') || url.startsWith('/api')) {
    return url
  }
  // 添加 /api 前缀
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}

async function handleDeletePost(postId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条动态？', '提示', { type: 'warning' })
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    ElMessage.success('已删除')
  } catch {
    // 用户取消或删除失败
  }
}
</script>

<style lang="scss" scoped>
@use 'sass:color';
.discover-page { padding: 0; }

.page-header {
  padding: 16px 24px 0;
  border-bottom: 1px solid $border-light;
  position: sticky;
  top: 0;
  background: rgba($bg-primary, 0.9);
  backdrop-filter: blur(12px);
  z-index: 10;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  width: 100%;
}

.search-input {
  width: 280px;

  :deep(.el-input__wrapper) {
    border-radius: $radius-full;
    box-shadow: 0 0 0 1px $border-light;
  }
}

.post-btn {
  padding: 8px 20px;
  border-radius: $radius-full;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.discover-tabs {
  display: flex;
  gap: 8px;
  padding-bottom: 12px;
  width: 100%;
}

.tab-btn {
  padding: 8px 20px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  border-radius: $radius-full;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover {
    background: rgba($primary, 0.08);
    color: $primary;
  }

  &.active {
    background: $primary;
    color: white;
  }
}

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
  background: $bg-primary;
  border: none;
  border-radius: $radius-xl;
  padding: 16px;
  cursor: pointer;
  box-shadow: $shadow-sm;
  transition: transform $transition-fast, box-shadow $transition-fast;

  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-2px);
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

  .meta-item-clickable {
    cursor: pointer;

    &:hover { color: $primary; }
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
  gap: 16px;
  padding: 16px 24px;
}

.feed-card {
  background: $bg-primary;
  border-radius: $radius-xl;
  padding: 20px;
  cursor: pointer;
  border: none;
  box-shadow: $shadow-sm;
  transition: transform $transition-fast, box-shadow $transition-fast;

  &:hover {
    box-shadow: $shadow-md;
    transform: translateY(-2px);
  }
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
  flex: 1;
  cursor: pointer;
}

.feed-delete-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: 13px;
  cursor: pointer;
  border-radius: $radius-md;
  transition: color $transition-fast, background $transition-fast;

  &:hover {
    color: $danger;
    background: rgba($danger, 0.08);
  }
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
  gap: 8px;
}

.comment-thread {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.thread-replies {
  margin-left: 16px;
  padding-left: 10px;
  border-left: 2px solid rgba($primary, 0.15);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.expand-btn {
  padding: 4px 10px;
  font-size: 12px;
  color: $primary;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-weight: 500;
  transition: color $transition-fast;

  &:hover {
    color: darken($primary, 10%);
    text-decoration: underline;
  }
}

.comment-item {
  font-size: 13px;
  line-height: 1.5;
  padding: 6px 10px;
  background: $bg-tertiary;
  border-radius: $radius-md;

  &.is-reply {
    background: rgba($bg-tertiary, 0.6);
  }

  .comment-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 2px;
  }

  .comment-author {
    font-weight: 600;
    color: $primary;
    cursor: pointer;
  }

  .reply-indicator {
    font-size: 11px;
    color: $text-muted;
  }

  .replied-name {
    font-size: 12px;
    color: $primary;
    font-weight: 500;
  }

  .comment-time {
    font-size: 12px;
    color: $text-muted;
  }

  .comment-reply-btn {
    margin-left: auto;
    padding: 1px 6px;
    font-size: 11px;
    color: $text-muted;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: color $transition-fast;

    &:hover {
      color: $primary;
    }
  }

  .comment-text {
    color: $text-primary;
    white-space: pre-wrap;
  }
}

.comment-input {
  margin-top: 8px;

  .replying-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 8px;
    background: rgba($primary, 0.08);
    border-radius: $radius-sm;
    margin-bottom: 6px;
    font-size: 12px;
    color: $primary;
  }

  .reply-cancel {
    margin-left: auto;
    padding: 0 4px;
    background: transparent;
    border: none;
    color: $text-muted;
    cursor: pointer;
    font-size: 13px;
    line-height: 1;

    &:hover {
      color: $text-secondary;
    }
  }

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

// 多媒体上传样式
.post-media-section {
  margin-top: 12px;
}

.media-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.media-preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: $bg-tertiary;

  .preview-img,
  .preview-video {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .preview-remove {
    position: absolute;
    top: 4px;
    right: 4px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.6);
    color: white;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover { background: rgba(245, 34, 45, 0.9); }
  }
}

.link-preview-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: $bg-tertiary;
  border-radius: 8px;
  margin-bottom: 12px;

  .link-preview-img {
    width: 50px;
    height: 50px;
    border-radius: 6px;
    object-fit: cover;
    flex-shrink: 0;
  }

  .link-preview-content {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 6px;
  }

  .link-preview-title {
    flex: 1;
    font-size: 13px;
    font-weight: 500;
    word-break: break-all;
  }

  .link-preview-remove {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.1);
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover { background: rgba(245, 34, 45, 0.2); }
  }
}

.media-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.media-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: $bg-tertiary;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($primary, 0.1);
    color: $primary;
    border-color: $primary;
  }
}

.link-input-wrapper {
  display: flex;
  gap: 8px;
  margin-top: 8px;

  .el-input { flex: 1; }
}

.btn-link-add {
  padding: 6px 14px;
  background: $primary;
  color: white;
  border: none;
  border-radius: $radius-md;
  font-size: 13px;
  cursor: pointer;

  &:hover { opacity: 0.9; }
}

// 动态列表中的视频和链接样式
.feed-videos {
  margin: 8px 0;
}

.feed-video {
  width: 100%;
  max-height: 300px;
  border-radius: $radius-md;
  object-fit: contain;
  background: #000;
}

.feed-link {
  margin: 8px 0;
}

.link-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: $bg-tertiary;
  border-radius: $radius-md;
  text-decoration: none;
  transition: all 0.2s;

  &:hover { background: color.adjust($bg-tertiary, $lightness: -5%); }
}

.link-image {
  width: 60px;
  height: 60px;
  border-radius: $radius-sm;
  object-fit: cover;
  flex-shrink: 0;
}

.link-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.link-title {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.link-url {
  font-size: 11px;
  color: $text-muted;
}
</style>
