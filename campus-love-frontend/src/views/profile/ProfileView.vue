<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-cover"></div>
      <div class="profile-main">
        <div class="avatar-wrapper">
          <img :src="profile?.avatarUrl || defaultAvatar" class="profile-avatar avatar" width="100" height="100" />
          <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="handleAvatarChange" />
          <button v-if="isMe" class="avatar-upload-btn" @click="avatarInput?.click()">
            <el-icon><Camera /></el-icon>
          </button>
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
        </div>
        <div v-else class="profile-actions">
          <button class="btn-outline" @click="$router.push('/setup-profile')">
            <el-icon><Edit /></el-icon> 编辑资料
          </button>
          <button class="btn-outline" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </div>

    <div v-if="profile" class="profile-info">
      <div class="profile-name-row">
        <h2 class="profile-name">{{ profile.nickname }}</h2>
        <div class="level-display">
          <span class="level-badge large">Lv{{ profile.userLevel }}</span>
          <span v-if="profile.isAdmin" class="admin-badge">管理员</span>
        </div>
      </div>
      <p class="profile-email">{{ profile.email }}</p>
      <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>

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
        <span v-if="profile.school" class="meta-item">🎓 {{ profile.school }}</span>
        <span v-if="profile.major" class="meta-item">📚 {{ profile.major }}</span>
        <span v-if="profile.grade" class="meta-item">📅 {{ profile.grade }}</span>
        <span v-if="profile.zodiac" class="meta-item">⭐ {{ profile.zodiac }}</span>
        <span v-if="profile.mbti" class="meta-item mbti">🧠 {{ profile.mbti }}</span>
        <span v-if="profile.bazi" class="meta-item">🔮 {{ profile.bazi }}</span>
      </div>

      <div v-if="profile.interests" class="profile-interests">
        <span v-for="tag in profile.interests.split(',')" :key="tag" class="interest-tag">{{ tag }}</span>
      </div>

      <div class="profile-stats">
        <div class="stat-item" @click="handleOpenFollowing">
          <span class="stat-num">{{ followingCount }}</span>
          <span class="stat-label">关注</span>
        </div>
        <div class="stat-item" @click="handleOpenFollowers">
          <span class="stat-num">{{ followerCount }}</span>
          <span class="stat-label">粉丝</span>
        </div>
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
        <button
          :class="['tab-btn', { active: postsTab === 'timeline' }]"
          @click="postsTab = 'timeline'; loadPostsByType()"
        >
          朋友圈
        </button>
        <button
          :class="['tab-btn', { active: postsTab === 'discovery' }]"
          @click="postsTab = 'discovery'; loadPostsByType()"
        >
          发现动态
        </button>
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
        {{ postsTab === 'timeline' ? '暂无朋友圈动态' : '暂无发现动态' }}
      </div>
    </div>
  </div>

  <!-- 关注列表弹窗 -->
  <el-dialog :title="isMe ? '我的关注' : `${profile?.nickname || 'TA'}的关注`" width="500px" :model-value="showFollowing" @update:model-value="showFollowing = false">
    <div v-if="followingList.length" class="user-list">
      <div v-for="user in followingList" :key="user.userId" class="user-list-item" @click="goToUserProfile(user.userId)">
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.nickname }}</div>
        </div>
        <button class="user-list-action" title="聊天" @click.stop="goToChat(user.userId)">
          <el-icon><ChatDotRound /></el-icon>
        </button>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无关注' : 'TA暂无关注' }}</div>
  </el-dialog>

  <!-- 粉丝列表弹窗 -->
  <el-dialog :title="isMe ? '我的粉丝' : `${profile?.nickname || 'TA'}的粉丝`" width="500px" :model-value="showFollowers" @update:model-value="showFollowers = false">
    <div v-if="followerList.length" class="user-list">
      <div v-for="user in followerList" :key="user.userId" class="user-list-item" @click="goToUserProfile(user.userId)">
        <img :src="user.avatarUrl || defaultAvatar" class="user-list-avatar" />
        <div class="user-list-info">
          <div class="user-list-name">{{ user.nickname }}</div>
        </div>
        <button class="user-list-action" title="聊天" @click.stop="goToChat(user.userId)">
          <el-icon><ChatDotRound /></el-icon>
        </button>
      </div>
    </div>
    <div v-else class="empty-hint">{{ isMe ? '暂无粉丝' : 'TA暂无粉丝' }}</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { getUserProfile, type UserProfile } from '@/api/userApi'
import { getMatchDetail, type MatchResult } from '@/api/matchApi'
import { getInviteStats, getUserInviteStats, type InviteStats } from '@/api/inviteApi'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList, getUserFollowing, getUserFollowers, type FollowUser } from '@/api/followApi'
import { getUserTimelinePosts, getUserDiscoveryPosts, deletePost, type FeedPost } from '@/api/feedApi'
import { ElMessage } from 'element-plus'
import { Camera, ChatDotRound, Calendar } from '@element-plus/icons-vue'
import { MATCH_DIMENSION_LABELS } from '@/constants/matchConst'
import { FOLLOW_STATUS_LABELS, FollowStatus } from '@/constants/followConst'
import { uploadAvatar } from '@/api/userApi'

const defaultAvatar = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23f0f2f5" width="100" height="100" rx="50"/><text x="50%" y="55%" text-anchor="middle" fill="%23adb5bd" font-size="44">👤</text></svg>'
const dimensionLabels = MATCH_DIMENSION_LABELS

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profileId = computed(() => route.params.userId ? Number(route.params.userId) : userStore.user?.id)
const isMe = computed(() => profileId.value === userStore.user?.id)

const profile = ref<UserProfile | null>(null)
const followStatus = ref<string>(FollowStatus.NONE)
const matchResult = ref<MatchResult | null>(null)
const inviteStats = ref<InviteStats | null>(null)
const posts = ref<FeedPost[]>([])
const postsTab = ref<'timeline' | 'discovery'>('timeline')
const followingCount = ref(0)
const followerCount = ref(0)
const showFollowing = ref(false)
const showFollowers = ref(false)
const followingList = ref<FollowUser[]>([])
const followerList = ref<FollowUser[]>([])
const avatarInput = ref<HTMLInputElement | null>(null)

const followLabel = computed(() => FOLLOW_STATUS_LABELS[followStatus.value as FollowStatus] || '关注')

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

// 打开粉丝列表
function handleOpenFollowers() {
  const isAdmin = userStore.user?.isAdmin || false
  if (!isMe.value && !isAdmin) {
    ElMessage.warning('只有管理员可以查看他人的粉丝列表')
    return
  }
  showFollowers.value = true
}

async function loadProfile() {
  if (!profileId.value) return
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
    }

    // 最后加载关注/粉丝数量（独立处理，不影响其他数据）
    loadFollowCounts()
  } catch (err) {
    console.error('loadProfile error:', err)
  }
}

// 独立函数加载关注/粉丝数量
async function loadFollowCounts() {
  try {
    const [fing, fers] = await Promise.all([
      isMe.value ? getFollowingList() : getUserFollowing(profileId.value!),
      isMe.value ? getFollowerList() : getUserFollowers(profileId.value!)
    ])
    followingCount.value = fing.data.data?.length || 0
    followerCount.value = fers.data.data?.length || 0
  } catch (err) {
    console.error('load follow/follower count error:', err)
    followingCount.value = 0
    followerCount.value = 0
  }
}

async function loadPostsByType() {
  if (!profileId.value) return
  try {
    let res
    if (postsTab.value === 'timeline') {
      res = await getUserTimelinePosts(profileId.value, 0, 20)
    } else {
      res = await getUserDiscoveryPosts(profileId.value, 0, 20)
    }
    posts.value = res.data.data || []
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
  router.push(`/profile/${userId}`)
}

// 跳转到聊天页面（暂时提示功能开发中）
function goToChat(_userId: number) {
  showFollowing.value = false
  showFollowers.value = false
  ElMessage.info('聊天功能开发中，敬请期待！')
  // router.push(`/chat/${userId}`)  // 聊天功能实现后启用
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
}

.profile-main {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 24px;
  margin-top: -50px;
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
  padding-bottom: 12px;
}

.profile-info { padding: 16px 24px; }

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}

.profile-name { font-size: 22px; font-weight: 800; }

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
  padding: 16px;
  background: rgba($primary, 0.06);
  border-radius: $radius-lg;
  border: 1px solid rgba($primary, 0.15);
}

.invite-stats-card {
  margin-top: 12px;
  padding: 16px;
  background: linear-gradient(135deg, rgba($primary, 0.08), rgba($primary, 0.02));
  border-radius: $radius-lg;
  border: 1px solid rgba($primary, 0.2);
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
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid $border-light;
}

.stat-item {
  cursor: pointer;
  .stat-num { font-size: 18px; font-weight: 800; margin-right: 4px; }
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

.profile-posts { border-top: 1px solid $border-light; margin-top: 8px; }

.posts-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-bottom: 1px solid $border-light;
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

  &:hover {
    background: rgba($primary, 0.08);
    color: $primary;
  }

  &.active {
    background: $primary;
    color: white;
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
</style>
