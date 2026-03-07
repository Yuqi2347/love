<template>
  <div class="main-layout">
    <!-- Left Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-inner">
        <router-link to="/" class="logo-link">
          <span class="logo-icon">💕</span>
          <span class="logo-text">CampusLove</span>
        </router-link>

        <nav class="nav-menu">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.showDot" class="nav-dot" />
          </router-link>
        </nav>

        <button class="btn-primary post-btn" @click="showPostDialog = true">
          <el-icon><EditPen /></el-icon>
          发布动态
        </button>

        <div v-if="userStore.user" class="sidebar-user" @click="$router.push('/profile')">
          <img :src="userStore.user.avatarUrl || defaultAvatar" class="avatar" width="40" height="40" />
          <div class="user-info">
            <div class="user-name text-ellipsis">{{ userStore.user.nickname }}</div>
            <div class="user-email text-ellipsis">{{ userStore.user.email }}</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="content-area">
      <router-view />
    </main>

    <!-- Right Panel -->
    <aside class="right-panel">
      <div ref="searchBoxRef" class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户昵称..."
          prefix-icon="Search"
          size="large"
          round
          clearable
          :loading="searchLoading"
          @focus="searchKeyword && doSearch()"
          @clear="clearSearch"
        />
        <div v-if="showSearchDropdown" class="search-dropdown">
          <div v-if="searchLoading" class="search-loading">搜索中...</div>
          <div v-else-if="searchKeyword.trim().length < 2" class="search-hint">输入至少 2 个字符搜索</div>
          <div v-else-if="!searchResults.length" class="search-empty">未找到用户</div>
          <div
            v-for="u in searchResults"
            v-else
            :key="u.id"
            class="search-item"
            @click="goToProfile(u.id)"
          >
            <img :src="u.avatarUrl || defaultAvatar" class="avatar" width="36" height="36" />
            <span class="search-item-name text-ellipsis">{{ u.nickname }}</span>
            <button
              :class="['btn-sm', searchFollowedIds.includes(u.id) ? 'btn-followed' : 'btn-outline']"
              @click.stop="handleSearchFollow(u.id)"
            >
              {{ searchFollowedIds.includes(u.id) ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
      </div>

      <div class="panel-card">
        <h3 class="panel-title">今日推荐</h3>
        <div v-if="topMatches.length" class="recommend-list">
          <div
            v-for="m in topMatches"
            :key="m.userId"
            class="recommend-item"
            @click="$router.push(`/profile/${m.userId}`)"
          >
            <img :src="m.avatarUrl || defaultAvatar" class="avatar" width="40" height="40" />
            <div class="recommend-info">
              <div class="recommend-name text-ellipsis">{{ m.nickname }}</div>
              <div class="recommend-meta">匹配度 {{ m.matchScore }}%</div>
            </div>
            <button
              :class="['btn-sm', recommendFollowedIds.includes(m.userId) ? 'btn-followed' : 'btn-outline']"
              @click.stop="handleRecommendFollow(m.userId)"
            >
              {{ recommendFollowedIds.includes(m.userId) ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
        <div v-else class="empty-hint">完善资料后查看推荐</div>
      </div>

      <div class="panel-card">
        <div class="panel-title-row">
          <h3 class="panel-title">热门邀约看板</h3>
          <button class="btn-text" @click="$router.push('/invite')">查看全部</button>
        </div>
        <div v-if="boardLoading" class="board-loading">加载中...</div>
        <div v-else-if="inviteBoard.length" class="board-list">
          <div
            v-for="item in inviteBoard"
            :key="item.inviteType"
            class="board-item"
            @click="$router.push(`/invite?type=${item.inviteType}`)"
          >
            <div class="board-item-left">
              <span class="board-dot" :style="{ background: getTypeColor(item.inviteType) }" />
              <span class="board-type">{{ typeLabel(item.inviteType) }}</span>
            </div>
            <div class="board-item-right">
              <span class="board-count">{{ item.count }}</span>
            </div>
            <div class="board-bar">
              <div class="board-bar-fill" :style="{ width: `${item.percent}%`, background: getTypeColor(item.inviteType) }" />
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无进行中的邀约</div>
      </div>
    </aside>

    <!-- Post Dialog -->
    <el-dialog v-model="showPostDialog" title="发布动态" width="560px" :close-on-click-modal="false">
      <el-input v-model="postContent" type="textarea" :rows="4" placeholder="分享你的校园生活..." maxlength="500" show-word-limit />

      <!-- 多媒体上传区域 -->
      <div class="post-media-section">
        <!-- 已上传的图片预览 -->
        <div v-if="uploadedImages.length" class="media-preview-grid">
          <div v-for="(img, idx) in uploadedImages" :key="idx" class="media-preview-item">
            <img :src="getMediaUrl(img)" class="preview-img" />
            <button class="preview-remove" @click="removeImage(idx)">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <!-- 已上传的视频预览 -->
        <div v-if="uploadedVideos.length" class="media-preview-grid">
          <div v-for="(video, idx) in uploadedVideos" :key="idx" class="media-preview-item">
            <video :src="getMediaUrl(video)" class="preview-video" />
            <button class="preview-remove" @click="removeVideo(idx)">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <!-- 链接预览 -->
        <div v-if="linkPreview.url" class="link-preview-card">
          <img v-if="linkPreview.image" :src="getMediaUrl(linkPreview.image)" class="link-preview-img" />
          <div class="link-preview-content">
            <div class="link-preview-title">{{ linkPreview.title || linkPreview.url }}</div>
            <button class="link-preview-remove" @click="clearLink">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <!-- 上传按钮 -->
        <div class="media-actions">
          <input ref="imageInputRef" type="file" accept="image/*" multiple hidden @change="handleImageSelect" />
          <button class="media-btn" @click="imageInputRef?.click()">
            <el-icon><Picture /></el-icon>
            <span>图片</span>
          </button>

          <input ref="videoInputRef" type="file" accept="video/*" hidden @change="handleVideoSelect" />
          <button class="media-btn" @click="videoInputRef?.click()">
            <el-icon><VideoCamera /></el-icon>
            <span>视频</span>
          </button>

          <button class="media-btn" @click="showLinkInput = !showLinkInput">
            <el-icon><Link /></el-icon>
            <span>链接</span>
          </button>
        </div>

        <!-- 链接输入 -->
        <div v-if="showLinkInput" class="link-input-wrapper">
          <el-input v-model="linkUrlInput" placeholder="粘贴链接 (https://...)" @keyup.enter="handleAddLink" />
          <button class="btn-link-add" @click="handleAddLink">添加</button>
        </div>
      </div>

      <template #footer>
        <button class="btn-primary" :disabled="isPublishing || (!postContent.trim() && !uploadedImages.length && !uploadedVideos.length && !linkPreview.url)" @click="handleCreatePost">
          {{ isPublishing ? '发布中...' : '发布' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useBadgeStore } from '@/store/badgeStore'
import { getRecommendations, type MatchResult } from '@/api/matchApi'
import { followUser, unfollowUser, getFollowingList } from '@/api/followApi'
import { createPost, uploadImage, uploadVideo } from '@/api/feedApi'
import { searchUsers, type UserSearchItem } from '@/api/userApi'
import { ElMessage } from 'element-plus'
// 右侧面板不再展示”热门标签”，改为热门邀约看板
import { getHotInviteTypeCounts, type InviteTypeCount } from '@/api/inviteApi'
import { InviteType, INVITE_TYPE_LABELS } from '@/constants/inviteConst'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const badgeStore = useBadgeStore()

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect fill="%23f0f2f5" width="40" height="40" rx="20"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="18">👤</text></svg>'

const navItems = computed(() => {
  const b = badgeStore.badges
  return [
    { path: '/discover', label: '发现', icon: 'Compass', showDot: false },
    { path: '/match', label: '匹配', icon: 'MagicStick', showDot: false },
    { path: '/invite', label: '邀约', icon: 'Calendar', showDot: b.newInviteActivityCount > 0 },
    { path: '/chat', label: '消息', icon: 'ChatDotRound', showDot: b.unreadMessageCount > 0 },
    { path: '/feed', label: '朋友圈', icon: 'Notebook', showDot: b.newFeedActivityCount > 0 },
    { path: '/profile', label: '我的', icon: 'User', showDot: b.newFollowerCount > 0 },
  ]
})

const isActive = (path: string) => route.path.startsWith(path)

const topMatches = ref<MatchResult[]>([])
const recommendFollowedIds = ref<number[]>([])
const showPostDialog = ref(false)
const postContent = ref('')

// 多媒体上传状态
const uploadedImages = ref<string[]>([])
const uploadedVideos = ref<string[]>([])
const linkPreview = ref<{ url: string; title: string; image: string }>({ url: '', title: '', image: '' })
const linkUrlInput = ref('')
const showLinkInput = ref(false)
const isPublishing = ref(false)
const imageInputRef = ref<HTMLInputElement>()
const videoInputRef = ref<HTMLInputElement>()

// 邀约看板（按类型统计）
const inviteBoard = ref<Array<InviteTypeCount & { percent: number }>>([])
const boardLoading = ref(false)

// 搜索
const searchBoxRef = ref<HTMLElement>()
const searchKeyword = ref('')
const searchResults = ref<UserSearchItem[]>([])
const searchFollowedIds = ref<number[]>([])
const searchLoading = ref(false)
const showSearchDropdown = ref(false)
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

watch(searchKeyword, (val) => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  if (!val?.trim()) {
    showSearchDropdown.value = false
    searchResults.value = []
    searchFollowedIds.value = []
    return
  }
  showSearchDropdown.value = true
  if (val.trim().length < 2) return
  searchDebounceTimer = setTimeout(doSearch, 300)
})

async function doSearch() {
  const kw = searchKeyword.value?.trim()
  if (!kw || kw.length < 2) {
    searchResults.value = []
    searchFollowedIds.value = []
    return
  }
  searchLoading.value = true
  try {
    const [searchRes, followRes] = await Promise.all([
      searchUsers(kw, 10),
      getFollowingList(),
    ])
    searchResults.value = searchRes.data.data || []
    searchFollowedIds.value = (followRes.data.data || []).map(f => f.userId)
  } catch {
    searchResults.value = []
    searchFollowedIds.value = []
  } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  searchResults.value = []
  searchFollowedIds.value = []
  showSearchDropdown.value = false
}

function goToProfile(userId: number) {
  searchKeyword.value = ''
  clearSearch()
  router.push(`/profile/${userId}`)
}

async function handleSearchFollow(userId: number) {
  const isFollowing = searchFollowedIds.value.includes(userId)
  try {
    if (isFollowing) {
      await unfollowUser(userId)
      searchFollowedIds.value = searchFollowedIds.value.filter(id => id !== userId)
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId)
      searchFollowedIds.value = [...searchFollowedIds.value, userId]
      ElMessage.success('关注成功')
    }
  } catch { /* handled by interceptor */ }
}

function onDocumentClick(e: MouseEvent) {
  if (searchBoxRef.value && !searchBoxRef.value.contains(e.target as Node)) {
    showSearchDropdown.value = false
  }
}

let badgePollTimer: ReturnType<typeof setInterval> | null = null
async function loadRecommendations() {
  try {
    const [recRes, followRes] = await Promise.all([
      getRecommendations(0, 5),
      getFollowingList(),
    ])
    topMatches.value = recRes.data.data || []
    recommendFollowedIds.value = (followRes.data.data || []).map((f: { userId: number }) => f.userId)
  } catch {
    topMatches.value = []
    recommendFollowedIds.value = []
  }
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible' && userStore.user) badgeStore.fetchBadges()
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('visibilitychange', onVisibilityChange)
  if (userStore.user) badgeStore.fetchBadges()
  badgePollTimer = setInterval(() => {
    if (userStore.user) badgeStore.fetchBadges()
  }, 15000)
  loadRecommendations()
})

watch(() => route.path, () => {
  if (userStore.user) badgeStore.fetchBadges()
}, { immediate: false })

watch(() => userStore.user, (u) => {
  if (u) badgeStore.fetchBadges()
}, { immediate: true })

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (badgePollTimer) clearInterval(badgePollTimer)
})

async function handleRecommendFollow(userId: number) {
  const isFollowed = recommendFollowedIds.value.includes(userId)
  try {
    if (isFollowed) {
      await unfollowUser(userId)
      recommendFollowedIds.value = recommendFollowedIds.value.filter(id => id !== userId)
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId)
      recommendFollowedIds.value = [...recommendFollowedIds.value, userId]
      ElMessage.success('关注成功')
    }
  } catch { /* handled by interceptor */ }
}

async function handleCreatePost() {
  if (!postContent.value.trim() && !uploadedImages.value.length && !uploadedVideos.value.length && !linkPreview.value.url) {
    return
  }

  isPublishing.value = true
  try {
    await createPost({
      content: postContent.value.trim(),
      images: uploadedImages.value.length ? uploadedImages.value.join(',') : undefined,
      videos: uploadedVideos.value.length ? uploadedVideos.value.join(',') : undefined,
      linkUrl: linkPreview.value.url || undefined,
      linkTitle: linkPreview.value.title || undefined,
      linkImage: linkPreview.value.image || undefined
    })
    ElMessage.success('发布成功')
    showPostDialog.value = false
    resetPostForm()
  } catch {
    ElMessage.error('发布失败')
  } finally {
    isPublishing.value = false
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
    title: url, // 简单处理，实际可以抓取标题
    image: '' // 简单处理，实际可以抓取预览图
  }
  linkUrlInput.value = ''
  showLinkInput.value = false
}

function clearLink() {
  linkPreview.value = { url: '', title: '', image: '' }
}

function typeLabel(t: string) {
  return INVITE_TYPE_LABELS[t as InviteType] || t
}

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

function getMediaUrl(url: string | null): string {
  if (!url) return ''
  // 如果是完整 URL 或已包含 /api 前缀，直接返回
  if (url.startsWith('http') || url.startsWith('/api')) {
    return url
  }
  // 添加 /api 前缀
  return '/api' + (url.startsWith('/') ? url : '/' + url)
}

async function loadInviteBoard() {
  boardLoading.value = true
  try {
    const res = await getHotInviteTypeCounts(10)
    const list = res.data.data || []
    const max = Math.max(...list.map(i => i.count), 0)
    inviteBoard.value = list.map(i => ({
      ...i,
      percent: max <= 0 ? 0 : Math.max(6, Math.round((i.count / max) * 100)),
    }))
  } catch {
    inviteBoard.value = []
  } finally {
    boardLoading.value = false
  }
}

onMounted(loadInviteBoard)
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  max-width: 1280px;
  margin: 0 auto;
}

// === Left Sidebar ===
.sidebar {
  width: $sidebar-width;
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid $border-light;
  background: $bg-primary;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 16px;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 24px;

  .logo-icon { font-size: 28px; }
  .logo-text {
    font-size: 22px;
    font-weight: 800;
    background: $primary-gradient;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: $radius-full;
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;
  transition: all $transition-fast;
  position: relative;

  &:hover { background: $bg-tertiary; }

  &.active {
    font-weight: 700;
    color: $primary;
  }

  .nav-dot {
    position: absolute;
    right: 16px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $danger;
  }
}

.post-btn {
  width: 100%;
  height: 48px;
  margin-top: 16px;
  font-size: 16px;
}

// 发布动态多媒体区域
.post-media-section {
  margin-top: 16px;
}

.media-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
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
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.6);
    color: white;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;

    &:hover { background: rgba(245, 34, 45, 0.9); }
  }
}

.link-preview-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: $bg-tertiary;
  border-radius: 8px;
  margin-bottom: 12px;

  .link-preview-img {
    width: 60px;
    height: 60px;
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
    gap: 8px;
  }

  .link-preview-title {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
    word-break: break-all;
  }

  .link-preview-remove {
    flex-shrink: 0;
    width: 20px;
    height: 20px;
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
  gap: 6px;
  padding: 8px 14px;
  background: $bg-tertiary;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  font-size: 14px;
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
  padding: 8px 16px;
  background: $primary;
  color: white;
  border: none;
  border-radius: $radius-md;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { opacity: 0.9; }
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-top: 16px;
  border-radius: $radius-full;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover { background: $bg-tertiary; }

  .user-info { flex: 1; min-width: 0; }
  .user-name { font-size: 14px; font-weight: 600; }
  .user-email { font-size: 12px; color: $text-muted; }
}

// === Main Content ===
.content-area {
  flex: 1;
  min-width: 0;
  border-right: 1px solid $border-light;
  background: $bg-primary;
}

// === Right Panel ===
.right-panel {
  width: $right-panel-width;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  padding: 16px 24px;
  background: $bg-secondary;
}

.search-box {
  margin-bottom: 20px;
  position: relative;

  :deep(.el-input__wrapper) {
    border-radius: $radius-full;
    background: $bg-primary;
  }
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 8px;
  background: $bg-primary;
  border-radius: $radius-lg;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  max-height: 320px;
  overflow-y: auto;
  z-index: 100;
}

.search-loading,
.search-hint,
.search-empty {
  padding: 16px;
  text-align: center;
  color: $text-muted;
  font-size: 14px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover { background: $bg-tertiary; }

  .avatar { border-radius: 50%; flex-shrink: 0; }
  .search-item-name { flex: 1; min-width: 0; font-size: 14px; font-weight: 500; }
}

.panel-card {
  background: $bg-primary;
  border-radius: $radius-lg;
  padding: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: $text-primary;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-text {
  background: transparent;
  border: none;
  color: $text-muted;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: $radius-md;
  transition: all $transition-fast;

  &:hover {
    background: $bg-tertiary;
    color: $text-secondary;
  }
}

.board-loading {
  padding: 20px;
  text-align: center;
  color: $text-muted;
  font-size: 14px;
}

.board-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.board-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 12px 10px;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover { background: $bg-tertiary; }
}

.board-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.board-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.board-type {
  font-size: 14px;
  font-weight: 700;
  color: $text-primary;
  white-space: nowrap;
}

.board-item-right {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-shrink: 0;
}

.board-count {
  font-size: 16px;
  font-weight: 800;
  color: $text-primary;
}

.board-bar {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 8px;
  height: 6px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.board-bar-fill {
  height: 100%;
  border-radius: 999px;
  opacity: 0.9;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 0;

  &:hover .recommend-name { color: $primary; }

  .recommend-info { flex: 1; min-width: 0; }
  .recommend-name { font-size: 14px; font-weight: 600; }
  .recommend-meta { font-size: 12px; color: $primary; margin-top: 2px; }
}

.btn-sm { padding: 4px 12px; font-size: 12px; }

.btn-followed {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: $bg-tertiary;
  color: $text-muted;
  border: none;
  border-radius: $radius-full;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    background: rgba($danger, 0.1);
    color: $danger;
  }
}

.empty-hint {
  text-align: center;
  padding: 20px;
  color: $text-muted;
  font-size: 14px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  padding: 4px 12px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.1);
    color: $primary;
  }
}
</style>
