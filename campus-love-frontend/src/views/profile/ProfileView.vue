<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-cover">
        <!-- Settings button in top right -->
        <div v-if="isMe" class="profile-settings">
          <el-dropdown trigger="click">
            <button class="settings-btn">
              <el-icon><Setting /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/setup-profile')">
                  <el-icon><Edit /></el-icon> 编辑资料
                </el-dropdown-item>
                <el-dropdown-item @click="showPrivacySettings = true">
                  <el-icon><Lock /></el-icon> 隐私设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout" class="text-danger">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="profile-main">
        <div class="avatar-wrapper">
          <img :src="profile?.avatarUrl || defaultAvatar" class="profile-avatar avatar" width="100" height="100" />
          <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="handleAvatarChange" />
          <button v-if="isMe" class="avatar-upload-btn" @click="avatarInput?.click()">
            <el-icon><Camera /></el-icon>
          </button>
        </div>
        
        <!-- User Info moved to the right of avatar -->
        <div v-if="profile" class="profile-info-header">
          <div class="profile-name-row">
            <h2
              v-if="!editingNickname"
              class="profile-name profile-name-clickable"
              @click="isMe && startEditNickname()"
            >
              {{ profile.nickname }}
            </h2>
            <div v-else class="profile-name-edit">
              <el-input
                ref="nicknameInputRef"
                v-model="editingNicknameValue"
                size="small"
                maxlength="20"
                show-word-limit
                @keyup.enter="saveNickname"
              />
              <button class="btn-save-nickname" @click="saveNickname">保存</button>
              <button class="btn-cancel-nickname" @click="cancelEditNickname">取消</button>
            </div>
            <div class="level-display">
              <span class="level-badge large">Lv{{ profile.userLevel }}</span>
              <span v-if="profile.isAdmin" class="admin-badge">管理员</span>
            </div>
          </div>
          <p v-if="isMe && profile.email" class="profile-email">{{ profile.email }}</p>
          <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
        </div>

        <div v-if="!isMe" class="profile-actions">
          <button :class="['btn-primary', { 'btn-outline': followStatus !== 'NONE' }]" @click="handleFollowToggle">
            {{ followLabel }}
          </button>
          <button class="btn-outline" @click="$router.push(`/chat/${profileId}`)">
            <el-icon><ChatDotRound /></el-icon> 聊天
          </button>
          <button v-if="followStatus === 'MUTUAL'" class="btn-primary" @click="handleInviteUser">
            <el-icon><Calendar /></el-icon> 约TA一下
          </button>
          <button v-if="followStatus === 'MUTUAL'" class="btn-yuanfen" @click="openYuanFen">
            缘分解析 ✨
          </button>
        </div>
      </div>
    </div>

    <div v-if="profile" class="profile-info">
      <div class="profile-stats">
        <div class="stat-item" @click="handleOpenFollowing">
          <span class="stat-num">{{ followingCount }}</span>
          <span class="stat-label">我关注的</span>
        </div>
        <div class="stat-item" @click="handleOpenFollowers">
          <span class="stat-num">{{ followerCount }}</span>
          <span v-if="isMe && newFollowerCount > 0" class="stat-new">+{{ newFollowerCount }}</span>
          <span class="stat-label">关注我的</span>
        </div>
        <div class="stat-item" @click="handleOpenMutual">
          <span class="stat-num">{{ mutualCount }}</span>
          <span class="stat-label">朋友</span>
        </div>
      </div>

      <!-- 等级进度条 -->
      <div v-if="isMe" class="level-progress-card">
        <div class="level-progress-header">
          <span class="level-progress-label">活跃度</span>
          <span class="level-progress-score">{{ profile.activityScore }}分</span>
        </div>
        <div class="level-progress-bar">
          <div class="level-progress-fill" :style="{ width: getLevelProgress(profile.activityScore) + '%' }"></div>
        </div>
        <div class="level-progress-text">
          <span>当前等级 Lv{{ profile.userLevel }}</span>
          <span>{{ getNextLevelScore(profile.activityScore) }}分升级</span>
        </div>
      </div>

      <!-- 邀约成就 & 平均评分 -->
      <div v-if="inviteStats || profile.inviteCount || profile.participateCount" class="invite-stats-card">
        <div class="invite-stats-header">
          <div>
            <span class="invite-stats-title">{{ isMe ? '邀约成就' : `${profile?.nickname || 'TA'}的邀约成就` }}</span>
            <span class="invite-stats-subtitle">展示邀约次数与平均评分</span>
          </div>
          <div class="invite-credit">
            <span class="label">信用分</span>
            <span class="value">{{ profile.creditScore ?? 100 }}</span>
          </div>
        </div>
        <div class="invite-stats-body">
          <div class="invite-score-block">
            <div class="score-main">
              <span class="score-value">
                {{ inviteStats?.receivedSocialRating != null ? inviteStats.receivedSocialRating.toFixed(1) : '-' }}
              </span>
              <span class="score-unit">/ 5.0</span>
            </div>
            <div class="score-label">社交体验平均评分</div>
            <div class="score-desc">
              来自所有邀约中的他人评价
            </div>
          </div>
          <div class="invite-count-block">
            <div class="count-item">
              <span class="count-label">发起邀约</span>
              <span class="count-value">{{ inviteStats?.inviteCount ?? profile.inviteCount ?? 0 }}</span>
            </div>
            <div class="count-item">
              <span class="count-label">参与邀约</span>
              <span class="count-value">{{ inviteStats?.participateCount ?? profile.participateCount ?? 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-meta">
        <span v-if="profile.gender !== null && profile.gender !== undefined" class="meta-item gender" :class="profile.gender === 1 ? 'male' : 'female'">
          {{ profile.gender === 1 ? '♂ 男' : '♀ 女' }}
        </span>
        <span v-if="displayAge != null" class="meta-item">👤 {{ displayAge }}岁</span>
        <span v-if="profile.school" class="meta-item">🎓 {{ profile.school }}</span>
        <span v-if="profile.major" class="meta-item">📚 {{ profile.major }}</span>
        <span v-if="profile.grade" class="meta-item">📅 {{ profile.grade }}</span>
        <span v-if="profile.zodiac" class="meta-item">⭐ {{ profile.zodiac }}</span>
        <span v-if="profile.mbti" class="meta-item mbti">🧠 {{ profile.mbti }}</span>
        <span v-if="isMe && profile.bazi" class="meta-item">🔮 {{ profile.bazi }}</span>
      </div>

      <div v-if="profile.interests" class="profile-interests">
        <span v-for="tag in profile.interests.split(',')" :key="tag" class="interest-tag">{{ tag }}</span>
      </div>

      <!-- Match detail if not me -->
      <div v-if="!isMe && matchResult" class="match-section card">
        <h3 class="section-title">匹配分析</h3>
        <div class="match-total">
          <span class="match-score">{{ matchResult.matchScore }}%</span>
          <span class="match-label">综合匹配度</span>
        </div>
        <div class="match-bars">
          <div v-for="(val, key) in matchResult.detail" :key="key" class="bar-item">
            <span class="bar-label">{{ dimensionLabels[key as string] || key }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: val + '%' }"></div></div>
            <span class="bar-val">{{ val }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- User's Posts -->
    <div class="profile-posts">
      <div class="posts-tabs">
        <button class="tab-btn active">动态</button>
      </div>

      <div v-for="post in posts" :key="post.id" class="feed-item">
        <div class="feed-item-content">
          <p class="feed-content">{{ post.content }}</p>
          <div class="feed-meta">
            <span>{{ formatTime(post.createdAt) }}</span>
            <span>❤️ {{ post.likeCount }} · 💬 {{ post.commentCount }}</span>
          </div>
        </div>
        <button
          v-if="canDeletePost(post)"
          class="feed-delete-btn"
          title="删除"
          @click="handleDeletePost(post.id)"
        >
          🗑️
        </button>
      </div>
      <div v-if="!posts.length" class="empty-hint">
        暂无动态
      </div>
    </div>
  </div>

  <!-- 关注列表弹窗 -->
  <el-dialog :title="isMe ? '我关注的' : `${profile?.nickname || 'TA'}关注的`" width="500px" :model-value="showFollowing" @update:model-value="showFollowing = false">
    <div v-if="followingList.length" class="user-list">
      <div v-for="user in followingList" :key="user.userId" class="user-list-item" @click="goToUserProfile(user.userId)">
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.nickname }}</div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无关注' : 'TA暂无关注' }}</div>
  </el-dialog>

  <!-- 粉丝列表弹窗 -->
  <el-dialog :title="isMe ? '关注我的' : `关注${profile?.nickname || 'TA'}的`" width="500px" :model-value="showFollowers" @update:model-value="showFollowers = false">
    <div v-if="followerList.length" class="user-list">
      <div v-for="user in followerList" :key="user.userId" class="user-list-item" @click="goToUserProfile(user.userId)">
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.nickname }}</div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无粉丝' : 'TA暂无粉丝' }}</div>
  </el-dialog>

  <!-- 朋友列表弹窗 -->
  <el-dialog :title="isMe ? '我的朋友' : `${profile?.nickname || 'TA'}的朋友`" width="500px" :model-value="showMutual" @update:model-value="showMutual = false">
    <div v-if="mutualList.length" class="user-list">
      <div v-for="user in mutualList" :key="user.userId" class="user-list-item" @click="goToUserProfile(user.userId)">
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.nickname }}</div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无朋友' : 'TA暂无朋友' }}</div>
  </el-dialog>

  <!-- 缘分解析弹层 -->
  <YuanFenAnalysisSheet
    v-if="showYuanFen"
    :model-value="showYuanFen"
    :target-user-id="profileId"
    :current-nickname="userStore.user?.nickname || '我'"
    :target-nickname="profile?.nickname || 'TA'"
    @close="showYuanFen = false"
    @analyzed="onYuanFenAnalyzed"
  />

  <!-- 隐私设置弹窗 -->
  <el-dialog title="隐私设置" width="400px" :model-value="showPrivacySettings" @update:model-value="showPrivacySettings = false">
    <div class="privacy-settings-form">
      <div class="setting-item">
        <div class="setting-label">谁可以看我的动态</div>
        <el-radio-group v-model="feedVisibility" @change="saveFeedVisibility">
          <el-radio label="ALL">所有人可见</el-radio>
          <el-radio label="FOLLOWERS">粉丝可见</el-radio>
          <el-radio label="SELF">仅自己可见</el-radio>
        </el-radio-group>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useBadgeStore } from '@/store/badgeStore'
import { getUserProfile, updateNickname, updateFeedVisibility, type UserProfile } from '@/api/userApi'
import { getMatchDetail, type MatchResult } from '@/api/matchApi'
import { getInviteStats, getUserInviteStats, type InviteStats } from '@/api/inviteApi'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList, getUserFollowing, getUserFollowers, type FollowUser } from '@/api/followApi'
import { getUserTimelinePosts, getUserDiscoveryPosts, deletePost, type FeedPost } from '@/api/feedApi'
import { ElMessage } from 'element-plus'
import { Camera, ChatDotRound, Calendar, Setting, Edit, Lock, SwitchButton } from '@element-plus/icons-vue'
import { MATCH_DIMENSION_LABELS } from '@/constants/matchConst'
import { FOLLOW_STATUS_LABELS, FollowStatus } from '@/constants/followConst'
import { uploadAvatar } from '@/api/userApi'
import { getYuanFenCooldown } from '@/api/aiApi'
import YuanFenAnalysisSheet from './components/YuanFenAnalysisSheet.vue'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const dimensionLabels = MATCH_DIMENSION_LABELS

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const badgeStore = useBadgeStore()

const profileId = computed(() => route.params.userId ? Number(route.params.userId) : userStore.user?.id)
const isMe = computed(() => profileId.value === userStore.user?.id)

const profile = ref<UserProfile | null>(null)
const followStatus = ref<string>(FollowStatus.NONE)
const matchResult = ref<MatchResult | null>(null)
const inviteStats = ref<InviteStats | null>(null)
const posts = ref<FeedPost[]>([])
const followingCount = ref(0)
const followerCount = ref(0)
const mutualCount = ref(0)
const showFollowing = ref(false)
const showFollowers = ref(false)
const showMutual = ref(false)

// 缘分解析
const showYuanFen = ref(false)
const yuanfenCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const yuanfenCooldownText = computed(() => {
  const m = Math.ceil(yuanfenCooldown.value / 60)
  return `${m}分钟后可再次解析`
})

function openYuanFen() {
  // 冷却期间仍允许打开弹窗查看上次解析结果；是否重新调用 AI 由后端+冷却策略控制
  showYuanFen.value = true
}

function onYuanFenAnalyzed(seconds: number) {
  startCooldownTimer(seconds)
}

function startCooldownTimer(seconds: number) {
  yuanfenCooldown.value = seconds
  if (cooldownTimer) clearInterval(cooldownTimer)
  if (seconds <= 0) return
  cooldownTimer = setInterval(() => {
    yuanfenCooldown.value--
    if (yuanfenCooldown.value <= 0 && cooldownTimer) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}
const followingList = ref<FollowUser[]>([])
const followerList = ref<FollowUser[]>([])
const mutualList = ref<FollowUser[]>([])
const avatarInput = ref<HTMLInputElement | null>(null)

// 隐私设置
const showPrivacySettings = ref(false)
const feedVisibility = ref<string>('ALL')

async function saveFeedVisibility(val: string | number | boolean) {
  try {
    const res = await updateFeedVisibility(String(val))
    if (userStore.user && res.data.data) {
      userStore.user.feedVisibility = res.data.data.feedVisibility
    }
    ElMessage.success('隐私设置已保存')
  } catch {
    ElMessage.error('设置失败')
  }
}

watch(() => userStore.user?.feedVisibility, (v) => {
  if (v) feedVisibility.value = v
}, { immediate: true })

// 昵称编辑
const editingNickname = ref(false)
const editingNicknameValue = ref('')
const nicknameInputRef = ref<InstanceType<typeof import('element-plus').Input> | null>(null)

const followLabel = computed(() => FOLLOW_STATUS_LABELS[followStatus.value as FollowStatus] || '关注')
const newFollowerCount = computed(() => badgeStore.badges.newFollowerCount)

// 年龄展示：他人用 profile.age，本人用 birthDate 计算
const displayAge = computed(() => {
  const p = profile.value
  if (!p) return null
  if (p.age != null) return p.age
  if (isMe.value && p.birthDate) {
    const birth = new Date(p.birthDate)
    const today = new Date()
    let age = today.getFullYear() - birth.getFullYear()
    const m = today.getMonth() - birth.getMonth()
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--
    return age >= 0 ? age : null
  }
  return null
})

// 判断是否可以删除帖子（管理员或帖子作者）
function canDeletePost(post: FeedPost): boolean {
  const isAdmin = userStore.user?.isAdmin || false
  const isOwner = post.userId === userStore.user?.id
  return isAdmin || isOwner
}

// 打开关注列表
function handleOpenFollowing() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的关注列表')
    return
  }
  showFollowing.value = true
}

// 打开粉丝列表（本人查看时标记已读，消除新粉丝红点）
function handleOpenFollowers() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的粉丝列表')
    return
  }
  if (isMe.value) badgeStore.markFollowersViewed()
  showFollowers.value = true
}

// 打开朋友列表
function handleOpenMutual() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的朋友列表')
    return
  }
  showMutual.value = true
}

async function loadProfile() {
  if (!profileId.value) return
  if (isMe.value) void badgeStore.fetchBadges()
  try {
    const res = await getUserProfile(profileId.value)
    profile.value = res.data.data

    // 加载邀约统计（成就与平均评分）
    try {
      const statsRes = isMe.value ? await getInviteStats() : await getUserInviteStats(profileId.value)
      inviteStats.value = statsRes.data.data || null
    } catch {
      inviteStats.value = null
    }

    // 加载帖子
    await loadPostsByType()

    // 他人页面：获取关注状态和匹配度（先加载这些重要数据）
    if (!isMe.value) {
      const statusRes = await getFollowStatus(profileId.value)
      followStatus.value = statusRes.data.data || FollowStatus.NONE
      const matchRes = await getMatchDetail(profileId.value)
      matchResult.value = matchRes.data.data

      // 互相关注时加载缘分解析冷却状态
      if (followStatus.value === FollowStatus.MUTUAL) {
        try {
          const cdRes = await getYuanFenCooldown(profileId.value)
          const remaining = cdRes.data.data?.remainingSeconds || 0
          if (remaining > 0) startCooldownTimer(remaining)
        } catch { /* ignore */ }
      }
    }

    // 最后加载关注/粉丝数量（独立处理，不影响其他数据）
    loadFollowCounts()
    if (isMe.value) await badgeStore.fetchBadges()
  } catch (err) {
    console.error('loadProfile error:', err)
  }
}

// 独立函数加载关注/粉丝/朋友数量
async function loadFollowCounts() {
  try {
    const [fing, fers] = await Promise.all([
      isMe.value ? getFollowingList() : getUserFollowing(profileId.value!),
      isMe.value ? getFollowerList() : getUserFollowers(profileId.value!)
    ])
    const fingList = fing.data.data || []
    const fersList = fers.data.data || []
    followingCount.value = fingList.length
    followerCount.value = fersList.length
    // 朋友即互相关注的人
    const fingIds = new Set(fingList.map(u => u.userId))
    mutualCount.value = fersList.filter(u => fingIds.has(u.userId)).length
  } catch (err) {
    console.error('load follow/follower count error:', err)
    followingCount.value = 0
    followerCount.value = 0
    mutualCount.value = 0
  }
}

async function loadPostsByType() {
  if (!profileId.value) return
  try {
    // 合并获取所有动态
    const [timelineRes, discoveryRes] = await Promise.all([
      getUserTimelinePosts(profileId.value, 0, 20),
      getUserDiscoveryPosts(profileId.value, 0, 20)
    ])
    const allPosts = [...(timelineRes.data.data || []), ...(discoveryRes.data.data || [])]
    // 按时间倒序
    posts.value = allPosts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } catch { /* handled */ }
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

// 跳转到用户主页并关闭弹窗
function goToUserProfile(userId: number) {
  showFollowing.value = false
  showFollowers.value = false
  showMutual.value = false
  router.push(`/profile/${userId}`)
}

// 监听弹窗打开，加载用户列表
watch(showFollowing, async (val) => {
  if (val) {
    try {
      const res = isMe.value ? await getFollowingList() : await getUserFollowing(profileId.value!)
      followingList.value = res.data.data || []
    } catch (err) {
      console.error('load following error:', err)
      ElMessage.error('加载关注列表失败')
    }
  }
})

watch(showFollowers, async (val) => {
  if (val) {
    try {
      const res = isMe.value ? await getFollowerList() : await getUserFollowers(profileId.value!)
      followerList.value = res.data.data || []
    } catch (err) {
      console.error('load followers error:', err)
      ElMessage.error('加载粉丝列表失败')
    }
  }
})

watch(showMutual, async (val) => {
  if (val) {
    try {
      const [fing, fers] = await Promise.all([
        isMe.value ? getFollowingList() : getUserFollowing(profileId.value!),
        isMe.value ? getFollowerList() : getUserFollowers(profileId.value!)
      ])
      const fingList = fing.data.data || []
      const fersList = fers.data.data || []
      const fingIds = new Set(fingList.map(u => u.userId))
      mutualList.value = fersList.filter(u => fingIds.has(u.userId))
    } catch (err) {
      console.error('load mutual error:', err)
      ElMessage.error('加载朋友列表失败')
    }
  }
})

onMounted(loadProfile)
watch(() => route.params.userId, loadProfile)

async function handleFollowToggle() {
  if (!profileId.value) return
  try {
    if (followStatus.value === FollowStatus.NONE) {
      await followUser(profileId.value)
      followStatus.value = FollowStatus.ONE_WAY
      ElMessage.success('关注成功')
    } else {
      await unfollowUser(profileId.value)
      followStatus.value = FollowStatus.NONE
      ElMessage.success('已取消关注')
    }
  } catch { /* handled */ }
}

function handleInviteUser() {
  if (!profileId.value) return
  router.push({
    path: '/invite/create',
    query: { target: profileId.value }
  })
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

// 等级配置
const levelThresholds = [0, 50, 150, 300, 500, 800, 1200, 1700, 2300, 3000]

function getLevelProgress(score: number): number {
  const currentLevel = getLevelByScore(score)
  if (currentLevel >= 10) return 100
  const currentThreshold = levelThresholds[currentLevel - 1]!
  const nextThreshold = levelThresholds[currentLevel]!
  return Math.round(((score - currentThreshold) / (nextThreshold - currentThreshold)) * 100)
}

function getNextLevelScore(score: number): number {
  const currentLevel = getLevelByScore(score)
  if (currentLevel >= 10) return 0
  return levelThresholds[currentLevel]! - score
}

function getLevelByScore(score: number): number {
  for (let i = levelThresholds.length - 1; i >= 0; i--) {
    if (score >= levelThresholds[i]!) return i + 1
  }
  return 1
}

function startEditNickname() {
  if (!profile.value) return
  editingNicknameValue.value = profile.value.nickname
  editingNickname.value = true
  setTimeout(() => nicknameInputRef.value?.focus(), 50)
}

function cancelEditNickname() {
  editingNickname.value = false
  editingNicknameValue.value = ''
}

async function saveNickname() {
  const val = editingNicknameValue.value.trim()
  if (!val) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (val === profile.value?.nickname) {
    cancelEditNickname()
    return
  }
  try {
    const res = await updateNickname(val)
    if (profile.value) profile.value.nickname = res.data.data?.nickname ?? val
    await userStore.fetchProfile()
    ElMessage.success('昵称已更新')
    cancelEditNickname()
  } catch {
    ElMessage.error('昵称更新失败')
  }
}

async function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型和大小
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  try {
    ElMessage.info('上传中...')
    const res = await uploadAvatar(file)
    if (profile.value) {
      profile.value.avatarUrl = res.data.data
    }
    await userStore.fetchProfile()
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    if (target) target.value = ''
  }
}
</script>

<style lang="scss" scoped>
@use 'sass:color';
.profile-page { padding: 0; }

.profile-header { position: relative; }

.profile-cover {
  height: 160px;
  background: $primary-gradient;
  position: relative;
}

.profile-settings {
  position: absolute;
  top: 16px;
  right: 24px;
}

.settings-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-fast;
  backdrop-filter: blur(4px);

  &:hover {
    background: rgba(0, 0, 0, 0.4);
    transform: scale(1.05);
  }

  .el-icon {
    font-size: 20px;
  }
}

.profile-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 0 24px;
  margin-top: -50px;
}

.profile-info-header {
  flex: 1;
  margin-left: 20px;
  padding-top: 60px; /* Push content down into the white area */
  padding-bottom: 12px;
  min-width: 0;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid $bg-primary;
  box-shadow: $shadow-md;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $primary;
  color: white;
  border: 2px solid $bg-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    background: color.adjust($primary, $lightness: -10%);
    transform: scale(1.1);
  }

  .el-icon { font-size: 16px; }
}

.profile-actions {
  display: flex;
  gap: 10px;
  padding-top: 60px; /* Align with profile info */
  padding-bottom: 12px;
  flex-shrink: 0;
}

.profile-info { 
  padding: 0 24px 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.profile-name { font-size: 24px; font-weight: 800; color: $text-primary; }
.profile-name-clickable {
  cursor: pointer;
  &:hover { color: $primary; }
}
.profile-name-edit {
  display: flex;
  align-items: center;
  gap: 8px;
  .el-input { width: 180px; }
}
.btn-save-nickname, .btn-cancel-nickname {
  padding: 6px 14px;
  border-radius: $radius-md;
  font-size: 13px;
  cursor: pointer;
  border: none;
}
.btn-save-nickname {
  background: $primary;
  color: white;
  &:hover { opacity: 0.9; }
}
.btn-cancel-nickname {
  background: $bg-tertiary;
  color: $text-secondary;
  &:hover { background: $border-light; }
}

.level-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-badge {
  padding: 4px 12px;
  background: $primary-gradient;
  color: white;
  border-radius: $radius-full;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;

  &.large {
    padding: 6px 16px;
    font-size: 16px;
  }
}

.admin-badge {
  padding: 4px 12px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  border-radius: $radius-full;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.level-progress-card {
  margin-top: 12px;
  padding: 20px;
  background: rgba($primary, 0.04);
  border-radius: $radius-xl;
  border: none;
}

.invite-stats-card {
  margin-top: 12px;
  padding: 20px;
  background: linear-gradient(135deg, rgba($primary, 0.06), rgba($primary, 0.02));
  border-radius: $radius-xl;
  border: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.invite-stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.invite-stats-title {
  font-size: 14px;
  font-weight: 700;
  color: $text-primary;
}

.invite-stats-subtitle {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: $text-secondary;
}

.invite-credit {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.invite-credit .label {
  font-size: 12px;
  color: $text-secondary;
}

.invite-credit .value {
  font-size: 20px;
  font-weight: 800;
  color: $primary;
}

.invite-stats-body {
  display: flex;
  align-items: stretch;
  gap: 16px;
  margin-top: 4px;
}

.invite-score-block {
  flex: 1.3;
  padding: 10px 12px;
  border-radius: $radius-md;
  background: rgba(#ffffff, 0.6);
}

.score-main {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-value {
  font-size: 24px;
  font-weight: 800;
  color: $primary;
}

.score-unit {
  font-size: 12px;
  color: $text-secondary;
}

.score-label {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
}

.score-desc {
  margin-top: 2px;
  font-size: 12px;
  color: $text-secondary;
}

.invite-count-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.count-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: $radius-md;
  background: rgba(#ffffff, 0.6);
  font-size: 13px;
}

.count-label {
  color: $text-secondary;
}

.count-value {
  font-weight: 700;
  color: $text-primary;
}

.level-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.level-progress-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.level-progress-score {
  font-size: 16px;
  font-weight: 700;
  color: $primary;
}

.level-progress-bar {
  height: 8px;
  background: $bg-tertiary;
  border-radius: $radius-full;
  overflow: hidden;
  margin-bottom: 8px;
}

.level-progress-fill {
  height: 100%;
  background: $primary-gradient;
  border-radius: $radius-full;
  transition: width 0.6s ease;
}

.level-progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: $text-secondary;
}
.profile-email { font-size: 14px; color: $text-muted; margin-top: 2px; }
.profile-bio { font-size: 15px; color: $text-primary; margin-top: 10px; line-height: 1.5; }

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;

  .meta-item {
    font-size: 14px;
    color: $text-secondary;
    &.mbti { color: $primary; font-weight: 600; }
    &.gender {
      padding: 4px 12px;
      border-radius: $radius-full;
      font-weight: 600;
      &.male { background: rgba(#2196F3, 0.1); color: #2196F3; }
      &.female { background: rgba(#E91E63, 0.1); color: #E91E63; }
    }
  }
}

.profile-interests {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.interest-tag {
  padding: 4px 12px;
  background: rgba($primary, 0.08);
  color: $primary;
  border-radius: $radius-full;
  font-size: 13px;
}

.profile-stats {
  display: flex;
  gap: 32px;
  margin-top: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-light;
}

.stat-item {
  cursor: pointer;
  .stat-num { font-size: 18px; font-weight: 800; margin-right: 4px; }
  .stat-new { font-size: 14px; font-weight: 700; color: var(--el-color-danger); margin-right: 2px; }
  .stat-label { font-size: 14px; color: $text-secondary; }
  &:hover .stat-num { color: $primary; }
}

.match-section {
  margin-top: 20px;
  padding: 20px;
}

.section-title { font-size: 16px; font-weight: 700; margin-bottom: 14px; }

.match-total {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;

  .match-score { font-size: 36px; font-weight: 900; color: $primary; }
  .match-label { font-size: 14px; color: $text-secondary; }
}

.match-bars { display: flex; flex-direction: column; gap: 10px; }

.bar-item {
  display: flex;
  align-items: center;
  gap: 10px;

  .bar-label { width: 72px; font-size: 13px; color: $text-secondary; flex-shrink: 0; }
  .bar-track { flex: 1; height: 8px; background: $bg-tertiary; border-radius: $radius-full; overflow: hidden; }
  .bar-fill { height: 100%; background: $primary-gradient; border-radius: $radius-full; transition: width 0.6s ease; }
  .bar-val { width: 28px; font-size: 13px; font-weight: 700; color: $text-primary; text-align: right; }
}

.profile-posts { margin-top: 8px; }

.posts-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.tab-btn {
  padding: 8px 20px;
  border: none;
  background: transparent;
  color: $text-secondary;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-radius: $radius-full;
  transition: all $transition-fast;

  &:hover {
    background: rgba($primary, 0.08);
    color: $primary;
  }

  &.active {
    background: $primary;
    color: white;
    box-shadow: 0 4px 12px rgba($primary, 0.3);
  }
}

.feed-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid $border-light;
  gap: 12px;
}

.feed-item-content {
  flex: 1;
  min-width: 0;
}

.feed-content { font-size: 14px; line-height: 1.6; margin-bottom: 8px; }

.feed-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: $text-muted;
}

.feed-delete-btn {
  flex-shrink: 0;
  padding: 4px 8px;
  border: none;
  background: transparent;
  font-size: 16px;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity $transition-fast;

  &:hover {
    opacity: 1;
  }
}

.empty-hint {
  text-align: center;
  padding: 40px;
  color: $text-muted;
  font-size: 14px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.user-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: background $transition-fast;

  &:hover {
    background: $bg-secondary;
  }
}

.user-list-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-list-info {
  flex: 1;
  min-width: 0;
}

.user-list-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.user-list-email {
  font-size: 13px;
  color: $text-secondary;
}

.user-list-action {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid $border-color;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all $transition-fast;
  color: $primary;

  &:hover {
    background: $primary;
    color: white;
    border-color: $primary;
  }
}

.btn-yuanfen {
  padding: 8px 16px;
  border-radius: $radius-full;
  background: linear-gradient(135deg, #a855f7, #6366f1);
  color: white;
  font-weight: 600;
  font-size: 13px;
  border: none;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(#a855f7, 0.3);
  }

  &:disabled {
    background: $bg-tertiary;
    color: $text-muted;
    cursor: not-allowed;
    box-shadow: none;
  }
}
.privacy-settings-form {
  padding: 10px 0;
}

.setting-item {
  margin-bottom: 20px;
  
  .setting-label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 12px;
  }
}
</style>
