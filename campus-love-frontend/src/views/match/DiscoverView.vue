<template>
  <div class="discover-page">
    <div class="glass-tuner-wrap">
      <div class="search-wrap">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户、帖子、邀约..."
          class="glass-search-input"
          clearable
          @keyup.enter="doSearch"
          @focus="searchKeyword.trim().length >= 2 && (showUserDropdown = true)"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <div v-if="showUserDropdown" class="user-search-dropdown glass-panel">
          <div v-if="userSearchLoading" class="usd-hint">搜索中...</div>
          <div v-else-if="searchKeyword.trim().length < 2" class="usd-hint">输入至少 2 个字符</div>
          <div v-else-if="!userSearchResults.length" class="usd-hint">未找到用户，按回车搜索帖子</div>
          <template v-else>
            <div class="usd-section-label">用户星球</div>
            <div
              v-for="u in userSearchResults"
              :key="u.id"
              class="usd-item"
              @click="goToUserProfile(u.id)"
            >
              <AppAvatar :src="u.avatarUrl" :name="u.nickname" :size="32" class="usd-avatar" />
              <span class="usd-name">{{ u.nickname }}</span>
              <button
                :class="['usd-follow-btn', followStore.isFollowed(u.id) ? 'followed' : '']"
                @click.stop="handleUserFollow(u.id)"
              >
                {{ followStore.isFollowed(u.id) ? '已关注' : '关注' }}
              </button>
            </div>
            <div class="usd-divider" />
            <div class="usd-hint usd-search-posts" @click="doSearch; showUserDropdown = false">
              <el-icon><Search /></el-icon> 探索频段「{{ searchKeyword.trim() }}」
            </div>
          </template>
        </div>
      </div>

      <div class="tuner-capsule glass-panel">
        <div class="tuner-tabs">
          <button
            :class="['tuner-btn', { active: activeTab === 'recommend' }]"
            @click="switchTab('recommend')"
          >推荐</button>
          <button
            :class="['tuner-btn', { active: activeTab === 'following' }]"
            @click="switchTab('following')"
          >关注</button>
          <button
            :class="['tuner-btn', { active: activeTab === 'liked' }]"
            @click="switchTab('liked')"
          >点赞</button>
        </div>
        
        <el-dropdown
          v-if="activeTab === 'recommend' || activeTab === 'following'"
          trigger="click"
          placement="bottom-end"
          @command="handleSortCommand"
        >
          <button class="tuner-sort-btn" type="button" title="切换频段模式">
            <el-icon><Operation /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="sort-dropdown-menu glass-panel">
              <el-dropdown-item :class="{ 'is-active': feedSort === 'recommend' }" command="recommend">算法推荐</el-dropdown-item>
              <el-dropdown-item :class="{ 'is-active': feedSort === 'time' }" command="time">最新时空</el-dropdown-item>
              <el-dropdown-item :class="{ 'is-active': feedSort === 'hot' }" command="hot">校园热度</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div v-if="pullRefreshVisible" class="pull-refresh-indicator">
      <el-icon class="spinning"><Loading /></el-icon> 捕捉信号中...
    </div>

    <div v-if="timelineItems.length" class="timeline-list">
      <div
        v-for="item in timelineItems"
        :key="item.key"
        class="timeline-item"
      >
        <div
          class="feed-card glass-card"
          :class="{ 'feed-card--pinned': item.post.pinned }"
          @click="goPostDetail(item.post.id)"
        >
          <div v-if="item.post.pinned" class="feed-pinned-badge">置顶</div>
          
          <div class="feed-header" @click.stop>
            <div class="avatar-glow-wrap" :style="{ '--glow-color': getAvatarGlow(item.post.userId) }">
              <el-image
                :src="getMediaUrl(item.post.avatarUrl) || defaultAvatar"
                class="feed-avatar"
                fit="cover"
                @click="$router.push(`/profile/${item.post.userId}`)"
              />
            </div>

            <div class="feed-user" @click="$router.push(`/profile/${item.post.userId}`)">
              <div class="feed-name">
                {{ item.post.nickname }}
              </div>
              <div class="feed-time">{{ formatTime(item.post.createdAt) }}</div>
            </div>

            <button v-if="isAdmin && (reportCountByPostId[item.post.id] || 0) > 0" type="button" class="feed-icon-btn text-warning" @click.stop="$router.push(`/admin/reports?targetId=${item.post.id}`)">
              <el-icon><WarningFilled /></el-icon> {{ reportCountByPostId[item.post.id] }}
            </button>
            <button
              v-if="isAdmin"
              type="button"
              class="feed-pin-text-btn"
              :class="{ 'is-pinned': item.post.pinnedAt }"
              @click.stop="handlePinPost(item.post)"
            >
              {{ item.post.pinnedAt ? '取消置顶' : '置顶' }}
            </button>
            <button type="button" :class="['feed-icon-btn', { reported: reportedPostIds.has(item.post.id) }]" @click.stop="handleReportClick(item.post.id, 'POST')">
              <el-icon><Flag /></el-icon>
            </button>
            <button v-if="canDeletePost(item.post)" type="button" class="feed-icon-btn text-danger" @click.stop="handleDeletePost(item.post.id)">
              <el-icon><Delete /></el-icon>
            </button>
          </div>
          
          <div class="feed-content">
            <template v-if="shouldCollapse(item.post.content)">
              <span v-if="isExpanded(item.post.id)" class="feed-text">{{ item.post.content }}</span>
              <span v-else class="feed-text">{{ getDisplayContent(item.post.content, item.post.id) }}</span>
              <button class="expand-btn" @click.stop="toggleExpand(item.post.id)">
                {{ isExpanded(item.post.id) ? '收起' : '展开信号' }}
              </button>
            </template>
            <span v-else class="feed-text">{{ item.post.content }}</span>
          </div>

          <div v-if="item.post.images" class="feed-images" @click.stop>
            <img
              v-for="(img, idx) in feedCardImagePaths(item.post).slice(0, 4)"
              :key="idx"
              :src="getMediaUrl(img)"
              class="feed-image"
              loading="lazy"
              @error="onFeedListImgError($event, item.post, idx)"
              @click.stop="openImagePreview(fullFeedImageUrls(item.post), idx)"
            />
          </div>

          <div v-if="item.post.videos" class="feed-videos" @click.stop>
            <video v-for="(video, idx) in item.post.videos.split(',')" :key="idx" :src="getMediaUrl(video)" class="feed-video glass-panel" controls />
          </div>

          <FeedInviteCard v-if="item.post.inviteCard" :card="item.post.inviteCard" />

          <div v-if="item.post.aiTags" class="feed-ai-tags">
            <span v-for="tag in (item.post.aiTags || '').split(/[,，]/).filter(Boolean)" :key="tag" class="ai-tag glass-pill">
              # {{ tag.trim() }}
            </span>
          </div>

          <div class="feed-actions" @click.stop>
            <button
              :class="['action-btn like-btn', { active: item.post.liked }]"
              @click="handleLike(item.post.id, item.post.liked)"
            >
              <el-icon :size="20"><StarFilled v-if="item.post.liked" /><Star v-else /></el-icon>
              <span>{{ item.post.likeCount || '共鸣' }}</span>
              <div v-if="item.post.liked" class="like-ripple"></div>
            </button>
            <button class="action-btn" @click="goPostDetail(item.post.id)">
              <el-icon :size="20"><ChatDotRound /></el-icon>
              <span>{{ item.post.commentCount || '回应' }}</span>
            </button>
            <button class="action-btn share-btn" @click="openShareDialog(item.post)">
              <el-icon :size="20"><Share /></el-icon>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!timelineItems.length && !feedLoading" class="empty-radar-state">
      <div class="radar-waves">
        <div class="wave wave-1"></div>
        <div class="wave wave-2"></div>
        <div class="wave wave-3"></div>
        <div class="radar-core">
          <el-icon><Microphone /></el-icon>
        </div>
      </div>
      <p class="empty-title">这片宇宙还在等待第一声回响</p>
      <p class="empty-subtitle">发出你的频段，吸引同频的人</p>
    </div>

    <div ref="sentinelRef" class="sentinel">
      <div v-if="feedLoading && timelineItems.length" class="loading-more">
        <el-icon class="spinning"><Loading /></el-icon> 探索深层宇宙...
      </div>
      <div v-else-if="!feedHasMore && timelineItems.length" class="no-more">
        已抵达星系边缘
      </div>
    </div>

    <div class="fab-container">
      <el-tooltip :content="postDisabledTip" placement="left" :disabled="isAdmin">
        <button
          class="fab-post-btn glow-btn-warm"
          :class="{ 'disabled': !isAdmin }"
          @click="handlePostEntry"
        >
          <el-icon><Plus /></el-icon>
        </button>
      </el-tooltip>
    </div>

    <el-dialog v-model="showPostDialog" title="发射引力波" :width="postDialogWidth" class="light-glass-dialog" align-center :close-on-click-modal="false" destroy-on-close>
      <el-form @submit.prevent="handlePost" label-position="top">
        <el-form-item>
          <el-input v-model="postContent" type="textarea" :rows="postTextareaRows" placeholder="分享此刻的共鸣..." maxlength="500" show-word-limit />
        </el-form-item>
        <div class="post-media-section">
          <div v-if="uploadedImages.length" class="media-preview-grid">
            <div v-for="(img, idx) in uploadedImages" :key="idx" class="media-preview-item">
              <img :src="getMediaUrl(img)" class="preview-img" />
              <button type="button" class="preview-remove" @click="removeImage(idx)"><el-icon><Close /></el-icon></button>
            </div>
          </div>
          <div class="media-actions">
            <input ref="mediaInputRef" type="file" accept="image/*,video/*" multiple hidden @change="handleMediaSelect" />
            <button type="button" class="media-btn glass-pill" @click="mediaInputRef?.click()"><el-icon><Picture /></el-icon><span>画面/频段</span></button>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closePostDialog" class="glass-btn">取消</el-button>
        <el-button type="primary" class="glow-btn-warm" :disabled="posting || (!postContent.trim() && !uploadedImages.length && !uploadedVideos.length && selectedInviteId == null)" @click="handlePost">
          {{ posting ? '发射中...' : '发射' }}
        </el-button>
      </template>
    </el-dialog>

    <ReportDialog v-model="showReportDialog" :target-type="reportTargetType" :target-id="reportTargetId" @success="onReportSuccess" />
    <ShareDialog v-model:show="showShareDialog" :post="currentSharePost" @success="handleShareSuccess" />
    <el-image-viewer v-if="previewVisible" :url-list="previewImages" :initial-index="previewIndex" teleported @close="previewVisible = false" />
  </div>
</template>

<script setup lang="ts">
/** 与 MainLayout keep-alive include="Discover" 一致，从帖子详情返回时保留列表与滚动位置 */
defineOptions({ name: 'Discover' })
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getDiscoveryPosts, getTimeline, getLikedPosts, likePost, unlikePost,
  pinPost, unpinPost, getLevelInfo, createDiscoveryPost, deletePost,
  uploadImage, uploadVideo, type FeedPost, type UserLevelInfo
} from '@/api/feedApi'
import { checkReported, getMyReport, getReportCountByPostIds, VIOLATION_TYPES } from '@/api/reportApi'
import ReportDialog from '@/components/ReportDialog.vue'
import FeedInviteCard from '@/components/FeedInviteCard.vue'
import { useUserStore } from '@/store/userStore'
import { useFollowStore } from '@/store/followStore'
import { useInviteStore } from '@/store/inviteStore'
import { followUser, unfollowUser } from '@/api/followApi'
import { searchUsers, type UserSearchItem } from '@/api/userApi'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Search, Flag, WarningFilled, Sort, Refresh, Loading, Operation, Star, StarFilled, ChatDotRound, Share, Microphone, Picture, Close } from '@element-plus/icons-vue'
import type { Invite } from '@/api/inviteApi'
import { getInvitesForFeed } from '@/api/inviteApi'
import { INVITE_TYPE_LABELS, type InviteType } from '@/constants/inviteConst'
import ShareDialog from '@/components/ShareDialog.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { DEFAULT_AVATAR, getMediaUrl, formatRelativeTime, feedCardImagePaths } from '@/utils/shared'
import { compressImageFile } from '@/utils/mediaCompress'
import { usePostPublishDialogLayout } from '@/composables/usePostPublishDialogLayout'

// --- 新增 UI 辅助函数：根据 UserID 生成发光颜色 ---
function getAvatarGlow(userId: number): string {
  const colors = [
    'rgba(79, 140, 255, 0.7)', // 科技蓝
    'rgba(255, 51, 102, 0.7)', // 心动粉
    'rgba(167, 139, 250, 0.7)', // 宇宙紫
    'rgba(54, 227, 138, 0.7)', // 极光绿
    'rgba(255, 123, 84, 0.7)'  // 日落橘
  ];
  return colors[(userId || 0) % colors.length]!
}
// ---------------------------------------------

const { postDialogWidth, postTextareaRows } = usePostPublishDialogLayout()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const followStore = useFollowStore()
const inviteStore = useInviteStore()
const defaultAvatar = DEFAULT_AVATAR

// 用户搜索
const userSearchResults = ref<UserSearchItem[]>([])
const userSearchLoading = ref(false)
const showUserDropdown = ref(false)
const searchBoxRef = ref<HTMLElement>()
let userSearchTimer: ReturnType<typeof setTimeout> | null = null

// 图片预览
const previewImages = ref<string[]>([])
const previewIndex = ref(0)
const previewVisible = ref(false)

function fullFeedImageUrls(post: FeedPost): string[] {
  return (post.images || '').split(',').map((s) => getMediaUrl(s.trim())).filter(Boolean)
}

function onFeedListImgError(e: Event, post: FeedPost, idx: number) {
  const el = e.target as HTMLImageElement
  const parts = (post.images || '').split(',').map((s) => s.trim()).filter(Boolean)
  const fallback = parts[idx]
  if (!fallback) return
  const next = getMediaUrl(fallback)
  if (el.src !== next) {
    el.onerror = null
    el.src = next
  }
}

function openImagePreview(images: string[], index: number) {
  previewImages.value = images
  previewIndex.value = index
  previewVisible.value = true
}

async function doUserSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw || kw.length < 2) return
  userSearchLoading.value = true
  try {
    if (followStore.followedIds.length === 0) await followStore.loadFollowedIds()
    const res = await searchUsers(kw, 8)
    userSearchResults.value = res.data.data || []
  } catch {
    userSearchResults.value = []
  } finally {
    userSearchLoading.value = false
  }
}

function goToUserProfile(userId: number) {
  showUserDropdown.value = false
  searchKeyword.value = ''
  router.push(`/profile/${userId}`)
}

async function handleUserFollow(userId: number) {
  const isFollowing = followStore.isFollowed(userId)
  try {
    if (isFollowing) {
      await unfollowUser(userId)
      followStore.removeFollowed(userId)
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId)
      followStore.addFollowed(userId)
      ElMessage.success('关注成功')
    }
  } catch { }
}

function closeUserDropdown(e: MouseEvent) {
  if (searchBoxRef.value && !searchBoxRef.value.contains(e.target as Node)) {
    showUserDropdown.value = false
  }
}

const posts = ref<FeedPost[]>([])
const followingPosts = ref<FeedPost[]>([])
const likedPosts = ref<FeedPost[]>([])
const expandedPosts = ref<Map<number, boolean>>(new Map())
const CONTENT_MAX_LENGTH = 100
const FEED_POST_MAX_IMAGES = 8

const levelInfo = ref<UserLevelInfo | null>(null)
const searchKeyword = ref('')

watch(searchKeyword, (val) => {
  if (userSearchTimer) clearTimeout(userSearchTimer)
  if (!val?.trim()) {
    showUserDropdown.value = false
    userSearchResults.value = []
    return
  }
  if (val.trim().length < 2) {
    showUserDropdown.value = true
    userSearchResults.value = []
    return
  }
  showUserDropdown.value = true
  userSearchTimer = setTimeout(doUserSearch, 300)
})

const reportedPostIds = ref<Set<number>>(new Set())
const showReportDialog = ref(false)
const reportTargetType = ref('POST')
const reportTargetId = ref(0)
const activeTab = ref<'recommend' | 'following' | 'liked'>('recommend')
const feedSort = ref<'recommend' | 'hot' | 'time'>('recommend')

const feedPage = ref(0)
const feedHasMore = ref(true)
const feedLoading = ref(false)
const sentinelRef = ref<HTMLElement>()
let scrollObserver: IntersectionObserver | null = null
let discoverSideEffectsBound = false

let touchStartY = 0
let isPullRefreshing = false
const pullRefreshVisible = ref(false)

const showPostDialog = ref(false)
const postContent = ref('')
const postVisibility = ref('ALL')
const posting = ref(false)
const postDisabledTip = '暂时不开放发帖功能，欢迎点赞回应~'

function handlePostEntry() {
  if (isAdmin.value) {
    showPostDialog.value = true
    return
  }
  ElMessage.info(postDisabledTip)
}

watch(showPostDialog, (open) => {
  if (open) {
    void loadInvitesForFeedPicker()
    postVisibility.value = userStore.user?.feedVisibility ?? 'ALL'
  }
})

function inviteOptionLabel(inv: Invite) {
  const t = INVITE_TYPE_LABELS[inv.inviteType as InviteType] || inv.inviteType
  return `${t} · ${inv.title}`
}

async function loadInvitesForFeedPicker() {
  loadingInvitesForFeed.value = true
  try {
    const res = await getInvitesForFeed(1, 50)
    inviteOptionsForFeed.value = res.data.data?.records ?? []
  } catch {
    inviteOptionsForFeed.value = []
  } finally {
    loadingInvitesForFeed.value = false
  }
}

const uploadedImages = ref<string[]>([])
const uploadedVideos = ref<string[]>([])
const selectedInviteId = ref<number | undefined>(undefined)
const inviteOptionsForFeed = ref<Invite[]>([])
const loadingInvitesForFeed = ref(false)
const mediaInputRef = ref<HTMLInputElement>()

const showShareDialog = ref(false)
const currentSharePost = ref<FeedPost | null>(null)

function openShareDialog(post: FeedPost) {
  currentSharePost.value = post
  showShareDialog.value = true
}
function handleShareSuccess() {}

function goPostDetail(postId: number) {
  const id = Number(postId)
  if (!Number.isFinite(id) || id <= 0) return
  router.push(`/feed/${id}`)
}

const isAdmin = computed(() => userStore.user?.isAdmin || false)
const reportCountByPostId = ref<Record<number, number>>({})

async function fetchReportCountsForPosts(postIds: number[]) {
  if (!isAdmin.value || !postIds.length) return
  try {
    const res = await getReportCountByPostIds(postIds)
    reportCountByPostId.value = { ...reportCountByPostId.value, ...res.data.data }
  } catch {}
}

function canDeletePost(post: FeedPost): boolean {
  return (userStore.user?.isAdmin || false) || (post.userId === userStore.user?.id)
}

function getInviteTypeFromRoute(): string | undefined {
  const t = route.query.type
  return typeof t === 'string' && t ? t : undefined
}

function switchTab(tab: 'recommend' | 'following' | 'liked') {
  activeTab.value = tab
  refreshPosts()
}

function setFeedSort(sort: 'recommend' | 'hot' | 'time') {
  feedSort.value = sort
  refreshPosts()
}

function handleSortCommand(command: string | number | object) {
  if (command === 'recommend' || command === 'time' || command === 'hot') {
    setFeedSort(command as any)
  }
}

function doSearch() { refreshPosts() }

async function refreshPosts() {
  feedPage.value = 0
  feedHasMore.value = true
  posts.value = []
  followingPosts.value = []
  likedPosts.value = []
  await loadByTab(true)
}

async function loadByTab(isRefresh = false) {
  const kw = searchKeyword.value.trim() || undefined
  if (activeTab.value === 'liked') {
    await loadLikedPosts(isRefresh)
  } else if (activeTab.value === 'following') {
    await loadFollowingPosts(isRefresh)
  } else {
    await loadPosts(kw, isRefresh)
  }
}

async function loadMorePosts() {
  if (feedLoading.value || !feedHasMore.value) return
  if (activeTab.value === 'liked') return 
  feedPage.value++
  await loadByTab(false)
}

async function loadLikedPosts(isRefresh = false) {
  if (isRefresh) likedPosts.value = []
  try {
    feedLoading.value = true
    const res = await getLikedPosts(feedPage.value, 10)
    const data = res.data.data || []
    if (isRefresh) likedPosts.value = data
    else likedPosts.value = [...likedPosts.value, ...data]
    feedHasMore.value = data.length >= 10
    if (isAdmin.value && data.length) fetchReportCountsForPosts(data.map((p) => p.id))
  } catch {} finally { feedLoading.value = false }
}

onMounted(async () => {
  await loadLevelInfo()
  await refreshPosts()
  bindDiscoverSideEffects()
})

onUnmounted(() => {
  unbindDiscoverSideEffects()
  if (userSearchTimer) clearTimeout(userSearchTimer)
})

onActivated(() => bindDiscoverSideEffects())
onDeactivated(() => unbindDiscoverSideEffects())

function bindDiscoverSideEffects() {
  if (discoverSideEffectsBound) return
  discoverSideEffectsBound = true
  scrollObserver = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting && !feedLoading.value && feedHasMore.value) loadMorePosts()
  }, { rootMargin: '120px' })
  if (sentinelRef.value) scrollObserver.observe(sentinelRef.value)
  document.addEventListener('touchstart', onTouchStart, { passive: true })
  document.addEventListener('touchend', onTouchEnd, { passive: true })
  document.addEventListener('click', closeUserDropdown)
}

function unbindDiscoverSideEffects() {
  if (!discoverSideEffectsBound) return
  discoverSideEffectsBound = false
  scrollObserver?.disconnect()
  scrollObserver = null
  document.removeEventListener('touchstart', onTouchStart)
  document.removeEventListener('touchend', onTouchEnd)
  document.removeEventListener('click', closeUserDropdown)
}

function onTouchStart(e: TouchEvent) {
  touchStartY = e.touches[0]?.clientY ?? 0
  isPullRefreshing = false
}

function onTouchEnd(e: TouchEvent) {
  if (!touchStartY) return
  const endY = e.changedTouches[0]?.clientY ?? 0
  const delta = endY - touchStartY
  if (delta > 80 && window.scrollY < 10 && !isPullRefreshing && !feedLoading.value) {
    isPullRefreshing = true
    pullRefreshVisible.value = true
    feedSort.value = 'recommend'
    refreshPosts().finally(() => pullRefreshVisible.value = false)
  }
  touchStartY = 0
}

watch(() => route.query.type, () => loadInvites(searchKeyword.value.trim() || undefined))

type TimelineItem = { kind: 'post'; post: FeedPost; time: string; key: string }

const timelineItems = computed<TimelineItem[]>(() => {
  let postList = activeTab.value === 'following' ? followingPosts.value : activeTab.value === 'liked' ? likedPosts.value : posts.value
  return postList.map(post => ({ kind: 'post' as const, post, time: post.createdAt, key: `post-${post.id}` }))
})

async function loadPosts(keyword?: string, isRefresh = false) {
  if (feedLoading.value && !isRefresh) return
  try {
    feedLoading.value = true
    const res = await getDiscoveryPosts(feedPage.value, 10, feedSort.value, keyword)
    const data = res.data.data || []
    if (isRefresh) posts.value = data
    else posts.value = [...posts.value, ...data]
    feedHasMore.value = data.length >= 10
    if (isAdmin.value && data.length) fetchReportCountsForPosts(data.map((p) => p.id))
  } catch {} finally { feedLoading.value = false }
}

async function loadFollowingPosts(isRefresh = false) {
  if (feedLoading.value && !isRefresh) return
  try {
    feedLoading.value = true
    const res = await getTimeline(feedPage.value, 10, feedSort.value)
    const data = res.data.data || []
    if (isRefresh) followingPosts.value = data
    else followingPosts.value = [...followingPosts.value, ...data]
    feedHasMore.value = data.length >= 10
    if (isAdmin.value && data.length) fetchReportCountsForPosts(data.map((p) => p.id))
  } catch {} finally { feedLoading.value = false }
}

async function loadInvites(keyword?: string) {
  try { await inviteStore.fetchInvites(getInviteTypeFromRoute(), undefined, 'year', keyword, true) } catch {}
}

async function loadLevelInfo() {
  try { levelInfo.value = (await getLevelInfo()).data.data } catch {}
}

function findPostById(postId: number): FeedPost | undefined {
  return posts.value.find(p => p.id === postId) || followingPosts.value.find(p => p.id === postId) || likedPosts.value.find(p => p.id === postId)
}

async function handleLike(postId: number, liked: boolean) {
  try {
    if (liked) {
      await unlikePost(postId)
      const post = findPostById(postId)
      if (post) { post.liked = false; post.likeCount-- }
      if (activeTab.value === 'liked') likedPosts.value = likedPosts.value.filter(p => p.id !== postId)
    } else {
      await likePost(postId)
      const post = findPostById(postId)
      if (post) { post.liked = true; post.likeCount++ }
    }
  } catch { ElMessage.error('交互异常') }
}

function formatTime(timeStr: string): string { return formatRelativeTime(timeStr) }

async function handlePost() {
  if (!isAdmin.value) return ElMessage.info(postDisabledTip)
  if (!postContent.value.trim() && !uploadedImages.value.length && !uploadedVideos.value.length && selectedInviteId.value == null) {
    return ElMessage.warning('发送内容不能为空')
  }
  posting.value = true
  try {
    const res = await createDiscoveryPost({
      content: postContent.value.trim(),
      images: uploadedImages.value.length ? uploadedImages.value.join(',') : undefined,
      videos: uploadedVideos.value.length ? uploadedVideos.value.join(',') : undefined,
      inviteId: selectedInviteId.value,
      visibility: postVisibility.value || 'ALL',
    })
    posts.value.unshift(res.data.data)
    closePostDialog()
    await loadLevelInfo()
    ElMessage.success('发射成功')
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '发射失败')
  } finally { posting.value = false }
}

function closePostDialog() {
  showPostDialog.value = false
  postContent.value = ''
  postVisibility.value = userStore.user?.feedVisibility ?? 'ALL'
  uploadedImages.value = []
  uploadedVideos.value = []
  selectedInviteId.value = undefined
}

async function handleMediaSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files) return
  let imageSlots = FEED_POST_MAX_IMAGES - uploadedImages.value.length
  for (const file of Array.from(files)) {
    const isVideo = file.type.startsWith('video/')
    if (isVideo) {
      if (file.size > 120 * 1024 * 1024) continue
      try {
        const path = (await uploadVideo(file)).data.data
        if (path) uploadedVideos.value.push(path)
      } catch {}
    } else {
      if (imageSlots <= 0 || file.size > 25 * 1024 * 1024) continue
      try {
        const toSend = await compressImageFile(file)
        const path = (await uploadImage(toSend)).data.data
        if (path) { uploadedImages.value.push(path); imageSlots-- }
      } catch {}
    }
  }
  target.value = ''
}

function removeImage(index: number) { uploadedImages.value.splice(index, 1) }
function removeVideo(index: number) { uploadedVideos.value.splice(index, 1) }

function isExpanded(postId: number): boolean { return expandedPosts.value.get(postId) || false }
function toggleExpand(postId: number) { expandedPosts.value.set(postId, !(expandedPosts.value.get(postId) || false)) }
function getDisplayContent(content: string, postId: number): string { return isExpanded(postId) || content.length <= CONTENT_MAX_LENGTH ? content : content.slice(0, CONTENT_MAX_LENGTH) + '...' }
function shouldCollapse(content: string): boolean { return content.length > CONTENT_MAX_LENGTH }

async function handleReportClick(targetId: number, targetType: string) {
  try {
    const res = await checkReported(targetType, targetId)
    if (res.data.data) return ElMessage.info('您已提交过反馈')
    reportTargetType.value = targetType
    reportTargetId.value = targetId
    showReportDialog.value = true
  } catch {}
}

function onReportSuccess() { reportedPostIds.value.add(reportTargetId.value); reportedPostIds.value = new Set(reportedPostIds.value) }

async function handlePinPost(post: FeedPost) {
  try {
    if (post.pinnedAt) {
      await unpinPost(post.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinPost(post.id)
      ElMessage.success('已置顶')
    }
    await refreshPosts()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

async function handleDeletePost(postId: number) {
  try {
    await ElMessageBox.confirm('确定消除这片宇宙的痕迹？', '提示', { type: 'warning' })
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    followingPosts.value = followingPosts.value.filter(p => p.id !== postId)
  } catch {}
}
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 暖白玻璃 UI
   ========================================== */

$bg-aurora: #f8fafc; // 清透冷白底色
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b; // 碳黑，比纯黑柔和
$text-sub: #64748b;  // 冷灰
$border-light: rgba(255, 255, 255, 0.8);

.discover-page {
  min-height: 100vh;
  background: $bg-aurora;
  color: $text-main;
  padding-bottom: 80px;
  position: relative;
  
  // 晨曦极光背景弥散
  &::before {
    content: ''; position: fixed; inset: 0; pointer-events: none;
    background: 
      radial-gradient(circle at 10% 10%, rgba(79, 140, 255, 0.12), transparent 45%),
      radial-gradient(circle at 90% 60%, rgba(255, 51, 102, 0.08), transparent 45%),
      radial-gradient(circle at 50% 100%, rgba(255, 123, 84, 0.06), transparent 50%);
    z-index: 0;
  }
}

// 通用的明亮毛玻璃面板
.glass-panel {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
}

.glass-pill {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 999px;
}

// 顶部悬浮胶囊区 (Tuner Wrap)
.glass-tuner-wrap {
  position: sticky; top: 16px; z-index: 50;
  display: flex; flex-direction: column; gap: 12px;
  max-width: 600px; margin: 0 auto; padding: 0 16px;
}

.search-wrap { position: relative; width: 100%; }

.glass-search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(16px);
  border-radius: 999px; border: 1px solid rgba(255, 255, 255, 1);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.02); height: 44px;
  
  .el-input__inner { color: $text-main; &::placeholder { color: #94a3b8; } }
  .el-icon { color: $text-sub; }
  
  &.is-focus { 
    border-color: rgba($accent-pink, 0.4); 
    box-shadow: 0 0 0 2px rgba($accent-pink, 0.1), inset 0 2px 6px rgba(0, 0, 0, 0.02);
  }
}

.tuner-capsule {
  display: flex; align-items: center; justify-content: space-between;
  border-radius: 999px; padding: 6px; gap: 8px;
}

.tuner-tabs { display: flex; flex: 1; position: relative; gap: 4px; }

.tuner-btn {
  flex: 1; padding: 10px 0; border-radius: 999px; border: none;
  background: transparent; color: $text-sub; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  
  &:hover { color: $text-main; }
  &.active {
    color: $accent-pink;
    background: linear-gradient(135deg, rgba(79, 140, 255, 0.08), rgba(255, 51, 102, 0.08));
    box-shadow: 0 2px 8px rgba(255, 51, 102, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  }
}

.tuner-sort-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: rgba(255, 255, 255, 0.5); color: $text-sub;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
  &:hover { background: #fff; color: $text-main; transform: rotate(90deg); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
}

// 动态卡片 (Feed Card)
.timeline-list {
  max-width: 680px; margin: 24px auto 0; padding: 0 16px;
  display: flex; flex-direction: column; gap: 20px;
}

.feed-card {
  padding: 24px; border-radius: 28px; position: relative;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid #ffffff;
  box-shadow: 0 10px 30px rgba(31, 38, 135, 0.06);
  transition: transform 0.3s, box-shadow 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 15px 40px rgba(31, 38, 135, 0.1);
  }

  &.feed-card--pinned {
    border-color: rgba($accent-pink, 0.35);
    box-shadow: 0 10px 30px rgba(255, 51, 102, 0.08);
  }
}

.feed-pinned-badge {
  position: absolute;
  top: 16px;
  right: 18px;
  font-size: 12px;
  font-weight: 700;
  color: $accent-pink;
  letter-spacing: 0.05em;
}

.feed-pin-text-btn {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba($accent-pink, 0.35);
  background: rgba(255, 255, 255, 0.85);
  color: $accent-pink;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s, border-color 0.2s;
  &:hover {
    background: rgba(255, 51, 102, 0.08);
    border-color: rgba($accent-pink, 0.55);
  }
  &.is-pinned {
    border-color: rgba($accent-pink, 0.55);
    background: rgba(255, 51, 102, 0.1);
  }
}

.feed-header { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }

// 头像光晕效果 (适配白底，光晕需更实一点)
.avatar-glow-wrap {
  position: relative; border-radius: 50%; padding: 2px;
  background: var(--glow-color); 
  box-shadow: 0 4px 12px var(--glow-color);
  .feed-avatar { width: 42px; height: 42px; border-radius: 50%; border: 2px solid #fff; }
}

.feed-user { flex: 1; }
.feed-name { font-size: 16px; font-weight: 700; color: $text-main; display: flex; align-items: center; gap: 8px; }
.feed-time { font-size: 12px; color: #94a3b8; margin-top: 2px; }

.feed-icon-btn {
  background: transparent; border: none; color: #94a3b8; padding: 6px; cursor: pointer;
  border-radius: 50%; transition: all 0.2s;
  &:hover { background: rgba(0, 0, 0, 0.04); color: $text-main; }
  &.text-warning:hover { color: #e6a23c; background: rgba(#e6a23c, 0.1); }
  &.text-danger:hover { color: #f56c6c; background: rgba(#f56c6c, 0.1); }
}

.feed-content { font-size: 15px; line-height: 1.8; color: #334155; margin-bottom: 16px; }
.feed-text { white-space: pre-wrap; word-wrap: break-word; }
.expand-btn { color: $accent-blue; background: none; border: none; cursor: pointer; font-size: 14px; font-weight: 600; }

.feed-images {
  display: grid; gap: 6px; margin-bottom: 16px; border-radius: 12px; overflow: hidden;
  grid-template-columns: repeat(4, 1fr);
  .feed-image { width: 100%; height: 120px; object-fit: cover; cursor: pointer; transition: transform 0.3s; &:hover { transform: scale(1.05); } }
}

// 底部互动栏
.feed-actions { display: flex; gap: 12px; border-top: 1px solid rgba(0, 0, 0, 0.04); padding-top: 16px; }
.action-btn {
  display: flex; align-items: center; gap: 6px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  border-radius: 999px; padding: 8px 16px; color: $text-sub; font-size: 14px; cursor: pointer;
  position: relative; overflow: hidden; transition: all 0.3s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
  
  &:hover { background: #fff; color: $text-main; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
  
  &.like-btn.active {
    color: $accent-pink; background: rgba(255, 51, 102, 0.05); border-color: rgba(255, 51, 102, 0.15);
    .el-icon { animation: heart-burst 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
    .like-ripple {
      position: absolute; left: 50%; top: 50%; width: 10px; height: 10px;
      background: rgba(255, 51, 102, 0.5); border-radius: 50%; transform: translate(-50%, -50%);
      animation: ripple-out 0.6s ease-out forwards; pointer-events: none;
    }
  }
}

@keyframes heart-burst { 0% { transform: scale(1); } 50% { transform: scale(1.4); } 100% { transform: scale(1); } }
@keyframes ripple-out { 0% { width: 0; height: 0; opacity: 0.8; } 100% { width: 100px; height: 100px; opacity: 0; } }

// 右下角悬浮 FAB (保持高饱和度的温暖渐变，作为视觉锚点)
.fab-container { position: fixed; bottom: 100px; right: 24px; z-index: 100; }
.glow-btn-warm {
  width: 56px; height: 56px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white;
  display: flex; align-items: center; justify-content: center; font-size: 24px; cursor: pointer;
  box-shadow: 0 10px 25px rgba(255, 51, 102, 0.4); transition: transform 0.3s, box-shadow 0.3s;
  &:hover { transform: translateY(-4px) scale(1.05); box-shadow: 0 15px 35px rgba(255, 51, 102, 0.5); }
  &.disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.8; }
}

// 雷达空状态 (白底版)
.empty-radar-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center; height: 50vh;
}
.radar-waves {
  position: relative; width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; margin-bottom: 24px;
  .wave {
    position: absolute; border: 1px solid rgba(79, 140, 255, 0.3); border-radius: 50%;
    animation: radar-ping 3s cubic-bezier(0, 0.2, 0.8, 1) infinite;
  }
  .wave-1 { animation-delay: 0s; } .wave-2 { animation-delay: 1s; } .wave-3 { animation-delay: 2s; }
  .radar-core {
    width: 48px; height: 48px; background: rgba(79, 140, 255, 0.1); border-radius: 50%;
    display: flex; align-items: center; justify-content: center; color: $accent-blue; font-size: 24px;
    box-shadow: 0 0 20px rgba(79, 140, 255, 0.2); z-index: 2;
  }
}
@keyframes radar-ping { 0% { width: 40px; height: 40px; opacity: 1; } 100% { width: 200px; height: 200px; opacity: 0; } }
.empty-title { font-size: 16px; color: $text-main; font-weight: 700; margin-bottom: 8px; }
.empty-subtitle { font-size: 13px; color: $text-sub; }

// 其他复用
.sentinel { height: 60px; display: flex; justify-content: center; align-items: center; color: $text-sub; font-size: 14px; }
.spinning { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

// 对话框明亮玻璃主题穿透
:deep(.light-glass-dialog) {
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(24px);
  border: 1px solid #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.1);
  .el-dialog__title { color: $text-main; font-weight: 700; }
  .el-form-item__label { color: $text-main; font-weight: 600; }
  .el-textarea__inner { background: rgba(255,255,255,0.8); border: 1px solid #e2e8f0; color: $text-main; }
}

// 标签样式调整
.ai-tag {
  font-size: 12px; color: $accent-blue;
  background: rgba(79, 140, 255, 0.1); border: 1px solid rgba(79, 140, 255, 0.2);
  padding: 4px 10px; border-radius: 999px;
  white-space: nowrap;
  display: inline-block;
}
.feed-ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>