<template>
  <div class="discover-page">
    <div class="page-header">
      <!-- 顶部搜索与发布 -->
      <div class="top-bar">
        <div ref="searchBoxRef" class="search-wrap">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户、帖子、邀约..."
            class="search-input"
            clearable
            @keyup.enter="doSearch"
            @focus="searchKeyword.trim().length >= 2 && (showUserDropdown = true)"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <!-- 用户搜索下拉 -->
          <div v-if="showUserDropdown" class="user-search-dropdown">
            <div v-if="userSearchLoading" class="usd-hint">搜索中...</div>
            <div v-else-if="searchKeyword.trim().length < 2" class="usd-hint">输入至少 2 个字符</div>
            <div v-else-if="!userSearchResults.length" class="usd-hint">未找到用户，按回车搜索帖子</div>
            <template v-else>
              <div class="usd-section-label">用户</div>
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
                <el-icon><Search /></el-icon> 搜索「{{ searchKeyword.trim() }}」相关帖子
              </div>
            </template>
          </div>
        </div>
        <button class="btn-primary post-btn" @click="showPostDialog = true">
          <el-icon><Plus /></el-icon> 发布
        </button>
      </div>

      <div class="discover-nav-row">
        <!-- Primary tabs -->
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

        <el-dropdown
          v-if="activeTab === 'recommend' || activeTab === 'following'"
          trigger="click"
          placement="bottom-end"
          @command="handleSortCommand"
        >
          <button class="sort-trigger-btn" type="button" title="排序">
            <el-icon><Operation /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu class="sort-dropdown-menu">
              <el-dropdown-item :class="{ 'is-active': feedSort === 'recommend' }" command="recommend">
                推荐
              </el-dropdown-item>
              <el-dropdown-item :class="{ 'is-active': feedSort === 'time' }" command="time">
                最新
              </el-dropdown-item>
              <el-dropdown-item :class="{ 'is-active': feedSort === 'hot' }" command="hot">
                热度
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div v-if="pullRefreshVisible" class="pull-refresh-indicator">
      <el-icon class="spinning"><Loading /></el-icon> 刷新中...
    </div>

    <!-- 动态刷新工具栏（推荐/关注/动态 tab，仅 PC 显示） -->
    <div v-if="activeTab !== 'liked'" class="load-toolbar">
      <button class="btn-refresh" :disabled="feedLoading" @click="refreshPosts">
        <el-icon :class="{ spinning: feedLoading }"><Refresh /></el-icon>
        {{ feedLoading ? '加载中...' : '刷新' }}
      </button>
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
          <div v-if="item.post.pinned" class="feed-pinned-badge">置顶</div>
          <div class="feed-header" @click.stop>
            <el-image
              :src="getMediaUrl(item.post.avatarUrl) || defaultAvatar"
              class="feed-avatar"
              :preview-src-list="[getMediaUrl(item.post.avatarUrl) || defaultAvatar]"
              preview-teleported
              fit="cover"
            />
            <div class="feed-user" @click="$router.push(`/profile/${item.post.userId}`)">
              <div class="feed-name">{{ item.post.nickname }}</div>
              <div class="feed-time">{{ formatTime(item.post.createdAt) }}</div>
            </div>
            <button
              v-if="isAdmin && (reportCountByPostId[item.post.id] || 0) > 0"
              type="button"
              class="feed-report-count-badge"
              :title="`${reportCountByPostId[item.post.id]} 条举报，点击查看`"
              @click.stop="$router.push(`/admin/reports?targetId=${item.post.id}`)"
            >
              <el-icon><WarningFilled /></el-icon>
              <span>{{ reportCountByPostId[item.post.id] }}</span>
            </button>
            <button
              type="button"
              :class="['feed-report-btn', { reported: reportedPostIds.has(item.post.id) }]"
              :title="reportedPostIds.has(item.post.id) ? '已举报' : '举报'"
              @click.stop="handleReportClick(item.post.id, 'POST')"
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
            <button
              v-if="isAdmin"
              type="button"
              :class="['feed-pin-btn', { pinned: item.post.pinned }]"
              :title="item.post.pinned ? '取消置顶' : '置顶'"
              @click.stop="handlePinPost(item.post)"
            >
              <el-icon><Sort /></el-icon>
              <span>{{ item.post.pinned ? '取消置顶' : '置顶' }}</span>
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
              loading="lazy"
              @click.stop="openImagePreview(item.post.images.split(',').map(i => getMediaUrl(i)), idx)"
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
          <!-- AI 标签 -->
          <div v-if="item.post.aiTags" class="feed-ai-tags">
            <span
              v-for="tag in (item.post.aiTags || '').split(/[,，]/).filter(Boolean)"
              :key="tag"
              class="ai-tag"
            >{{ tag.trim() }}</span>
          </div>
          <div class="feed-actions" @click.stop>
            <button
              :class="['action-btn', { active: item.post.liked }]"
              @click="handleLike(item.post.id, item.post.liked)"
            >
              <el-icon :size="18"><StarFilled v-if="item.post.liked" /><Star v-else /></el-icon>
              <span>{{ item.post.likeCount }}</span>
            </button>
            <button class="action-btn" @click="goPostDetail(item.post.id)">
              <el-icon :size="18"><ChatDotRound /></el-icon>
              <span>{{ item.post.commentCount }}</span>
            </button>
            <button class="action-btn" @click="openShareDialog(item.post)">
              <el-icon :size="18"><Share /></el-icon>
              <span>分享</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布动态弹窗 -->
    <el-dialog v-model="showPostDialog" title="发布动态" width="560px" :close-on-click-modal="false" destroy-on-close>
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

          <!-- 上传按钮：统一图片/视频选择 -->
          <div class="media-actions">
            <input ref="mediaInputRef" type="file" accept="image/*,video/*" multiple hidden @change="handleMediaSelect" />
            <button type="button" class="media-btn" @click="mediaInputRef?.click()">
              <el-icon><Picture /></el-icon>
              <span>图片/视频</span>
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

    <!-- 加载更多指示（sentinel for IntersectionObserver） -->
    <div ref="sentinelRef" class="sentinel">
      <div v-if="feedLoading && timelineItems.length" class="loading-more">
        <el-icon class="spinning"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="!feedHasMore && timelineItems.length" class="no-more">
        已加载全部内容
      </div>
    </div>

    <div v-if="!timelineItems.length && !feedLoading" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无内容</p>
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
      :post="currentSharePost"
      @success="handleShareSuccess"
    />

    <!-- 图片全屏预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      teleported
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getDiscoveryPosts,
  getTimeline,
  getLikedPosts,
  likePost,
  unlikePost,
  pinPost,
  unpinPost,
  getLevelInfo,
  createDiscoveryPost,
  deletePost,
  uploadImage,
  uploadVideo,
  type FeedPost,
  type UserLevelInfo,
} from '@/api/feedApi'
import { checkReported, getMyReport, getReportCountByPostIds, VIOLATION_TYPES } from '@/api/reportApi'
import ReportDialog from '@/components/ReportDialog.vue'
import { useUserStore } from '@/store/userStore'
import { useFollowStore } from '@/store/followStore'
import { useInviteStore } from '@/store/inviteStore'
import { followUser, unfollowUser } from '@/api/followApi'
import { searchUsers, type UserSearchItem } from '@/api/userApi'
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus'
import { Plus, Delete, Search, Flag, WarningFilled, Sort, Refresh, Loading, Operation } from '@element-plus/icons-vue'
import type { Invite } from '@/api/inviteApi'
import ShareDialog from '@/components/ShareDialog.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { DEFAULT_AVATAR, getMediaUrl, formatRelativeTime } from '@/utils/shared'

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
  } catch { /* handled by interceptor */ }
}

function closeUserDropdown(e: MouseEvent) {
  if (searchBoxRef.value && !searchBoxRef.value.contains(e.target as Node)) {
    showUserDropdown.value = false
  }
}

// 用户搜索 document click 监听合并到下面的 onMounted
const posts = ref<FeedPost[]>([])
const followingPosts = ref<FeedPost[]>([])
const likedPosts = ref<FeedPost[]>([])
// 帖子展开状态 Map
const expandedPosts = ref<Map<number, boolean>>(new Map())
// 文字内容折叠配置
const CONTENT_MAX_LENGTH = 100

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
const activeTab = ref<'recommend' | 'post' | 'following' | 'liked'>('recommend')
const feedSort = ref<'recommend' | 'hot' | 'time'>('recommend')

// 分页状态
const feedPage = ref(0)
const feedHasMore = ref(true)
const feedLoading = ref(false)
const sentinelRef = ref<HTMLElement>()
let scrollObserver: IntersectionObserver | null = null

// 触摸下拉刷新状态（手机端）
let touchStartY = 0
let isPullRefreshing = false
const pullRefreshVisible = ref(false)

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
const mediaInputRef = ref<HTMLInputElement>()

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
  const id = Number(postId)
  if (!Number.isFinite(id) || id <= 0) return
  router.push(`/feed/${id}`)
}

const isAdmin = computed(() => userStore.user?.isAdmin || false)

// 管理员：帖子举报数量（postId -> count）
const reportCountByPostId = ref<Record<number, number>>({})

async function fetchReportCountsForPosts(postIds: number[]) {
  if (!isAdmin.value || !postIds.length) return
  try {
    const res = await getReportCountByPostIds(postIds)
    reportCountByPostId.value = { ...reportCountByPostId.value, ...res.data.data }
  } catch { /* ignore */ }
}

// 判断是否可以删除帖子（管理员或帖子作者）
function canDeletePost(post: FeedPost): boolean {
  const admin = userStore.user?.isAdmin || false
  const isOwner = post.userId === userStore.user?.id
  return admin || isOwner
}

// 从 URL 读取邀约类型筛选（热门邀约看板跳转时传入）
function getInviteTypeFromRoute(): string | undefined {
  const t = route.query.type
  return typeof t === 'string' && t ? t : undefined
}

function switchTab(tab: 'recommend' | 'post' | 'following' | 'liked') {
  activeTab.value = tab
  refreshPosts()
}

function setFeedSort(sort: 'recommend' | 'hot' | 'time') {
  feedSort.value = sort
  refreshPosts()
}

function handleSortCommand(command: string | number | object) {
  if (command === 'recommend' || command === 'time' || command === 'hot') {
    setFeedSort(command)
  }
}

function doSearch() {
  refreshPosts()
}

/** 刷新：清空列表，从第 0 页重新拉取 */
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

/** 加载更多（追加）：上划触底触发 */
async function loadMorePosts() {
  if (feedLoading.value || !feedHasMore.value) return
  if (activeTab.value === 'liked') return // 点赞列表不分页加载更多
  feedPage.value++
  await loadByTab(false)
}

async function loadLikedPosts(isRefresh = false) {
  if (isRefresh) likedPosts.value = []
  try {
    feedLoading.value = true
    const res = await getLikedPosts(feedPage.value, 10)
    const data = res.data.data || []
    if (isRefresh) {
      likedPosts.value = data
    } else {
      likedPosts.value = [...likedPosts.value, ...data]
    }
    feedHasMore.value = data.length >= 10
    if (isAdmin.value && data.length) {
      fetchReportCountsForPosts(data.map((p) => p.id))
    }
  } catch { /* empty */ } finally {
    feedLoading.value = false
  }
}

onMounted(async () => {
  await loadLevelInfo()
  await refreshPosts()
  // IntersectionObserver：上划触底时加载更多
  scrollObserver = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && !feedLoading.value && feedHasMore.value) {
        loadMorePosts()
      }
    },
    { rootMargin: '120px' }
  )
  if (sentinelRef.value) scrollObserver.observe(sentinelRef.value)
  // 手机端下拉刷新（触摸事件）
  document.addEventListener('touchstart', onTouchStart, { passive: true })
  document.addEventListener('touchend', onTouchEnd, { passive: true })
  // 用户搜索下拉框点击外关闭
  document.addEventListener('click', closeUserDropdown)
})

onUnmounted(() => {
  scrollObserver?.disconnect()
  document.removeEventListener('touchstart', onTouchStart)
  document.removeEventListener('touchend', onTouchEnd)
  document.removeEventListener('click', closeUserDropdown)
  if (userSearchTimer) clearTimeout(userSearchTimer)
})

function onTouchStart(e: TouchEvent) {
  touchStartY = e.touches[0]?.clientY ?? 0
  isPullRefreshing = false
}

function onTouchEnd(e: TouchEvent) {
  if (!touchStartY) return
  const endY = e.changedTouches[0]?.clientY ?? 0
  const delta = endY - touchStartY
  // 下拉距离 > 80px 且处于页面顶部 → 刷新
  if (delta > 80 && window.scrollY < 10 && !isPullRefreshing && !feedLoading.value) {
    isPullRefreshing = true
    pullRefreshVisible.value = true
    refreshPosts().finally(() => {
      pullRefreshVisible.value = false
    })
  }
  touchStartY = 0
}

// 路由 query.type 变化时重新拉取邀约
watch(() => route.query.type, () => {
  loadInvites(searchKeyword.value.trim() || undefined)
})

// 发布新帖后插入到列表头部，不刷新分页


type TimelineItem = { kind: 'post'; post: FeedPost; time: string; key: string }

const timelineItems = computed<TimelineItem[]>(() => {
  let postList: FeedPost[] = []
  if (activeTab.value === 'following') {
    postList = followingPosts.value
  } else if (activeTab.value === 'liked') {
    postList = likedPosts.value
  } else {
    postList = posts.value
  }
  // 保持后端返回的排序顺序（recommend/hot/time 均由后端处理）
  return postList.map(post => ({
    kind: 'post' as const,
    post,
    time: post.createdAt,
    key: `post-${post.id}`,
  }))
})


async function loadPosts(keyword?: string, isRefresh = false) {
  if (feedLoading.value && !isRefresh) return
  try {
    feedLoading.value = true
    const res = await getDiscoveryPosts(feedPage.value, 10, feedSort.value, keyword)
    const data = res.data.data || []
    if (isRefresh) {
      posts.value = data
    } else {
      posts.value = [...posts.value, ...data]
    }
    feedHasMore.value = data.length >= 10
    if (isAdmin.value && data.length) {
      fetchReportCountsForPosts(data.map((p) => p.id))
    }
  } catch { /* empty */ } finally {
    feedLoading.value = false
  }
}

async function loadFollowingPosts(isRefresh = false) {
  if (feedLoading.value && !isRefresh) return
  try {
    feedLoading.value = true
    const res = await getTimeline(feedPage.value, 10, feedSort.value)
    const data = res.data.data || []
    if (isRefresh) {
      followingPosts.value = data
    } else {
      followingPosts.value = [...followingPosts.value, ...data]
    }
    feedHasMore.value = data.length >= 10
    if (isAdmin.value && data.length) {
      fetchReportCountsForPosts(data.map((p) => p.id))
    }
  } catch { /* empty */ } finally {
    feedLoading.value = false
  }
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
  return formatRelativeTime(timeStr)
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
async function handleMediaSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files) return

  for (const file of Array.from(files)) {
    const isVideo = file.type.startsWith('video/')
    if (isVideo) {
      if (file.size > 100 * 1024 * 1024) {
        ElMessage.warning('视频大小不能超过100MB')
        continue
      }
      try {
        ElMessage.info('视频上传中，请稍候...')
        const res = await uploadVideo(file)
        const path = res.data.data
        if (path) uploadedVideos.value.push(path)
        ElMessage.success('视频上传成功')
      } catch (err) {
        ElMessage.error('视频上传失败')
      }
    } else {
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning('图片大小不能超过10MB')
        continue
      }
      try {
        ElMessage.info('上传中...')
        const res = await uploadImage(file)
        const path = res.data.data
        if (path) uploadedImages.value.push(path)
        ElMessage.success('图片上传成功')
      } catch (err) {
        ElMessage.error('图片上传失败')
      }
    }
  }
  target.value = ''
}

function removeImage(index: number) {
  uploadedImages.value.splice(index, 1)
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

async function handleReportClick(targetId: number, targetType: string) {
  try {
    const res = await checkReported(targetType, targetId)
    if (res.data.data) {
      const myRes = await getMyReport(targetType, targetId)
      const info = myRes.data.data
      const types = info?.violationTypes ? info.violationTypes.split(',') : []
      const reason = info?.reason || ''
      const adminNote = info?.adminNote || ''
      const labels = types.map(t => VIOLATION_TYPES.find(v => v.value === t)?.label || t).filter(Boolean)
      let msg = labels.length ? `举报类型：${labels.join('、')}${reason ? `\n理由：${reason}` : ''}` : '您已举报过该内容'
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
  reportedPostIds.value.add(reportTargetId.value)
  reportedPostIds.value = new Set(reportedPostIds.value)
}

async function handleDeletePost(postId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条动态？', '提示', { type: 'warning' })
    await deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    followingPosts.value = followingPosts.value.filter(p => p.id !== postId)
    ElMessage.success('已删除')
  } catch {
    // 用户取消或删除失败
  }
}

async function handlePinPost(post: FeedPost) {
  try {
    if (post.pinned) {
      await unpinPost(post.id)
      post.pinned = false
      post.pinnedAt = null
      ElMessage.success('已取消置顶')
    } else {
      await pinPost(post.id)
      post.pinned = true
      post.pinnedAt = new Date().toISOString()
      ElMessage.success('已置顶')
    }
  } catch {
    ElMessage.error('操作失败')
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

.search-wrap {
  position: relative;
  width: 280px;
  flex: 1;
  min-width: 0;
  max-width: 340px;
}

.search-input {
  width: 100%;

  :deep(.el-input__wrapper) {
    border-radius: $radius-full;
    box-shadow: 0 0 0 1px $border-light;
  }
}

// 用户搜索下拉框
.user-search-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 100;
  background: $bg-primary;
  border-radius: $radius-lg;
  box-shadow: 0 8px 24px rgba(215, 127, 162, 0.16), 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid $border-light;
  overflow: hidden;
  max-height: 320px;
  overflow-y: auto;
}

.usd-section-label {
  padding: 8px 14px 4px;
  font-size: 11px;
  font-weight: 600;
  color: $text-muted;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.usd-hint {
  padding: 12px 14px;
  font-size: 13px;
  color: $text-muted;
}

.usd-search-posts {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: $primary;
  font-weight: 500;

  &:hover {
    background: rgba($primary, 0.06);
  }
}

.usd-divider {
  height: 1px;
  background: $border-light;
  margin: 4px 0;
}

.usd-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: rgba($primary, 0.05);
  }
}

.usd-avatar {
  flex-shrink: 0;
}

.usd-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.usd-follow-btn {
  flex-shrink: 0;
  padding: 4px 12px;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
  border: 1.5px solid $primary;
  color: $primary;
  background: transparent;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.1);
  }

  &.followed {
    border-color: $border-color;
    color: $text-muted;
    background: $bg-tertiary;

    &:hover {
      border-color: $danger;
      color: $danger;
      background: rgba($danger, 0.06);
    }
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

.discover-nav-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 10px;
  width: 100%;
  padding-bottom: 12px;
}

.discover-tabs {
  display: flex;
  flex: 1;
  min-width: 0;
  width: 100%;
  gap: 8px;
  padding: 4px;
  background: $bg-secondary;
  border: 1px solid $border-light;
  border-radius: $radius-full;
}

.tab-btn {
  flex: 1 1 0;
  min-width: 0;
  padding: 10px 12px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 15px;
  font-weight: 600;
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

.sort-trigger-btn {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: end;
  flex-shrink: 0;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  background: $bg-primary;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &:hover {
    color: $primary;
    border-color: rgba($primary, 0.3);
    background: rgba($primary, 0.06);
  }

  .el-icon {
    font-size: 18px;
  }
}

:deep(.sort-dropdown-menu .el-dropdown-menu__item.is-active) {
  color: $primary;
  font-weight: 600;
  background: rgba($primary, 0.08);
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
  animation: card-enter 0.35s ease both;
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Stagger entrance for timeline items
@for $i from 1 through 10 {
  .timeline-item:nth-child(#{$i}) {
    animation-delay: #{$i * 0.05}s;
  }
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
  position: relative;
  background: $bg-primary;
  border-radius: $radius-xl;
  padding: 20px;
  cursor: pointer;
  border: none;
  box-shadow: $shadow-sm;
  transition: transform $transition-fast, box-shadow $transition-fast;
  contain: content;

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
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;

  :deep(.el-image__inner) {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  :deep(.el-image__error) {
    width: 100%;
    height: 100%;
  }
}

.feed-user {
  flex: 1;
  cursor: pointer;
}

.feed-report-count-badge {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  border: none;
  background: rgba(#e65100, 0.12);
  color: #e65100;
  font-size: 12px;
  font-weight: 500;
  border-radius: $radius-md;
  cursor: pointer;
  &:hover { background: rgba(#e65100, 0.2); }
}

.feed-report-btn {
  display: flex;
  &.reported {
    color: var(--el-color-primary);
  }
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
  white-space: nowrap;
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

.feed-pinned-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  color: $primary;
  background: rgba($primary, 0.12);
  border-radius: $radius-md;
}

.feed-pin-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  white-space: nowrap;
  border: none;
  background: transparent;
  color: $text-muted;
  font-size: 13px;
  cursor: pointer;
  border-radius: $radius-md;
  transition: color $transition-fast, background $transition-fast;

  &:hover {
    color: $primary;
    background: rgba($primary, 0.08);
  }

  &.pinned {
    color: $primary;
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
  display: grid;
  gap: 6px;
  margin-bottom: 12px;

  // Single image: full width
  &:has(.feed-image:only-child) {
    grid-template-columns: 1fr;
    .feed-image { width: 100%; height: auto; max-height: 320px; }
  }

  // Two images: side by side
  &:has(.feed-image:nth-child(2):last-child) {
    grid-template-columns: 1fr 1fr;
    .feed-image { width: 100%; height: 180px; }
  }

  // Three+ images: first large, rest small
  &:has(.feed-image:nth-child(3)) {
    grid-template-columns: 2fr 1fr;
    grid-template-rows: auto auto;
    .feed-image:first-child { grid-row: 1 / 3; height: 200px; }
    .feed-image { width: 100%; height: 96px; }
  }
}

.feed-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: $radius-md;
  cursor: pointer;
  transition: transform $transition-fast;

  &:hover { transform: scale(1.02); }
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
  border-radius: $radius-md;

  &:hover {
    color: $primary;
    background: rgba($primary, 0.05);

    .el-icon { transform: scale(1.15); }
  }

  .el-icon { transition: transform $transition-fast; }

  &.active {
    color: $primary;

    .el-icon { animation: heartbeat 0.4s ease; }
  }

  &.delete-btn { color: $text-muted; }
  &.delete-btn:hover { color: $primary; }
}

@keyframes heartbeat {
  0% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.95); }
  75% { transform: scale(1.15); }
  100% { transform: scale(1); }
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

.load-toolbar {
  display: flex;
  justify-content: center;
  padding: 8px 24px 0;

  // 手机端隐藏刷新按钮（手机用下拉刷新）
  @media (max-width: $bp-mobile) {
    display: none;
  }
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 18px;
  background: transparent;
  border: 1px solid $border-light;
  border-radius: $radius-full;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: rgba($primary, 0.08);
    color: $primary;
    border-color: $primary;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// 手机端下拉刷新指示器（PC 端不显示）
.pull-refresh-indicator {
  display: none;

  @media (max-width: $bp-mobile) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px;
    font-size: 13px;
    color: $text-secondary;
  }
}

.sentinel {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 8px 0;
}

.loading-more, .no-more {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-muted;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning {
  animation: spin 1s linear infinite;
}

@media (max-width: $bp-mobile) {
  .discover-page {
    padding: 0 12px 12px;
  }

  .page-header {
    padding: 14px 12px 0;
  }

  .top-bar {
    gap: 10px;
    margin-bottom: 12px;
  }

  .search-wrap {
    width: 100%;
    max-width: none;
    flex: 1;
  }

  .search-input {
    width: 100%;
  }

  .user-search-dropdown {
    max-height: 260px;
  }

  .post-btn {
    flex-shrink: 0;
    padding: 8px 14px;
    font-size: 13px;
  }

  .discover-nav-row {
    grid-template-columns: minmax(0, 1fr) auto;
    column-gap: 8px;
    padding-bottom: 10px;
  }

  .discover-tabs {
    width: 100%;
    gap: 4px;
    padding: 3px;
  }

  .tab-btn {
    flex: 1 1 0;
    font-size: 13px;
  }

  .tab-btn {
    padding: 8px 10px;
  }

  .sort-trigger-btn {
    width: 38px;
    height: 38px;
  }

  .timeline-list {
    padding: 12px 0 16px;
  }

  .feed-card {
    padding: 16px;
  }

  .feed-header {
    gap: 8px;
    flex-wrap: wrap;
  }

  .feed-avatar {
    width: 40px;
    height: 40px;
  }

  .feed-name {
    font-size: 14px;
  }

  .feed-time,
  .feed-report-count-badge {
    font-size: 11px;
  }

  .feed-report-btn,
  .feed-delete-btn,
  .feed-pin-btn {
    padding: 5px 8px;
    font-size: 12px;
    border-radius: 999px;
  }

  .feed-delete-btn,
  .feed-pin-btn {
    gap: 3px;
  }

  .feed-delete-btn span,
  .feed-pin-btn span {
    white-space: nowrap;
  }

  .feed-actions {
    gap: 10px;
    justify-content: space-between;
  }

  .action-btn {
    gap: 4px;
    padding: 6px 8px;
    font-size: 12px;
    white-space: nowrap;
  }

  .feed-content {
    font-size: 14px;
  }

  .link-card {
    gap: 8px;
    padding: 9px;
  }

  .link-title {
    font-size: 12px;
  }
}
</style>
