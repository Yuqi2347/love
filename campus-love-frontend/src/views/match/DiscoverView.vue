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
        <button
          :class="['tab-btn', { active: activeTab === 'liked' }]"
          @click="switchTab('liked')"
        >
          点赞
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
              type="button"
              class="feed-report-btn"
              title="举报"
              @click.stop="handleReport(item.post.id, 'POST')"
            >
              <el-icon><Flag /></el-icon>
            </button>
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
          <div class="feed-content">
            <template v-if="shouldCollapse(item.post.content)">
              <span v-if="isExpanded(item.post.id)">{{ item.post.content }}</span>
              <span v-else>{{ getDisplayContent(item.post.content, item.post.id) }}</span>
              <button class="expand-btn" @click="toggleExpand(item.post.id)">
                {{ isExpanded(item.post.id) ? '收起' : '显示更多' }}
              </button>
            </template>
            <span v-else>{{ item.post.content }}</span>
          </div>
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
            <button class="action-btn" @click="goPostDetail(item.post.id)">
              <span class="action-icon">💬</span>
              <span>{{ item.post.commentCount }}</span>
            </button>
            <button class="action-btn" @click="openShareDialog(item.post)">
              <span class="action-icon">🔗</span>
              <span>分享</span>
            </button>
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

        <!-- 可见范围 -->
        <el-form-item label="可见范围">
          <el-select v-model="postVisibility" placeholder="选择可见范围" style="width: 100%">
            <el-option label="所有人" value="ALL" />
            <el-option label="关注我的人" value="FOLLOWERS" />
            <el-option label="朋友" value="FRIENDS" />
            <el-option label="仅自己" value="SELF" />
          </el-select>
        </el-form-item>
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

    <!-- 分享弹窗 -->
    <ShareDialog
      v-model:show="showShareDialog"
      :post="currentSharePost"
      @success="handleShareSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getDiscoveryPosts,
  getTimeline,
  getLikedPosts,
  likePost,
  unlikePost,
  getLevelInfo,
  createDiscoveryPost,
  deletePost,
  uploadImage,
  uploadVideo,
  type FeedPost,
  type UserLevelInfo,
} from '@/api/feedApi'
import { submitReport } from '@/api/reportApi'
import { useUserStore } from '@/store/userStore'
import { useInviteStore } from '@/store/inviteStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Search, Flag } from '@element-plus/icons-vue'
import type { Invite } from '@/api/inviteApi'
import ShareDialog from '@/components/ShareDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const inviteStore = useInviteStore()
const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const posts = ref<FeedPost[]>([])
const followingPosts = ref<FeedPost[]>([])
const likedPosts = ref<FeedPost[]>([])
// 帖子展开状态 Map
const expandedPosts = ref<Map<number, boolean>>(new Map())
// 文字内容折叠配置
const CONTENT_MAX_LENGTH = 100

const levelInfo = ref<UserLevelInfo | null>(null)
const searchKeyword = ref('')
const activeTab = ref<'recommend' | 'post' | 'following' | 'liked'>('recommend')
const showPostDialog = ref(false)
const postContent = ref('')
const postVisibility = ref('ALL')
const posting = ref(false)

// 多媒体上传状态
const uploadedImages = ref<string[]>([])
const uploadedVideos = ref<string[]>([])
const linkPreview = ref<{ url: string; title: string; image: string }>({ url: '', title: '', image: '' })
const linkUrlInput = ref('')
const showLinkInput = ref(false)
const imageInputRef = ref<HTMLInputElement>()
const videoInputRef = ref<HTMLInputElement>()

// 分享相关状态
const showShareDialog = ref(false)
const currentSharePost = ref<FeedPost | null>(null)

function openShareDialog(post: FeedPost) {
  currentSharePost.value = post
  showShareDialog.value = true
}

function handleShareSuccess() {
  // 分享成功后的回调，可以刷新页面等
  console.log('分享成功')
}

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

function switchTab(tab: 'recommend' | 'post' | 'following' | 'liked') {
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
  } else if (activeTab.value === 'liked') {
    await loadLikedPosts()
  } else {
    await loadPosts(kw)
  }
}

async function loadLikedPosts() {
  try {
    const res = await getLikedPosts(0, 20)
    likedPosts.value = res.data.data || []
  } catch { /* empty */ }
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
  let postList: FeedPost[] = []
  if (activeTab.value === 'following') {
    postList = followingPosts.value
  } else if (activeTab.value === 'liked') {
    postList = likedPosts.value
  } else {
    postList = posts.value
  }
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
    || likedPosts.value.find(p => p.id === postId)
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
      // 从点赞列表中移除
      if (activeTab.value === 'liked') {
        likedPosts.value = likedPosts.value.filter(p => p.id !== postId)
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
      linkImage: linkPreview.value.image || undefined,
      visibility: postVisibility.value || 'ALL'
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
  postVisibility.value = 'ALL'
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

// 检查帖子是否已展开
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

async function handleReport(targetId: number, targetType: string) {
  try {
    const { value } = await ElMessageBox.prompt('请输入举报理由', '举报', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入举报理由',
    })
    if (value) {
      await submitReport({ targetType, targetId, reason: value })
      ElMessage.success('举报已提交')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('举报失败')
  }
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

.feed-report-btn {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  &:hover { color: var(--el-color-warning); }
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
  white-space: pre-wrap;
  word-break: break-word;
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

  &:hover {
    background: #e8eaed;
  }
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
