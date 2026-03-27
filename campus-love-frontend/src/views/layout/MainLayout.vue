<template>
  <div class="main-layout" :style="schoolColorStyle">
    <div class="global-aurora-bg"></div>

    <aside class="sidebar glass-sidebar">
      <div class="sidebar-inner">
        <router-link to="/" class="logo-link">
          <span class="logo-text text-gradient-warm">Campal</span>
        </router-link>

        <nav class="nav-menu" role="navigation" aria-label="主导航">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            :aria-current="isActive(item.path) ? 'page' : undefined"
          >
            <div class="icon-wrap">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.showDot" class="nav-dot pulse-dot" />
          </router-link>
        </nav>

        <div v-if="userStore.user" class="sidebar-user glass-card-light" @click="$router.push('/profile')">
          <div class="avatar-glow-wrap">
            <AppAvatar :src="userStore.user.avatarUrl" :name="userStore.user.nickname" :size="36" class="avatar" />
          </div>
          <div class="user-info">
            <div class="user-name text-ellipsis">{{ userStore.user.nickname }}</div>
            <div class="user-email text-ellipsis">{{ userStore.user.email }}</div>
          </div>
        </div>
      </div>
    </aside>

    <main class="content-area">
      <router-view v-slot="{ Component, route }">
        <keep-alive :include="['Discover', 'Invite']">
          <component
            :is="Component"
            :key="route.meta.keepAlive ? String(route.name ?? route.path) : route.fullPath"
          />
        </keep-alive>
      </router-view>
    </main>

    <aside class="right-panel glass-sidebar">
      <div ref="searchBoxRef" class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户昵称..."
          class="glass-search-input"
          clearable
          :loading="searchLoading"
          @focus="searchKeyword && doSearch()"
          @clear="clearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div v-if="showSearchDropdown" class="search-dropdown glass-panel">
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
            <AppAvatar :src="u.avatarUrl" :name="u.nickname" :size="32" class="avatar" />
            <span class="search-item-name text-ellipsis">{{ u.nickname }}</span>
            <button
              :class="['btn-sm', followStore.isFollowed(u.id) ? 'btn-followed' : 'glass-btn']"
              @click.stop="handleSearchFollow(u.id)"
            >
              {{ followStore.isFollowed(u.id) ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
      </div>

      <div class="panel-card glass-card-light">
        <h3 class="panel-title"><span class="title-accent"></span>今日推荐</h3>
        <div v-if="topNotFollowedMatches.length" class="recommend-list">
          <div
            v-for="m in topNotFollowedMatches"
            :key="m.userId"
            class="recommend-item"
            @click="$router.push(`/profile/${m.userId}`)"
          >
            <AppAvatar :src="m.avatarUrl" :name="m.nickname" :size="36" class="avatar" />
            <div class="recommend-info">
              <div class="recommend-name text-ellipsis">{{ m.nickname }}</div>
              <div class="recommend-meta text-gradient-warm">匹配度 {{ m.matchScore }}%</div>
            </div>
            <button
              :class="['btn-sm', followStore.isFollowed(m.userId) ? 'btn-followed' : 'glass-btn']"
              @click.stop="handleRecommendFollow(m.userId)"
            >
              {{ followStore.isFollowed(m.userId) ? '已关注' : '关注' }}
            </button>
          </div>
        </div>
        <div v-else class="empty-hint">完善资料后查看推荐</div>
      </div>

      <div class="panel-card glass-card-light">
        <h3 class="panel-title"><span class="title-accent"></span>已关注</h3>
        <div v-if="topFollowedMatches.length" class="recommend-list">
          <div
            v-for="m in topFollowedMatches"
            :key="m.userId"
            class="recommend-item"
            @click="$router.push(`/profile/${m.userId}`)"
          >
            <AppAvatar :src="m.avatarUrl" :name="m.nickname" :size="36" class="avatar" />
            <div class="recommend-info">
              <div class="recommend-name text-ellipsis">{{ followStore.getDisplayName(m.userId, m.nickname) }}</div>
              <div class="recommend-meta text-gradient-warm">匹配度 {{ m.matchScore }}%</div>
            </div>
            <button class="btn-sm btn-followed" @click.stop="$router.push(`/chat/${m.userId}`)">
              聊天
            </button>
          </div>
        </div>
        <div v-else class="empty-hint">关注用户后在此显示</div>
      </div>

      <div class="panel-card board-card glass-card-light">
        <h3 class="panel-title"><span class="title-accent"></span>热门邀约看板</h3>
        <div v-if="boardLoading" class="board-loading">加载中...</div>
        <div v-else-if="inviteBoard.length" class="board-list">
          <div
            v-for="item in inviteBoard"
            :key="item.inviteType"
            class="board-item board-item-clickable glass-pill-light"
            @click="$router.push(`/invite?source=public&type=${item.inviteType}`)"
          >
            <div class="board-bar">
              <div class="board-bar-fill" :style="{ width: `${item.percent}%`, background: getTypeColor(item.inviteType) }" />
            </div>
            <div class="board-item-content">
              <div class="board-item-left">
                <span class="board-dot" :style="{ background: getTypeColor(item.inviteType) }" />
                <span class="board-type">{{ typeLabel(item.inviteType) }}</span>
              </div>
              <div class="board-item-right">
                <span class="board-count text-gradient-warm">{{ item.count }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无进行中的邀约</div>
      </div>
    </aside>

    <nav class="mobile-tab-bar glass-panel" role="navigation" aria-label="主导航">
      <router-link
        v-for="item in mobileNavItems"
        :key="item.path"
        :to="item.path"
        class="mobile-tab-item"
        :class="{ active: isActive(item.path) }"
        :aria-current="isActive(item.path) ? 'page' : undefined"
      >
        <div class="icon-wrap">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
          <span v-if="item.showDot" class="mobile-tab-dot pulse-dot" />
        </div>
        <span class="mobile-tab-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <el-dialog
      v-model="showPostDialog"
      title="发射引力波"
      :width="postDialogWidth"
      class="light-glass-dialog"
      align-center
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-input v-model="postContent" type="textarea" :rows="postTextareaRows" placeholder="分享此刻的想法..." maxlength="500" show-word-limit />

      <div class="post-media-section">
        <div v-if="uploadedImages.length" class="media-preview-grid">
          <div v-for="(img, idx) in uploadedImages" :key="idx" class="media-preview-item">
            <img :src="getMediaUrl(img)" class="preview-img" />
            <button class="preview-remove" @click="removeImage(idx)"><el-icon><Close /></el-icon></button>
          </div>
        </div>
        <div v-if="uploadedVideos.length" class="media-preview-grid">
          <div v-for="(video, idx) in uploadedVideos" :key="idx" class="media-preview-item">
            <video :src="getMediaUrl(video)" class="preview-video" />
            <button class="preview-remove" @click="removeVideo(idx)"><el-icon><Close /></el-icon></button>
          </div>
        </div>

        <div class="media-actions">
          <input ref="mediaInputRef" type="file" accept="image/*,video/*" multiple hidden @change="handleMediaSelect" />
          <button class="media-btn glass-pill" @click="mediaInputRef?.click()">
            <el-icon><Picture /></el-icon>
            <span>添加画面</span>
          </button>
        </div>
        <p class="media-hint">最多 {{ FEED_POST_MAX_IMAGES }} 张</p>

        <div class="invite-picker-row mt-4">
          <span class="invite-picker-label">关联邀约（可选）</span>
          <el-select v-model="selectedInviteId" clearable filterable placeholder="选择已发起或参与的邀约" :loading="loadingInvitesForFeed" class="glass-select">
            <el-option v-for="inv in inviteOptionsForFeed" :key="inv.id" :label="inviteOptionLabel(inv)" :value="inv.id" />
          </el-select>
        </div>

        <div class="invite-picker-row mt-4">
          <span class="invite-picker-label">可见范围</span>
          <el-select v-model="timelinePostVisibility" placeholder="默认与隐私设置一致" class="glass-select">
            <el-option label="所有人" value="ALL" />
            <el-option label="我关注的人可见" value="FOLLOWING" />
            <el-option label="关注我的人可见" value="FOLLOWERS" />
            <el-option label="朋友（互相关注）" value="FRIENDS" />
            <el-option label="仅自己" value="SELF" />
          </el-select>
        </div>
      </div>

      <template #footer>
        <button
          class="glow-btn-warm w-full"
          :disabled="isPublishing || (!postContent.trim() && !uploadedImages.length && !uploadedVideos.length && selectedInviteId == null)"
          @click="handleCreatePost"
        >
          {{ isPublishing ? '发射中...' : '确认发射' }}
        </button>
      </template>
    </el-dialog>

    <SiteAnnouncementLayer ref="announcementLayerRef" />
  </div>
</template>

<script setup lang="ts">
// ==========================================
// 核心逻辑 100% 保持原封不动
// ==========================================
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useBadgeStore } from '@/store/badgeStore'
import { useFollowStore } from '@/store/followStore'
import { useMatchStore } from '@/store/matchStore'
import { getRecommendations, getMatchDetail, type MatchResult } from '@/api/matchApi'
import { followUser, unfollowUser, getFollowingList } from '@/api/followApi'
import { createPost, uploadImage, uploadVideo } from '@/api/feedApi'
import { searchUsers, type UserSearchItem } from '@/api/userApi'
import { ElMessage } from 'element-plus'
import { Search, Picture, Close, Connection, Opportunity, Guide, ChatDotRound, MagicStick, Flag, User } from '@element-plus/icons-vue'
import { getHotInviteTypeCounts, getInvitesForFeed, type Invite, type InviteTypeCount } from '@/api/inviteApi'
import { InviteType, INVITE_TYPE_LABELS } from '@/constants/inviteConst'
import { getMediaUrl, getTypeColor } from '@/utils/shared'
import { usePostPublishDialogLayout } from '@/composables/usePostPublishDialogLayout'
import { getSchoolTheme } from '@/constants/schoolThemes'
import AppAvatar from '@/components/AppAvatar.vue'
import SiteAnnouncementLayer from '@/components/SiteAnnouncementLayer.vue'
import { compressImageFile } from '@/utils/mediaCompress'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const announcementLayerRef = ref<InstanceType<typeof SiteAnnouncementLayer> | null>(null)
const badgeStore = useBadgeStore()
const followStore = useFollowStore()
const matchStore = useMatchStore()
const SHOW_MATCH_NAV = false

const schoolTheme = computed(() => getSchoolTheme(userStore.user?.school ?? undefined))
const schoolColorStyle = computed(() => ({
  '--school-color': schoolTheme.value.primaryColor,
  '--school-color-light': `${schoolTheme.value.primaryColor}15`,
  '--school-color-mid': `${schoolTheme.value.primaryColor}40`,
}))

const navItems = computed(() => {
  const b = badgeStore.badges
  const items = [
    { path: '/discover', label: '共鸣', icon: Connection, showDot: false },
    { path: '/moment', label: '心动', icon: Opportunity, showDot: false },
    { path: '/invite', label: '同行', icon: Guide, showDot: b.newInviteActivityCount > 0 },
    { path: '/chat', label: '消息', icon: ChatDotRound, showDot: b.unreadMessageCount > 0 || b.newFollowerCount > 0 || b.newFeedActivityCount > 0 || b.newInviteActivityCount > 0 },
  ]
  if (SHOW_MATCH_NAV) items.splice(1, 0, { path: '/match', label: '缘分', icon: MagicStick, showDot: false })
  if (userStore.user?.isAdmin) {
    items.push({ path: '/admin/reports', label: '举报管理', icon: Flag, showDot: false })
    items.push({ path: '/admin/profile', label: '画像管理', icon: User, showDot: false })
  }
  return items
})

const isActive = (path: string) => route.path.startsWith(path)

const mobileNavItems = computed(() => {
  const b = badgeStore.badges
  const items = [
    { path: '/discover', label: '共鸣', icon: Connection, showDot: false },
    { path: '/moment', label: '心动', icon: Opportunity, showDot: false },
    { path: '/invite', label: '同行', icon: Guide, showDot: b.newInviteActivityCount > 0 },
    { path: '/chat', label: '消息', icon: ChatDotRound, showDot: b.unreadMessageCount > 0 || b.newFollowerCount > 0 || b.newFeedActivityCount > 0 },
    { path: '/profile', label: '我', icon: User, showDot: false },
  ]
  if (SHOW_MATCH_NAV) items.splice(1, 0, { path: '/match', label: '缘分', icon: MagicStick, showDot: false })
  return items
})

const topMatches = ref<MatchResult[]>([])
const followedMatches = ref<MatchResult[]>([])

const topNotFollowedMatches = computed(() => topMatches.value.filter(m => !followStore.isFollowed(m.userId)).slice(0, 5))
const topFollowedMatches = computed(() => followedMatches.value.slice(0, 5))
const { postDialogWidth, postTextareaRows } = usePostPublishDialogLayout()
const showPostDialog = ref(false)
const postContent = ref('')
const timelinePostVisibility = ref<string>('ALL')

watch(showPostDialog, (open) => {
  if (open) {
    void loadInvitesForFeedPicker()
    timelinePostVisibility.value = userStore.user?.feedVisibility ?? 'ALL'
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
  } catch { inviteOptionsForFeed.value = [] } 
  finally { loadingInvitesForFeed.value = false }
}

const FEED_POST_MAX_IMAGES = 8
const uploadedImages = ref<string[]>([])
const uploadedVideos = ref<string[]>([])
const selectedInviteId = ref<number | undefined>(undefined)
const inviteOptionsForFeed = ref<Invite[]>([])
const loadingInvitesForFeed = ref(false)
const isPublishing = ref(false)
const mediaInputRef = ref<HTMLInputElement>()
const inviteBoard = ref<Array<InviteTypeCount & { percent: number }>>([])
const boardLoading = ref(false)
const searchBoxRef = ref<HTMLElement>()
const searchKeyword = ref('')
const searchResults = ref<UserSearchItem[]>([])
const searchLoading = ref(false)
const showSearchDropdown = ref(false)
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

watch(searchKeyword, (val) => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  if (!val?.trim()) { showSearchDropdown.value = false; searchResults.value = []; return }
  showSearchDropdown.value = true
  if (val.trim().length < 2) return
  searchDebounceTimer = setTimeout(doSearch, 300)
})

async function doSearch() {
  const kw = searchKeyword.value?.trim()
  if (!kw || kw.length < 2) { searchResults.value = []; return }
  searchLoading.value = true
  try {
    if (followStore.followedIds.length === 0) await followStore.loadFollowedIds()
    const searchRes = await searchUsers(kw, 10)
    searchResults.value = searchRes.data.data || []
  } catch { searchResults.value = [] } 
  finally { searchLoading.value = false }
}

function clearSearch() { searchResults.value = []; showSearchDropdown.value = false }

function goToProfile(userId: number) {
  searchKeyword.value = ''
  clearSearch()
  router.push(`/profile/${userId}`)
}

async function handleSearchFollow(userId: number) {
  const isFollowing = followStore.isFollowed(userId)
  try {
    if (isFollowing) {
      await unfollowUser(userId); followStore.removeFollowed(userId); ElMessage.success('已取消关注')
    } else {
      await followUser(userId); followStore.addFollowed(userId); ElMessage.success('关注成功')
    }
  } catch { /* handled by interceptor */ }
}

function onDocumentClick(e: MouseEvent) {
  if (searchBoxRef.value && !searchBoxRef.value.contains(e.target as Node)) showSearchDropdown.value = false
}

let badgePollTimer: ReturnType<typeof setInterval> | null = null
async function loadRecommendations() {
  try {
    const [recRes] = await Promise.all([getRecommendations(0, 10), followStore.loadFollowedIds()])
    topMatches.value = recRes.data.data || []
  } catch { topMatches.value = [] }
  loadFollowedMatches()
}

async function loadFollowedMatches() {
  try {
    const res = await getFollowingList()
    const followingUsers = res.data.data || []
    if (!followingUsers.length) { followedMatches.value = []; return }
    const top = followingUsers.slice(0, 10)
    const details = await Promise.allSettled(top.map(u => getMatchDetail(u.userId)))
    const results: MatchResult[] = []
    details.forEach((d, i) => {
      const fallbackUser = top[i]
      if (!fallbackUser) return
      if (d.status === 'fulfilled' && d.value.data.data) results.push(d.value.data.data)
      else {
        results.push({
          userId: fallbackUser.userId, nickname: fallbackUser.nickname, avatarUrl: fallbackUser.avatarUrl,
          gender: 0, school: null, major: null, grade: null, mbti: null, zodiac: null, bio: null, interests: null,
          matchScore: 0, detail: { oceanScore: 0, interestScore: 0, valuesScore: null, ageGradeScore: 0, zodiacScore: 0, majorScore: 0 },
        })
      }
    })
    followedMatches.value = results.sort((a, b) => b.matchScore - a.matchScore)
  } catch { followedMatches.value = [] }
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible' && userStore.user) badgeStore.fetchBadges()
}

watch(() => matchStore.weightVersion, () => { if (userStore.user) loadRecommendations() })

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('visibilitychange', onVisibilityChange)
  if (userStore.user) badgeStore.fetchBadges()
  badgePollTimer = setInterval(() => { if (userStore.user) badgeStore.fetchBadges() }, 60000)
  loadRecommendations()
  loadInviteBoard()
})

watch(() => userStore.user, (u) => { if (u) badgeStore.fetchBadges() }, { immediate: true })
watch(() => userStore.user?.id, (id) => { if (id) announcementLayerRef.value?.load() }, { immediate: true })

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  if (badgePollTimer) clearInterval(badgePollTimer)
})

async function handleRecommendFollow(userId: number) {
  const isFollowed = followStore.isFollowed(userId)
  try {
    if (isFollowed) {
      await unfollowUser(userId); followStore.removeFollowed(userId); ElMessage.success('已取消关注')
    } else {
      await followUser(userId); followStore.addFollowed(userId); ElMessage.success('关注成功')
    }
  } catch { /* handled by interceptor */ }
}

async function handleCreatePost() {
  if (!postContent.value.trim() && !uploadedImages.value.length && !uploadedVideos.value.length && selectedInviteId.value == null) return
  isPublishing.value = true
  try {
    await createPost({
      content: postContent.value.trim(), images: uploadedImages.value.length ? uploadedImages.value.join(',') : undefined,
      videos: uploadedVideos.value.length ? uploadedVideos.value.join(',') : undefined,
      inviteId: selectedInviteId.value, visibility: timelinePostVisibility.value || 'ALL',
    })
    ElMessage.success('发射成功')
    showPostDialog.value = false
    resetPostForm()
  } catch { ElMessage.error('发射失败') } 
  finally { isPublishing.value = false }
}

function resetPostForm() {
  postContent.value = ''
  uploadedImages.value = []
  uploadedVideos.value = []
  selectedInviteId.value = undefined
  timelinePostVisibility.value = userStore.user?.feedVisibility ?? 'ALL'
}

async function handleMediaSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files) return
  let imageSlots = FEED_POST_MAX_IMAGES - uploadedImages.value.length
  if (imageSlots <= 0) {
    const hasImage = Array.from(files).some((f) => !f.type.startsWith('video/'))
    if (hasImage) ElMessage.warning(`最多 ${FEED_POST_MAX_IMAGES} 张图片`)
  }
  for (const file of Array.from(files)) {
    const isVideo = file.type.startsWith('video/')
    if (isVideo) {
      if (file.size > 120 * 1024 * 1024) { ElMessage.warning('视频不能超过 120MB'); continue }
      try {
        const path = (await uploadVideo(file)).data.data
        if (path) uploadedVideos.value.push(path)
      } catch { ElMessage.error('视频上传失败') }
    } else {
      if (imageSlots <= 0 || file.size > 25 * 1024 * 1024) continue
      try {
        const toSend = await compressImageFile(file)
        const path = (await uploadImage(toSend)).data.data
        if (path) { uploadedImages.value.push(path); imageSlots-- }
      } catch { ElMessage.error('图片上传失败') }
    }
  }
  target.value = ''
}

function removeImage(index: number) { uploadedImages.value.splice(index, 1) }
function removeVideo(index: number) { uploadedVideos.value.splice(index, 1) }
function typeLabel(t: string) { return INVITE_TYPE_LABELS[t as InviteType] || t }

async function loadInviteBoard() {
  boardLoading.value = true
  try {
    const res = await getHotInviteTypeCounts(10)
    const list = res.data.data || []
    const max = Math.max(...list.map(i => i.count), 0)
    inviteBoard.value = list.map(i => ({
      ...i, percent: max <= 0 ? 0 : Math.max(6, Math.round((i.count / max) * 100)),
    }))
  } catch { inviteBoard.value = [] } 
  finally { boardLoading.value = false }
}
</script>

<style lang="scss" scoped>
/* ==========================================
   晨曦极光 (Light Glassmorphism) 全局骨架
   ========================================== */
$bg-aurora: #f8fafc;
$accent-pink: #FF3366;
$accent-orange: #FF7B54;
$accent-blue: #4f8cff;
$text-main: #1e293b;
$text-sub: #64748b;
$serif: 'Noto Serif SC', 'Songti SC', 'Source Han Serif SC', Georgia, 'Times New Roman', serif;
$border-light: rgba(255, 255, 255, 0.6);

.main-layout {
  display: flex; gap: 20px; min-height: 100vh; max-width: 1280px; margin: 0 auto; padding: 0 20px;
  position: relative;
}

// 弥散的极光背景底色
.global-aurora-bg {
  position: fixed; inset: 0; pointer-events: none; z-index: -1; background: $bg-aurora;
  &::after {
    content: ''; position: absolute; inset: 0;
    background: 
      radial-gradient(circle at 10% 10%, rgba(79, 140, 255, 0.1), transparent 40%),
      radial-gradient(circle at 90% 40%, rgba(255, 51, 102, 0.08), transparent 40%),
      radial-gradient(circle at 50% 90%, rgba(255, 123, 84, 0.06), transparent 50%);
  }
}

// ================= 通用玻璃态组件 =================
.glass-panel {
  background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px);
  border: 1px solid $border-light; box-shadow: 0 10px 40px rgba(31, 38, 135, 0.05);
}
.glass-card-light {
  background: rgba(255, 255, 255, 0.5); backdrop-filter: blur(12px); 
  border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}
.glass-pill {
  background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.9); border-radius: 999px;
}
.glass-pill-light {
  background: rgba(255, 255, 255, 0.4); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 16px;
}

.text-gradient-warm {
  background: linear-gradient(135deg, $accent-pink, $accent-orange);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; font-weight: 800;
}
.glow-btn-warm {
  height: 48px; border-radius: 999px; border: none; display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, $accent-pink, $accent-orange); color: white;
  font-size: 15px; font-weight: 700; cursor: pointer; letter-spacing: 1px;
  box-shadow: 0 8px 25px rgba(255, 51, 102, 0.3); transition: all 0.3s ease;
  &:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 12px 30px rgba(255, 51, 102, 0.4); }
  &:disabled { background: #cbd5e1; box-shadow: none; cursor: not-allowed; opacity: 0.8; color: #fff; }
}
.glass-btn {
  padding: 6px 16px; border-radius: 999px; background: rgba(255, 255, 255, 0.6); border: 1px solid #fff;
  color: $text-sub; font-size: 13px; font-weight: 700; cursor: pointer; transition: all 0.3s;
  &:hover { background: #fff; color: $text-main; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
}

// 动画
.pulse-dot {
  width: 6px; height: 6px; border-radius: 50%; background: $accent-pink; animation: pulse 2s infinite;
  box-shadow: 0 0 8px $accent-pink;
}
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(255,51,102, 0.4); } 70% { box-shadow: 0 0 0 6px rgba(255,51,102, 0); } 100% { box-shadow: 0 0 0 0 rgba(255,51,102, 0); } }

// ================= 左侧导航 (Left Sidebar) =================
.sidebar {
  width: 240px; flex-shrink: 0; position: sticky; top: 0; height: 100vh; padding: 20px 0; z-index: 10;
}
.glass-sidebar {
  // 左侧和右侧面板本身不需要明显的盒子，只需要保持内部内容的通透
  background: transparent;
}
.sidebar-inner { display: flex; flex-direction: column; height: 100%; gap: 8px;}

.logo-link {
  display: flex; align-items: center; padding: 8px 16px; margin-bottom: 24px; text-decoration: none;
  .logo-text { font-family: 'Pacifico', cursive; font-size: 28px; font-weight: 400; }
}

.nav-menu { display: flex; flex-direction: column; gap: 8px; flex: 1; }
.nav-item {
  display: flex; align-items: center; gap: 14px; padding: 12px 16px; border-radius: 18px;
  color: $text-sub; font-size: 16px; font-weight: 600; transition: all 0.3s; text-decoration: none;
  position: relative;
  
  .icon-wrap {
    width: 36px; height: 36px; border-radius: 12px; background: rgba(255,255,255,0.5); border: 1px solid rgba(255,255,255,0.8);
    display: flex; align-items: center; justify-content: center; transition: all 0.3s;
  }
  
  &:hover {
    background: rgba(255,255,255,0.4); color: $text-main;
    .icon-wrap { transform: translateY(-2px); background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
  }
  &.active {
    color: $text-main; font-weight: 800; background: rgba(255,255,255,0.6); box-shadow: 0 4px 15px rgba(0,0,0,0.02);
    .icon-wrap { background: linear-gradient(135deg, $accent-pink, $accent-orange); color: #fff; border-color: transparent; box-shadow: 0 6px 15px rgba(255,51,102,0.3); }
  }
  .nav-dot { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); }
}

.sidebar-user {
  display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 20px; cursor: pointer; transition: all 0.3s;
  &:hover { background: rgba(255,255,255,0.7); transform: translateY(-2px); box-shadow: 0 8px 20px rgba(0,0,0,0.04);}
  .avatar-glow-wrap { border-radius: 50%; padding: 2px; background: rgba(79,140,255,0.2); box-shadow: 0 0 10px rgba(79,140,255,0.2); }
  .avatar { border: 2px solid #fff; }
  .user-info { flex: 1; min-width: 0; }
  .user-name { font-size: 14px; font-weight: 700; color: $text-main; }
  .user-email { font-size: 12px; color: $text-sub; }
}

// ================= 中心路由区 (Content Area) =================
.content-area {
  flex: 1; min-width: 0; position: relative;
  // 背景透明，让全局极光和内部页面的毛玻璃结合
  background: transparent;
}

// ================= 右侧面板 (Right Panel) =================
.right-panel {
  width: 320px; flex-shrink: 0; position: sticky; top: 0; height: 100vh; overflow-y: auto; padding: 20px 0;
  display: flex; flex-direction: column; gap: 16px;
  &::-webkit-scrollbar { display: none; }
}

/* 搜索框 */
.search-box { position: relative; }
.glass-search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(16px);
  border-radius: 999px; border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.02); height: 44px;
  .el-input__inner { color: $text-main; }
  &.is-focus { border-color: rgba($accent-pink, 0.4); box-shadow: 0 0 0 2px rgba($accent-pink, 0.1); }
}

.search-dropdown {
  position: absolute; top: calc(100% + 8px); left: 0; right: 0; z-index: 100;
  max-height: 320px; overflow-y: auto; padding: 8px;
}
.search-loading, .search-hint, .search-empty { padding: 16px; text-align: center; color: $text-sub; font-size: 13px; font-weight: 600; }
.search-item {
  display: flex; align-items: center; gap: 12px; padding: 10px; cursor: pointer; border-radius: 16px; transition: background 0.3s;
  &:hover { background: rgba(255,255,255,0.8); }
  .avatar { border-radius: 50%; border: 1px solid #fff; }
  .search-item-name { flex: 1; font-size: 14px; font-weight: 700; color: $text-main; }
}

/* 面板卡片 (推荐 / 邀约) */
.panel-card { padding: 20px; }
.panel-title {
  font-size: 16px; font-weight: 800; color: $text-main; margin: 0 0 16px; display: flex; align-items: center; gap: 8px;
  .title-accent { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, $accent-pink, $accent-orange); }
}

.recommend-list { display: flex; flex-direction: column; gap: 12px; }
.recommend-item {
  display: flex; align-items: center; gap: 12px; cursor: pointer; padding: 8px; border-radius: 16px; transition: background 0.3s;
  &:hover { background: rgba(255,255,255,0.6); }
  .avatar { border: 2px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.05);}
  .recommend-info { flex: 1; min-width: 0; }
  .recommend-name { font-size: 14px; font-weight: 700; color: $text-main; }
  .recommend-meta { font-size: 12px; margin-top: 2px; }
}

.btn-followed {
  padding: 6px 16px; font-size: 12px; font-weight: 700; border-radius: 999px; border: none;
  background: rgba(0,0,0,0.04); color: $text-sub; cursor: pointer; transition: all 0.3s;
  &:hover { background: rgba(245,108,108,0.1); color: #f56c6c; }
}

/* 热门邀约看板 */
.board-card { cursor: pointer; transition: transform 0.3s, box-shadow 0.3s; &:hover { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(0,0,0,0.06); } }
.board-list { display: flex; flex-direction: column; gap: 10px; }
.board-item {
  position: relative; padding: 12px 14px; overflow: hidden; cursor: pointer; transition: background 0.3s; border: 1px solid rgba(255,255,255,0.8);
  &:hover { background: rgba(255,255,255,0.7); }
}
.board-bar { position: absolute; bottom: 0; left: 0; width: 100%; height: 4px; background: transparent; }
.board-bar-fill { height: 100%; opacity: 0.8; border-top-right-radius: 4px; border-bottom-right-radius: 4px;}
.board-item-content { display: flex; justify-content: space-between; align-items: center; }
.board-item-left { display: flex; align-items: center; gap: 8px; }
.board-dot { width: 8px; height: 8px; border-radius: 50%; }
.board-type { font-size: 14px; font-weight: 700; color: $text-main; }
.board-count { font-size: 18px; }

.empty-hint { text-align: center; padding: 20px 0; color: $text-sub; font-size: 13px; font-weight: 600; }

// ================= 移动端导航 (Mobile Bottom Tab) =================
.mobile-tab-bar {
  display: none; position: fixed; bottom: 16px; left: 16px; right: 16px; height: 68px; z-index: 100;
  padding: 0 8px; justify-content: space-around; align-items: center; border-radius: 24px;
}
.mobile-tab-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; text-decoration: none; color: $text-sub; transition: all 0.3s;
  .icon-wrap { position: relative; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 10px; transition: all 0.3s; }
  .mobile-tab-label { font-size: 11px; font-weight: 600; }
  .mobile-tab-dot { position: absolute; top: -2px; right: -2px; }
  
  &.active {
    color: $accent-pink;
    .icon-wrap { background: linear-gradient(135deg, rgba(255,51,102,0.1), rgba(255,123,84,0.1)); color: $accent-pink; box-shadow: inset 0 0 0 1px rgba(255,51,102,0.2); }
  }
}

// ================= 发布弹窗 =================
.w-full { width: 100%; }
.mt-4 { margin-top: 16px; }

.post-media-section { margin-top: 16px; }
.media-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 10px; margin-bottom: 12px; }
.media-preview-item {
  position: relative; aspect-ratio: 1; border-radius: 12px; overflow: hidden; background: rgba(0,0,0,0.05);
  .preview-img, .preview-video { width: 100%; height: 100%; object-fit: cover; }
  .preview-remove {
    position: absolute; top: 4px; right: 4px; width: 24px; height: 24px; border-radius: 50%; background: rgba(0,0,0,0.6); color: #fff;
    border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: background 0.3s;
    &:hover { background: #f56c6c; }
  }
}

.media-actions { display: flex; gap: 8px; margin-bottom: 4px; }
.media-btn {
  display: flex; align-items: center; gap: 6px; padding: 8px 16px; font-size: 13px; font-weight: 700; color: $text-main; cursor: pointer; transition: all 0.3s; border: none;
  &:hover { background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.05); color: $accent-pink; }
}
.media-hint { font-size: 12px; color: $text-sub; margin: 0 0 16px; }

.invite-picker-row { display: flex; flex-direction: column; gap: 8px; }
.invite-picker-label { font-size: 13px; font-weight: 700; color: $text-main; }
.glass-select :deep(.el-input__wrapper) { background: rgba(255,255,255,0.6); backdrop-filter: blur(12px); border-radius: 12px; box-shadow: none; border: 1px solid rgba(255,255,255,0.8); }

:deep(.light-glass-dialog) {
  background: rgba(255, 255, 255, 0.85) !important; backdrop-filter: blur(24px); border: 1px solid #fff; border-radius: 24px; box-shadow: 0 20px 60px rgba(0,0,0,0.1);
  .el-dialog__title { color: $text-main; font-weight: 800; font-family: $serif; font-size: 20px;}
  .el-textarea__inner { background: rgba(255,255,255,0.6); border: 1px solid rgba(255,255,255,0.9); color: $text-main; border-radius: 16px; padding: 16px; box-shadow: inset 0 2px 6px rgba(0,0,0,0.02);}
}

// ================= 响应式 =================
@media (max-width: 1024px) {
  .right-panel { display: none; }
}

@media (max-width: 640px) {
  .main-layout { padding: 0; display: block; }
  .sidebar { display: none; }
  .content-area { padding-bottom: 90px; } // 留出底部导航空间
  .mobile-tab-bar { display: flex; }
}
</style>