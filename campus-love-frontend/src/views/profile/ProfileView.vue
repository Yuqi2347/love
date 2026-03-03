<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="profile-cover"></div>
      <div class="profile-main">
        <img :src="profile?.avatarUrl || defaultAvatar" class="profile-avatar avatar" width="100" height="100" />
        <div v-if="!isMe" class="profile-actions">
          <button :class="['btn-primary', { 'btn-outline': followStatus !== 'NONE' }]" @click="handleFollowToggle">
            {{ followLabel }}
          </button>
          <button class="btn-outline" @click="$router.push(`/chat/${profileId}`)">
            <el-icon><ChatDotRound /></el-icon> 聊天
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
      <h2 class="profile-name">{{ profile.nickname }}</h2>
      <p class="profile-email">{{ profile.email }}</p>
      <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>

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
        <div class="stat-item" @click="showFollowing = true">
          <span class="stat-num">{{ followingCount }}</span>
          <span class="stat-label">关注</span>
        </div>
        <div class="stat-item" @click="showFollowers = true">
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
      <h3 class="section-title" style="padding: 16px 24px;">动态</h3>
      <div v-for="post in posts" :key="post.id" class="feed-item">
        <p class="feed-content">{{ post.content }}</p>
        <div class="feed-meta">
          <span>{{ post.createdAt }}</span>
          <span>❤️ {{ post.likeCount }} · 💬 {{ post.commentCount }}</span>
        </div>
      </div>
      <div v-if="!posts.length" class="empty-hint">暂无动态</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { getUserProfile, type UserProfile } from '@/api/userApi'
import { getMatchDetail, type MatchResult } from '@/api/matchApi'
import { followUser, unfollowUser, getFollowStatus, getFollowingList, getFollowerList } from '@/api/followApi'
import { getUserPosts, type FeedPost } from '@/api/feedApi'
import { ElMessage } from 'element-plus'
import { MATCH_DIMENSION_LABELS } from '@/constants/matchConst'
import { FOLLOW_STATUS_LABELS, FollowStatus } from '@/constants/followConst'

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
const posts = ref<FeedPost[]>([])
const followingCount = ref(0)
const followerCount = ref(0)
const showFollowing = ref(false)
const showFollowers = ref(false)

const followLabel = computed(() => FOLLOW_STATUS_LABELS[followStatus.value as FollowStatus] || '关注')

async function loadProfile() {
  if (!profileId.value) return
  try {
    const res = await getUserProfile(profileId.value)
    profile.value = res.data.data

    const [postsRes] = await Promise.all([
      getUserPosts(profileId.value, 0, 20),
    ])
    posts.value = postsRes.data.data || []

    if (isMe.value) {
      const [fing, fers] = await Promise.all([getFollowingList(), getFollowerList()])
      followingCount.value = fing.data.data?.length || 0
      followerCount.value = fers.data.data?.length || 0
    } else {
      const statusRes = await getFollowStatus(profileId.value)
      followStatus.value = statusRes.data.data || FollowStatus.NONE
      const matchRes = await getMatchDetail(profileId.value)
      matchResult.value = matchRes.data.data
    }
  } catch { /* handled */ }
}

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

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
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

.profile-avatar {
  border: 4px solid $bg-primary;
  box-shadow: $shadow-md;
}

.profile-actions {
  display: flex;
  gap: 10px;
  padding-bottom: 12px;
}

.profile-info { padding: 16px 24px; }

.profile-name { font-size: 22px; font-weight: 800; }
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

.feed-item {
  padding: 14px 24px;
  border-bottom: 1px solid $border-light;
}

.feed-content { font-size: 14px; line-height: 1.6; margin-bottom: 8px; }

.feed-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: $text-muted;
}

.empty-hint {
  text-align: center;
  padding: 40px;
  color: $text-muted;
  font-size: 14px;
}
</style>
